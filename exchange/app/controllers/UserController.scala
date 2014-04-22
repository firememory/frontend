/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package controllers

import play.api.mvc._
import play.api.libs.functional.syntax._
import com.coinport.coinex.api.model._
import com.coinport.coinex.api.service._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.tototoshi.play2.json4s.native.Json4s
import com.coinport.coinex.data.{LoginSucceeded, UserProfile}

object UserController extends Controller with Json4s {
  def login = Action.async(parse.urlFormEncoded) {
    implicit request =>
    val data = request.body
    val email = getParam(data, "username").get
    val password = getParam(data, "password").get
    val user: User = User(None, email, None, password, None)
    UserService.login(user) map {
      result =>
      if (result.success) {
        result.data.get match {
          case succeeded: LoginSucceeded =>
            Ok(result.toJson).withSession(
              "username" -> succeeded.email,
              "uid" -> succeeded.id.toString
            )
          case _ =>
            Ok(result.toJson)
        }
      } else {
        Ok(result.toJson)
      }
    }
  }

  def register = Action.async(parse.urlFormEncoded) {
    implicit request =>
    println("try register: " + json)
    val data = request.body
    val uuid = getParam(data, "uuid").get
    val text = getParam(data, "text").get
    println(s"captchauuid: $uuid, text: $text")
    if(!CaptchaController.validate(uuid, text)) {
      println("captcha validate failed!")
      Future {Ok(ApiResult(false, -1, "error: 验证码错误").toJson)}
    } else {
      println("captcha validate success!")
      val email = getParam(data, "email").get
      val password = getParam(data, "password").get
      val nationalId = getParam(data, "nationalId")
      val realName = getParam(data, "realName")
      val user: User = User(Some(-1L), email, realName, password, nationalId)
      UserService.register(user) map {
        result =>
        if (result.success) {
          val profile = result.data.get.asInstanceOf[UserProfile]
          Ok(result.toJson).withSession(
            "username" -> profile.email,
            "uid" -> profile.id.toString
          )
        } else {
          Ok(result.toJson)
        }
      }
    }
  }

  def logout = Action {
    implicit request =>
      Redirect(routes.MainController.index()).withSession(
        session - "username" - "uid"
      )
  }

  private def getParam(queryString: Map[String, Seq[String]], param: String): Option[String] = {
    queryString.get(param).map(_(0))
  }
}
