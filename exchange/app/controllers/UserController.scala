/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package controllers

import play.api.mvc._
import play.api.libs.functional.syntax._
import com.coinport.coinex.api.model._
import com.coinport.coinex.data.ErrorCode
import com.coinport.coinex.api.service._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.tototoshi.play2.json4s.native.Json4s
import com.coinport.coinex.data.{LoginSucceeded, LoginFailed, UserProfile}
import ControllerHelper._

object UserController extends Controller with Json4s {
  def login = Action.async(parse.urlFormEncoded) {
    implicit request =>
    val data = request.body
    val email = getParam(data, "username").getOrElse("")
    val password = getParam(data, "password").getOrElse("")
    validateParamsAndThen(
      new StringNonemptyValidator(email, password),
      new EmailFormatValidator(email),
      new PasswordFormetValidator(password)
    ) {
      val user: User = User(id = -1, email = email, password = password)
      UserService.login(user)
    } map {
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
      } else
        Ok(result.toJson)
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
    validateParamsAndThen(
      new StringNonemptyValidator(uuid, text, email, password),
      new EmailFormatValidator(email),
      new PasswordFormetValidator(password)
    ) {
      if(!CaptchaController.validate(uuid, text))
        Future { ApiResult(false, ErrorCode.CaptchaNotMatch.value, "") }
      else {
        val user: User = User(id = -1, email = email, password = password)
        UserService.register(user)
      }
    } map { result => Ok(result.toJson) }
  }

  def getUserProfile(userId: String)  = Action.async {
    implicit request =>
    UserService.getProfile(userId.toLong) map {
      result =>
      Ok(result.toJson)
    }
  }

  def getDepositAddress(currency: String, userId: String) = Action.async {
    implicit  request =>
      UserService.getDepositAddress(currency, userId.toLong) map {
        result =>
          Ok(result.toJson)
      }
  }

  def getWithdrawalAddress(currency: String, userId: String) = Action.async {
    implicit  request =>
      UserService.getWithdrawalAddress(currency, userId.toLong) map {
        result =>
          Ok(result.toJson)
      }
  }

  def updateSettings = Authenticated.async(parse.urlFormEncoded) {
    implicit request =>
    val data = request.body
    val userId = session.get("uid").getOrElse("")
    val email = session.get("username").getOrElse("")
    //val nationalId = getParam(data, "nationalId").getOrElse("")
    val realName = getParam(data, "realName").getOrElse("")
    val mobile = getParam(data, "mobile").getOrElse("")
    val uuid = getParam(data, "verifyCodeUuid").getOrElse("")
    val verifyCode = getParam(data, "verifyCode").getOrElse("")
    println(s"mobile: $mobile, uuid: $uuid, verifycode: $verifyCode")
    validateParamsAndThen(
      new StringNonemptyValidator(userId, email, realName, mobile, uuid, verifyCode),
      new CachedValueValidator("sms verify code", uuid, verifyCode)
    ) {
      val uid = userId.toLong
      UserService.getProfile(uid)
    } flatMap {
      result =>
      if (result.success) {
        val oldUser = result.data.get.asInstanceOf[User]
        val updatedUser = User(oldUser.id, oldUser.email, Some(realName), oldUser.password, None, Some(mobile), oldUser.depositAddress, oldUser.withdrawalAddress)
        UserService.updateProfile(updatedUser) map {
          updateRes =>
          Ok(updateRes.toJson)
        }
      } else {
        Future(Ok(result.toJson))
      }
    }
  }

  def verifyEmail(token: String) = Action.async {
    implicit request =>
    println(s"verify email token: $token")
    UserService.verifyEmail(token) map {
      result =>
      if (result.success) {
        Redirect(routes.MainController.login(true, "login.verifyEmailSucceeded"))
      } else {
        Redirect(routes.MainController.prompt("prompt.verifyEmailFailed"))
      }
    }
  }

  def forgetPassword  = Action {
    implicit request =>
    Ok(views.html.forgetPassword.render(session, lang))
  }

  def requestPasswordReset(email: String) = Action.async {
    implicit request =>
    println(s"reset password email: $email")
    UserService.requestPasswordReset(email) map {
      result =>
      if (result.success) {
        Redirect(routes.MainController.prompt("prompt.resetPwdEmailSent"))
      } else {
        Redirect(routes.MainController.prompt("prompt.resetPwdFailed"))
      }
    }
  }

  def validatePasswordReset(token: String) = Action.async {
    implicit request =>
    println(s"password reset token: $token")
    UserService.validatePasswordResetToken(token) map {
      result =>
      if (result.success) {
        val profile = result.data.get.asInstanceOf[UserProfile]
        println(s"profile: $profile")
        Ok(views.html.resetPassword.render(token, session, lang))
      } else {
        Redirect(routes.MainController.prompt("prompt.resetPwdFailed"))
      }
    }
  }

  def doPasswordReset() = Action.async(parse.urlFormEncoded) {
    implicit request =>
    val data = request.body
    val newPassword = getParam(data, "password").getOrElse("")
    val token = getParam(data, "token").getOrElse("")
    UserService.resetPassword(newPassword, token) map {
      result =>
      Ok(result.toJson)
    }
  }

  def accountSettingsView() = Authenticated.async {
    implicit request =>
    val email = session.get("username").getOrElse("")
    assert(email!=null && email.trim.nonEmpty)
    UserService.queryUserProfileByEmail(email) map {
      result =>
      println(result)
      assert(result.success)
      assert (result.data != None)
      val profile = result.data.get.asInstanceOf[UserProfile]
      assert(profile != null && email.equals(profile.email))
      val profileMap = Map(
        ("emailVerified" -> profile.emailVerified.toString),
        ("mobileVerified" -> profile.mobileVerified.toString),
        ("realName" -> profile.realName.getOrElse("")),
        ("mobile" -> profile.mobile.getOrElse(""))
      )
      Ok(views.html.viewAccountSettings.render(profileMap, lang))
    }
  }

  def logout = Action {
    implicit request =>
      Redirect(routes.MainController.index()).withNewSession
  }
}
