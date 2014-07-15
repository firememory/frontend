package controllers

import play.api.mvc._
import play.api.libs.json._

import services.CaptchaService
import com.coinport.coinex.api.model._
import com.github.tototoshi.play2.json4s.native.Json4s

object CaptchaController extends Controller with Json4s {
  def captcha = Action { implicit request =>
    val captcha = CaptchaService.getCaptcha
    val apiResult = ApiResult(true, 0, "", Some(captcha))
    Ok(apiResult.toJson)
  }

  // def validate(uuid: String, text: String): Boolean = {
  //   println(s"captchaController.validate, uuid: $uuid, text: $text")
  //   try {
  //     captchaService.validateResponseForID(uuid, text)
  //   } catch {
  //     case e: Throwable =>
  //       e.printStackTrace
  //       false
  //   }
  // }
}
