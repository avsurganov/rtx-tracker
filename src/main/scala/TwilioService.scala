import com.twilio.Twilio
import com.twilio.`type`.PhoneNumber
import com.twilio.rest.api.v2010.account.Message
import io.github.cdimascio.dotenv.Dotenv
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.event.Logging

trait TwilioService {
  def sendSms(messageBody: String): Unit
}

class TwilioServiceImpl(accountSid: String, authToken: String, fromNumber: String, to: String)(implicit system: ActorSystem) extends TwilioService {
  private val log = system.log

  Twilio.init(accountSid, authToken)

  override def sendSms(messageBody: String): Unit = {
    val message = Message.creator(
      new PhoneNumber(to),
      new PhoneNumber(fromNumber),
      messageBody
    ).create()
    log.info(s"Sent message with SID: ${message.getSid}")
  }
}

object TwilioServiceImpl {
  private lazy val dotenv: Dotenv = Dotenv.load()

  private def getEnvOrThrow(key: String)(implicit system: ActorSystem): String = {
    val log = Logging(system, getClass.getName)
    Option(dotenv.get(key)).orElse(sys.env.get(key)) match {
      case Some(value) => value
      case None =>
        log.error(s"Error: Missing environment variable '$key'. Shutting down the application.")
        system.terminate()
        "" // Unreachable.
    }
  }

  def apply()(implicit system: ActorSystem): TwilioService = {
    val accountSid = getEnvOrThrow("TWILIO_ACCOUNT_SID")
    val authToken  = getEnvOrThrow("TWILIO_AUTH_TOKEN")
    val fromNumber = getEnvOrThrow("TWILIO_FROM_NUMBER")
    val to         = getEnvOrThrow("TO_NUMBER")
    new TwilioServiceImpl(accountSid, authToken, fromNumber, to)
  }
}
