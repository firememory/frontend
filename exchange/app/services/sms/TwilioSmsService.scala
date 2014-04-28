package services.sms

import scala.concurrent._
import ExecutionContext.Implicits.global
import java.util.{List => JList, ArrayList}

import com.twilio.sdk.TwilioRestClient
import com.twilio.sdk.TwilioRestException
import com.twilio.sdk.resource.factory.MessageFactory
import org.apache.http.NameValuePair
import org.apache.http.message.BasicNameValuePair
import com.coinport.coinex.api.model._
import services.SmsService

object TwilioSmsService extends SmsService {
  val twilio = smsConfig.getConfig("sms.twilio")
  val twilioNumber = twilio.getString("twilioFromNumber")
  val accountSid = twilio.getString("accountSid")
  val authToken = twilio.getString("authToken")

  override def sendSmsSingle(phoneNum: String, text: String): Future[ApiResult] = future {
    try {
      val client = new TwilioRestClient(accountSid, authToken)
      val mainAccount = client.getAccount()
      val messageFactory = mainAccount.getMessageFactory
      val messageParams: JList[NameValuePair] = new ArrayList[NameValuePair]()
      messageParams.add(new BasicNameValuePair("To", phoneNum))
      messageParams.add(new BasicNameValuePair("From", twilioNumber))
      messageParams.add(new BasicNameValuePair("Body", text))
      val message = messageFactory.create(messageParams)
      successResult
    } catch {
      case e: TwilioRestException =>
        e.printStackTrace()
        ApiResult(false, -1, e.getMessage, None)
    }
  }
  override def sendSmsGroup(phoneNums: Set[String], text: String): Future[ApiResult] = future {
    failedResult
  }
  override def sendVerifySms(phoneNum: String, randCode: String): Future[ApiResult] = {
    val verifyMessage = createVerifyMessage(randCode)
    sendSmsSingle(phoneNum, verifyMessage)
  }

  private def createVerifyMessage(randCode: String) =
    s"Your verification code is $randCode [coinport]"

}
