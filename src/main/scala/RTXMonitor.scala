import org.apache.pekko.actor.{Actor, ActorLogging, Props}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.HttpRequest
import org.apache.pekko.http.scaladsl.settings.ConnectionPoolSettings
import org.apache.pekko.stream.Materializer
import org.jsoup.Jsoup

import scala.concurrent.duration.*
import scala.util.{Failure, Success, Random}

class RTXMonitor(url: String)(implicit twilioService: TwilioService) extends Actor with ActorLogging {

  import RTXMonitor._
  import context.dispatcher

  implicit val materializer: Materializer = Materializer(context.system)

  private val cooldownMillis = 15.minutes.toMillis
  private val requestTimeout = 5.seconds

  private val http = Http()(context.system)
  private val settings = ConnectionPoolSettings(context.system)
    .withMaxOpenRequests(256)

  // Schedulers
  private val gpuCheck = scheduleTask(10.seconds, () => self ! Tick)
  private val healthCheck = scheduleTask(1.hour, () => self ! HealthCheck)

  override def preStart(): Unit = log.info(s"RTXMonitor started for URL: $url")
  override def postStop(): Unit = { gpuCheck.cancel(); healthCheck.cancel() }

  def receive: Receive = monitor(None)

  private def monitor(lastInStockAlert: Option[Long]): Receive = {
    case Tick =>
      val currentTime = System.currentTimeMillis()
      fetchHtml(url).onComplete {
        case Success(htmlContent) =>
          val newInStockAlert = processPage(htmlContent, currentTime, lastInStockAlert)
          self ! UpdateState(newInStockAlert)

        case Failure(exception) =>
          log.error(s"Request to $url failed: ${exception.getMessage}")
      }
    case UpdateState(newInStock) => context.become(monitor(newInStock))
    case HealthCheck             => log.info(s"${self.path.name} is healthy.")
  }

  // Fetch HTML with a timeout
  private def fetchHtml(url: String) = {
    http
      .singleRequest(HttpRequest(uri = url), settings = settings)
      .flatMap(_.entity.toStrict(requestTimeout))
      .map(_.data.utf8String)
  }

  // Process the HTML response
  private def processPage(htmlContent: String, currentTime: Long, lastInStockAlert: Option[Long]): Option[Long] = {
    val doc = Jsoup.parse(htmlContent)
    val inventoryText = doc.select("#pnlInventory").text().trim.toUpperCase
    val productName = doc
      .select("#product-details-control div.product-header h1 span")
      .text()
      .trim
    val sku = doc
      .select("#product-details-control span.sku")
      .text()
      .trim
      .replace("SKU:", "")
      .trim

    if (inventoryText.contains("IN STOCK") && isCooldownOver(lastInStockAlert, cooldownMillis, currentTime)) {
      sendStockAlert(productName, inventoryText, sku)
      Some(currentTime)
    } else lastInStockAlert
  }

  // Check if cooldown period has passed
  private def isCooldownOver(lastAlert: Option[Long], cooldown: Long, currentTime: Long): Boolean = {
    lastAlert.forall(ts => currentTime - ts >= cooldown)
  }

  // Send stock alert
  private def sendStockAlert(product: String, inventory: String, sku: String): Unit = {
    val message = s"CHIP IN STOCK\n\nProduct: $product\n\nInventory: $inventory\n\nSKU: $sku\n\nURL: $url"
    log.info(message)
    twilioService.sendSms(message)
  }

  // Helper to schedule tasks with a random initial delay
  private def scheduleTask(interval: FiniteDuration, task: () => Unit, initialDelay: Option[FiniteDuration] = None) =
    context.system.scheduler
      .scheduleAtFixedRate(
        initialDelay = {
          initialDelay match {
            case Some(delay) => delay
            case _           => Random.nextInt(5).seconds
          }
        },
        interval = interval
      )(() => task())
}

object RTXMonitor {
  def props(url: String)(implicit twilioService: TwilioService): Props = Props(
    new RTXMonitor(url)
  )
  private case object Tick
  private case object HealthCheck
  private case class UpdateState(lastInStockAlert: Option[Long])
}
