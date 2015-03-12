package controllers

import play.api.mvc._
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global
import java.util.UUID
import java.util.Random
import scala.concurrent.Future
import scala.concurrent.Await

import com.coinport.coinex.data.Implicits._
import com.github.tototoshi.play2.json4s.native.Json4s
import com.coinport.coinex.api.model._
import com.coinport.coinex.data.ErrorCode
import com.coinport.coinex.api.service.UserService
import services._
import models._
import play.api.libs.json._
import ControllerHelper._



object SmsController extends Controller with Json4s {
  val logger = Logger(this.getClass)

  val smsService = SmsService.getDefaultServiceImpl
  val smsServiceInChina = SmsService.getNamedServiceImpl(SmsService.CLOOPEN_REST_SERVICE_IMPL)
  val cacheService = CacheService.getDefaultServiceImpl

  val allowedMinIntervalSeconds :Int = 20
  val rand = new Random()
  val randMax = 999999
  val randMin = 100000

  private def generateVerifyCode: (String, String) = {
    val uuid = UUID.randomUUID().toString
    val verifyNum = rand.nextInt(randMax - randMin) + randMin
    val verifyCode = verifyNum.toString
    cacheService.putWithTimeout(uuid, verifyCode, 30 * 60)
    (uuid, verifyCode)
  }

  def sendVerifySms = Authenticated.async(parse.urlFormEncoded) {
    implicit request =>
    val data = request.body
    val phoneNum = getParam(data, "phoneNumber").getOrElse("")
    logger.info(s"phoneNum: $phoneNum")
    val (uuid, verifyCode) = generateVerifyCode
    validateParamsAndThen(
      new PhoneNumberValidator(phoneNum)
    ) {
      sendSms(phoneNum, verifyCode, uuid)
    } map {
      result =>
      Ok(result.toJson)
    }
  }

  private def checkSmsFrequency(phoneNum: String): Boolean = {
    val lastTsStr = cacheService.get(phoneNum)
    val currTs = System.currentTimeMillis
    if (lastTsStr == null) {
      cacheService.putWithTimeout(phoneNum, currTs.toString, 120)
      true
    } else {
      val lastTs = lastTsStr.toLong
      logger.debug(s"lastTsStr: $lastTsStr, currTs: $currTs")
      if (currTs - lastTs < allowedMinIntervalSeconds * 1000)
        false
      else {
        cacheService.putWithTimeout(phoneNum, currTs.toString, 120)
        true
      }
    }
  }

  // TODO check phoneNum format.
  private def sendSms(phoneNum: String, verifyCode: String, uuid: String): Future[ApiResult] =
    if (! checkSmsFrequency(phoneNum)) {
      val err = ErrorCode.SendSmsFrequencyTooHigh
      Future(ApiResult(false, err.value, err.toString))
    } else {
      //logger.info(s"send sms verify code: phoneNum=$phoneNum, verifyCode=$verifyCode")
      val sendRes = if (phoneNum.startsWith("+86") || phoneNum.startsWith("0086")) {
        val shortPhoneNum = if (phoneNum.startsWith("+86")) phoneNum.substring(3).trim
        else phoneNum.substring(4).trim
        smsServiceInChina.sendVerifySms(shortPhoneNum, verifyCode)
      } else if(!phoneNum.startsWith("+") && !phoneNum.startsWith("00") &&
        phoneNum.trim.length == 11) {
        smsServiceInChina.sendVerifySms(phoneNum, verifyCode)
      } else {
        smsService.sendVerifySms(phoneNum, verifyCode)
      }

      sendRes map {
        result =>
        if (result.success)
          ApiResult(true, 0, "", Some(uuid))
        else
          result
      }
    }

  def sendVerificationEmail = Authenticated.async {
    implicit request =>
      val userEmail = request.session.get("username").getOrElse("")
      val (uuid, verifyCode) = generateVerifyCode
      UserService.sendVerificationCodeEmail(userEmail, verifyCode.toString, None, None) map {
        rv => Ok(ApiResult(true, 0, "", Some(uuid)).toJson)
      }
  }

