import org.apache.pekko.actor.{Actor, ActorLogging, Props}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.HttpRequest
import org.apache.pekko.http.scaladsl.settings.ConnectionPoolSettings
import org.apache.pekko.stream.Materializer
import org.jsoup.Jsoup
import scala.concurrent.duration.*
import scala.util.{Failure, Success}
import scala.util.Random

class RTXMonitor(url: String)(implicit twilioService: TwilioService)
    extends Actor
    with ActorLogging {
  import RTXMonitor.*
  import context.dispatcher

  implicit val materializer: Materializer = Materializer(context.system)

  private val cooldownMillis: Long = 15.minutes.toMillis
  private val errorCooldownMillis: Long = 10.hours.toMillis

  private val http = Http()(context.system)
  private val settings = ConnectionPoolSettings(context.system)
    .withMaxOpenRequests(64) // Increase open requests

  private val gpuCheck = context.system.scheduler.scheduleAtFixedRate(
    initialDelay = Random.nextInt(5).seconds, // Prevents burst requests
    interval = 10.seconds
  )(() => self ! Tick)

  private val healthCheck = context.system.scheduler.scheduleAtFixedRate(
    initialDelay = 30.seconds,
    interval = 1.hour
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

  private def active(
      lastInStockAlert: Option[Long],
      lastErrorAlert: Option[Long]
  ): Receive = {
    case Tick =>
      val currentTime = System.currentTimeMillis()
      http
        .singleRequest(HttpRequest(uri = url), settings = settings)
        .onComplete {
          case Success(response) =>
            response.entity.toStrict(5.seconds).onComplete {
              case Success(strictEntity) =>
                val htmlContent = strictEntity.data.utf8String
                val doc = Jsoup.parse(htmlContent)

                val inventoryText = doc.select("#pnlInventory").text().trim
                val productDetailsElem = doc.select("#product-details-control")
                val productName = productDetailsElem
                  .select("div.product-header h1 span")
                  .text()
                  .trim
                val skuText = productDetailsElem.select("span.sku").text().trim
                val sku = skuText.replace("SKU:", "").trim

                val newInStockAlert = {
                  if (
                    inventoryText.toUpperCase.contains("IN STOCK") &&
                    lastInStockAlert
                      .forall(ts => currentTime - ts >= cooldownMillis)
                  ) {
                    val messageBody =
                      s"CHIP IN STOCK\nProduct: $productName\nInventory: $inventoryText\nSKU: $sku"
                    log.info(messageBody)
                    twilioService
                      .sendSms(messageBody) // Send in chunks if needed
                    Some(currentTime)
                  } else lastInStockAlert
                }

                self ! UpdateState(newInStockAlert, lastErrorAlert)

              case Failure(e) =>
                log.error(
                  s"Failed to retrieve content from $url: ${e.getMessage}"
                )
                val newErrorAlert =
                  if (
                    lastErrorAlert
                      .forall(ts => currentTime - ts >= errorCooldownMillis)
                  ) {
//                    twilioService.sendSms(
//                      s"Error retrieving content from $url: ${e.getMessage}"
//                    )
                    Some(currentTime)
                  } else lastErrorAlert
                self ! UpdateState(lastInStockAlert, newErrorAlert)
            }

          case Failure(exception) =>
            log.error(s"Request to $url failed: ${exception.getMessage}")
            val newErrorAlert =
              if (
                lastErrorAlert
                  .forall(ts => currentTime - ts >= errorCooldownMillis)
              ) {
//                twilioService.sendSms(
//                  s"Error: Request to $url failed: ${exception.getMessage}"
//                )
                Some(currentTime)
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
  def props(url: String)(implicit twilioService: TwilioService): Props = Props(
    new RTXMonitor(url)
  )
  private case object Tick
  private case object HealthCheck
  private case class UpdateState(
      lastInStockAlert: Option[Long],
      lastErrorAlert: Option[Long]
  )
}
