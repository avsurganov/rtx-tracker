import org.apache.pekko.actor.{Actor, ActorLogging, Props}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.HttpRequest
import org.apache.pekko.stream.Materializer
import org.jsoup.Jsoup

import scala.concurrent.duration.*
import scala.util.{Failure, Success}

class RTXMonitor(url: String)(implicit twilioService: TwilioService) extends Actor with ActorLogging {
  import RTXMonitor.*
  import context.dispatcher

  implicit val materializer: Materializer = Materializer(context.system)

  private val cooldownMillis: Long = 15 * 60 * 1000
  private val errorCooldownMillis: Long = 60 * 10 * 60 * 1000

  private val gpuCheck = context.system.scheduler.scheduleAtFixedRate(
    initialDelay = 0.seconds,
    interval = 3.second
  )(() => self ! Tick)

  private val healthCheck = context.system.scheduler.scheduleAtFixedRate(
    initialDelay = (60 * 60).seconds,
    interval = (60 * 60).second
  )(() => self ! HealthCheck)


  override def preStart(): Unit = {
    log.info(s"${self.getClass.getName} starting. Watching URL: $url")
    super.preStart()
  }

  override def postStop(): Unit = {
    gpuCheck.cancel()
    healthCheck.cancel()
    super.postStop()
  }

  def receive: Receive = active(None, None)

  private def active(lastInStockAlert: Option[Long], lastErrorAlert: Option[Long]): Receive = {
    case Tick =>
      val currentTime = System.currentTimeMillis()
      Http()(context.system).singleRequest(HttpRequest(uri = url)).onComplete {
        case Success(response) =>
          response.entity.toStrict(5.seconds).onComplete {
            case Success(strictEntity) =>
              val htmlContent = strictEntity.data.utf8String
              val doc = Jsoup.parse(htmlContent)

              val inventoryText = doc.select("#pnlInventory").text().trim
              val productDetailsElem = doc.select("#product-details-control")
              val productName = productDetailsElem.select("div.product-header h1 span").text().trim
              val skuText = productDetailsElem.select("span.sku").text().trim
              val sku = skuText.replace("SKU:", "").trim

              val newInStockAlert =
                if (inventoryText.toUpperCase.contains("IN STOCK") && lastInStockAlert.forall(ts => currentTime - ts >= cooldownMillis)) {
                  val messageBody = s"CHIP IN STOCK\n\nProduct: $productName\n\nInventory: $inventoryText\n\nSKU: $sku"
                  log.info(messageBody)
                  twilioService.sendSms(messageBody)
                  currentTime.some
                } else lastInStockAlert

              val newErrorAlert = lastErrorAlert  // No error here; retain the error state.
              self ! UpdateState(newInStockAlert, newErrorAlert)

            case Failure(e) =>
              log.error(s"Failed to retrieve strict entity from $url: $e")
              val newErrorAlert =
                if (lastErrorAlert.forall(ts => currentTime - ts >= errorCooldownMillis)) {
                  twilioService.sendSms(s"Error: Failed to retrieve content from $url: $e")
                  currentTime.some
                } else lastErrorAlert
              self ! UpdateState(lastInStockAlert, newErrorAlert)
          }
        case Failure(exception) =>
          log.error(s"Request to $url failed: $exception")
          val newErrorAlert =
            if (lastErrorAlert.forall(ts => currentTime - ts >= errorCooldownMillis)) {
              twilioService.sendSms(s"Error: Request to $url failed: $exception")
              currentTime.some
            } else lastErrorAlert
          self ! UpdateState(lastInStockAlert, newErrorAlert)
      }

    case UpdateState(newInStock, newError) =>
      context.become(active(newInStock, newError))

    case HealthCheck =>
      log.info(s"${self.path.name} is healthy.")
  }
}

implicit class OptionOps[T](val t: T) extends AnyVal {
  def some: Option[T] = Some(t)
}

object RTXMonitor {
  def props(url: String)(implicit twilioService: TwilioService): Props = Props(new RTXMonitor(url))
  private case object Tick
  private case object HealthCheck
  private case class UpdateState(lastInStockAlert: Option[Long], lastErrorAlert: Option[Long])
}