  def sendVerifySms2 = Authenticated.async {
    implicit request =>
    val userId = request.session.get("uid").getOrElse("")
    val (uuid, verifyCode) = generateVerifyCode
    val uid = userId.toLong
    UserService.getProfile(uid) flatMap {
      result =>
      if (result.success) {
        val user = result.data.get.asInstanceOf[User]
        val phoneNum = user.mobile.getOrElse("")
        if (phoneNum == null || phoneNum.trim.length == 0) {
          val err = ErrorCode.MobileNotVerified
          Future(Ok(ApiResult(false, err.value, err.toString).toJson))
        } else {
          sendSms(phoneNum, verifyCode, uuid) map {
            result =>
            Ok(result.toJson)
          }
        }
      } else
        Future(Ok(result.toJson))
    }
  }

  def apiV2SendVerifyCodes() = Authenticated.async(parse.json) {
    implicit request =>
      val json = Json.parse(request.body.toString)
      val userId = request.userId
      val toEmail = (json \ "toEmail").asOpt[Boolean].getOrElse(false)
      val toPhone = (json \ "toPhone").asOpt[Boolean].getOrElse(false)
      val exchangeVersion = (json \ "exchangeVersion").asOpt[String]
      val lang = (json \ "lang").asOpt[String]

      val pfFuture = UserService.getProfileApiV2(userId)
      val pfResult = Await.result(pfFuture, 5 seconds).asInstanceOf[ApiResult]

      if (pfResult.success) {
        val pf = pfResult.data.get.asInstanceOf[ApiV2Profile]
        val email = pf.email
        val phone = pf.mobile
        var sendResult = SendVerifyCodeResult(false, None, false, None)
        if (toEmail) {
          val (emailUuid, emailVerifyCode) = generateVerifyCode
          val sendEmailVerifyFuture = UserService.sendVerificationCodeEmail(email, emailVerifyCode.toString, exchangeVersion, lang)
          val sendEmailVerifyResult = Await.result(sendEmailVerifyFuture, 5 seconds).asInstanceOf[ApiResult]
          val isEmailCodeSent = sendEmailVerifyResult.success
          sendResult = sendResult.copy(sendToEmail = true, emailUuid = Some(emailUuid))
        }

        if (toPhone && phone.isDefined) {
          val (phoneUuid, phoneVerifyCode) = generateVerifyCode
          validateParamsAndThen(
            new PhoneNumberValidator(phone.get)
          ) {
            sendSms(phone.get, phoneVerifyCode, phoneUuid)
          } map {
            result =>
              if (result.success)
                Ok(ApiV2Result(data = Some(sendResult.copy(sendToPhone = result.success, phoneUuid = Some(phoneUuid)))).toJson)
              else
                Ok(ApiV2Result(data = Some(sendResult.copy(sendToPhone = result.success))).toJson)
          }
        } else {
          Future(Ok(ApiV2Result(data = Some(sendResult)).toJson))
        }
      } else {
        Future(Ok(defaultApiV2Result(pfResult.code).toJson))
      }

  }

  def sendMobileBindCode() = Authenticated.async(parse.json) {
    implicit request =>
      val json = Json.parse(request.body.toString)
      val phone = (json \ "phone").asOpt[String].getOrElse("")
      val (phoneUuid, phoneVerifyCode) = generateVerifyCode
      validateParamsAndThen(
        new PhoneNumberValidator(phone)
      ) {
        sendSms(phone, phoneVerifyCode, phoneUuid)
      } map {
        result =>
          if (result.success) {
            Ok(ApiV2Result(data = Some(SendMobileBindCodeResult(phoneUuid = phoneUuid))).toJson)
          } else {
            Ok(defaultApiV2Result(result.code).toJson)
          }
      }
  }

  private def defaultApiV2Result(code: Int) = ApiV2Result(code, System.currentTimeMillis, None)
}
