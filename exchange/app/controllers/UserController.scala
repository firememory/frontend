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
import ControllerHelper._

object UserController extends Controller with Json4s {
  def login = Action.async(parse.urlFormEncoded) {
    implicit request =>
    val data = request.body
    val email = getParam(data, "username").getOrElse("")
    val password = getParam(data, "password").getOrElse("")
    val (paramsValid, failedResult) = stringParamsNotEmpty(Seq( email, password)) {
      parmaErrorResult
    }
    if (!paramsValid) {
      Future { Ok(failedResult.toJson) }
    } else {
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
  }

  def register = Action.async(parse.urlFormEncoded) {
    implicit request =>
    val data = request.body
    val uuid = getParam(data, "uuid").getOrElse("")
    val text = getParam(data, "text").getOrElse("")
    val email = getParam(data, "email").getOrElse("")
    val password = getParam(data, "password").getOrElse("")
    val nationalId = getParam(data, "nationalId")
    val realName = getParam(data, "realName")
    val (paramsValid, failedResult) = stringParamsNotEmpty(Seq(uuid, text, email, password)){
      parmaErrorResult
    }
    if (!paramsValid) {
      Future { Ok(failedResult.toJson) }
    }
    else if(!CaptchaController.validate(uuid, text)) {
      Future { Ok(ApiResult(false, -1, "error: 验证码错误").toJson) }
    } else {
      val user: User = User(Some(-1L), email, realName, password, nationalId)
      UserService.register(user) map {
        result =>
        if (result.success) {
          val profile = result.data.get.asInstanceOf[UserProfile]
          Ok(result.toJson)
          // Ok(result.toJson).withSession(
          //   "username" -> profile.email,
          //   "uid" -> profile.id.toString
          // )
        } else {
          Ok(result.toJson)
        }
      }
    }
  }

  def updateSettings = Action.async(parse.urlFormEncoded) {
    implicit request =>
    val data = request.body
    val userId = session.get("uid").getOrElse("")
    val email = session.get("username").getOrElse("")
    println(session.toJson)
    println(s"userId: $userId, email: $email")
    //val nationalId = getParam(data, "nationalId").getOrElse("")
    val realName = getParam(data, "realName").getOrElse("")
    val mobile = getParam(data, "mobile").getOrElse("")
    // TODO: validate sms verification code here
    val (paramsValid, failedResult) = stringParamsNotEmpty(Seq(userId, email, realName, mobile)){
      parmaErrorResult
    }

    val uid = userId.toLong
    if (!paramsValid) {
      Future { Ok(failedResult.toJson) }
    } else {
      val user: User = User(Some(uid), email,  Some(realName), null, None, Some(mobile))
      UserService.updateProfile(user) map {
        result =>
        Ok(result.toJson)
      }
    }
  }

  def verifyEmail(token: String) = Action.async {
    implicit request =>
    println(s"verify email token: $token")
    UserService.verifyEmail(token) map {
      result =>
      if (result.success) {
        println("verify email success!")
        Redirect(routes.MainController.login(true))
      } else {
        println("verify email failed!")
        Ok(views.html.responseMessage.render("邮件验证失败！"))
      }
    }
  }

  def registerSucceeded() = Action {
    implicit request =>
    Ok(views.html.responseMessage.render("验证邮件已发送到注册邮箱，请点击链接完成最后注册。"))
  }

  def verifyEmailFailed() = Action {
    implicit request =>
    Ok(views.html.responseMessage.render("邮件验证失败！"))
  }

  def logout = Action {
    implicit request =>
      Redirect(routes.MainController.index()).withSession(
        session - "username" - "uid"
      )
  }
}
