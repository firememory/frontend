package controllers

import play.api.mvc._
import play.api.libs.functional.syntax._
import scala.concurrent._
import ExecutionContext.Implicits.global
import java.util.UUID
import java.util.Random

import com.github.tototoshi.play2.json4s.native.Json4s
import ControllerHelper._
import com.coinport.coinex.api.model._
import com.coinport.coinex.data.ErrorCode
import services._
import models._

object SmsController extends Controller with Json4s {
  val smsService = SmsService.getDefaultServiceImpl
  val cacheService = CacheService.getDefaultServiceImpl

  val rand = new Random()
  val randMax = 999999
  val randMin = 100000

  private def generateVerifyCode: (String, String) = {
    val uuid = UUID.randomUUID().toString
    val verifyNum = rand.nextInt(randMax - randMin) + randMin
    val verifyCode = verifyNum.toString
    cacheService.put(uuid, verifyCode)
    (uuid, verifyCode)
  }

  def sendVerifySms = Action.async(parse.urlFormEncoded) {
    implicit request =>
    val data = request.body
    val phoneNum = getParam(data, "phoneNumber").getOrElse("")
    println(s"phoneNum: $phoneNum")
    val (uuid, verifyCode) = generateVerifyCode
    smsService.sendVerifySms(phoneNum, verifyCode) map {
      result =>
      if (result.success) {
        Ok(ApiResult(true, 0, "", Some(uuid)).toJson)
      } else {
        Ok(result.toJson)
      }
    }
  }

  // def validateSmsCode (uuid: String, code: String): Boolean = {
  //   val codeCached = cacheService.get(uuid)
  //   if (codeCached != null && codeCached.trim.equals(code.trim)) true else false
  // }
}
