package controllers

import play.api.mvc._
import play.api.libs.functional.syntax._
import scala.concurrent._
import ExecutionContext.Implicits.global

import com.github.tototoshi.play2.json4s.native.Json4s
import ControllerHelper._
import services.SmsService
import services.sms.VerifyCodeManager
import com.coinport.coinex.api.model._
import models._

object SmsController extends Controller with Json4s {
  val smsService = SmsService.getDefaultServiceImpl

  def sendVerifySms = Action.async(parse.urlFormEncoded) {
    implicit request =>
    val data = request.body
    val phoneNum = getParam(data, "phoneNumber").getOrElse("")
    val (uuid, verifyCode) = VerifyCodeManager.generateVerifyCode
    println(s"verifySms: uuid=$uuid, verifyCode=$verifyCode")
    smsService.sendVerifySms(phoneNum, verifyCode) map {
      result =>
      if (result.success) {
        Ok(ApiResult(true, 0, "", Some(uuid)).toJson)
      } else {
        Ok(result.toJson)
      }
    }
  }

  // def validateAndThen[T](validator: (Seq[T]) => Boolean)(failed: => Future[ApiResult])(andThen: => Future[ApiResult]) = {

  // }
  // def verify (uuid: String, code: String): Boolean = {
  //   VerifyCodeManager.verify(uuid, code)
  // }
}
