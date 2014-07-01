/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package controllers

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import java.io.File
import java.io.FileOutputStream
import java.util.Properties

import play.api.mvc._
import play.api.Logger
import play.api.libs.functional.syntax._
import com.github.tototoshi.play2.json4s.native.Json4s
import com.coinport.coinex.api.model._
import com.coinport.coinex.data._
import com.coinport.coinex.api.service._
import utils.Constant
import ControllerHelper._

object UserController extends Controller with Json4s with AccessLogging {
  val logger = Logger(this.getClass)

  // val inviteCodeFile: String = "/var/coinport/private/inviteCode.txt"
  // val usedInviteCodeFile: String = "/var/coinport/private/usedInviteCode.properties"

  def login = Action.async(parse.urlFormEncoded) {
    implicit request =>
    val data = request.body
    val email = getParam(data, "username").getOrElse("")
    val password = getParam(data, "password").getOrElse("")
    logger.info(s"email: $email, password: $password")
    validateParamsAndThen(
      new StringNonemptyValidator(password),
      new EmailFormatValidator(email),
      new PasswordFormetValidator(password)
    ) {
      val user: User = User(id = -1, email = email, password = password)
      UserService.login(user)
    } map {
      result =>
      logger.info(s"login result: $result")
      if (result.success) {
        result.data.get match {
          case succeeded: LoginSucceeded =>
            Ok(result.toJson).withSession(
              "username" -> succeeded.email,
              "uid" -> succeeded.id.toString,
              "referralToken" -> succeeded.referralToken.getOrElse(0L).toString
            )
          case _ =>
            Ok(result.toJson)
        }
      } else
        Ok(result.toJson)
    }
  }

  // def verifyInviteCode(inviteCode: String, email: String) = Action {
  //   implicit request =>
  //   def checkInviteCode: Boolean = {
  //     if (inviteCode == null) false
  //     else {
  //       try {
  //         val isValid = Source.fromFile(inviteCodeFile).getLines.exists(_.trim.equals(inviteCode.trim))
  //         val isUsed = if(new File(usedInviteCodeFile).exists)
  //           Source.fromFile(usedInviteCodeFile).getLines.exists(line => line.contains(inviteCode) && !line.contains(email))
  //         else false

  //         isValid && !isUsed
  //       } catch {
  //         case e: Throwable =>
  //           logger.error(e.getMessage, e)
  //           false
  //       }
  //     }
  //   }

  //   def updateUsedInviteCodeFile() = {
  //     val props = new Properties()
  //     props.setProperty(inviteCode, email)
  //     var output: FileOutputStream = null
  //     try {
  //       output = new FileOutputStream(usedInviteCodeFile, true)
  //       props.store(output, "used invite code.")
  //     } catch {
  //       case e: Throwable => logger.error(e.getMessage, e)
  //     } finally {
  //       output.close()
  //     }
  //   }

  //   if (checkInviteCode) {
  //     updateUsedInviteCodeFile()
  //     Ok(views.html.register.render(email, request.session, langFromRequestCookie(request)))
  //     //MainController.register(email)
  //   } else {
  //     Redirect(routes.MainController.inviteCode("register.inviteCodeNoMatch"))
  //   }
  // }

  def register = Action.async(parse.urlFormEncoded) {
    implicit request =>
    val data = request.body
    val uuid = getParam(data, "uuid").getOrElse("")
    val text = getParam(data, "text").getOrElse("")
    val email = getParam(data, "email").getOrElse("")
    val password = getParam(data, "password").getOrElse("")
    val nationalId = getParam(data, "nationalId")
    val realName = getParam(data, "realName")
    val rf =  getParam(data, "rf")
    validateParamsAndThen(
      new StringNonemptyValidator(uuid, text, email, password),
      new EmailFormatValidator(email),
      //new EmailWithInviteCodeValidator(email),
      new PasswordFormetValidator(password)
    ) {
      if(!CaptchaController.validate(uuid, text))
        Future { ApiResult(false, ErrorCode.CaptchaNotMatch.value, "") }
      else {
        val user: User = User(id = -1, email = email, password = password, referedToken = rf)
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

  def setWithdrawalAddress(currency: String, userId: String, address: String) = Action.async {
    implicit  request =>
      UserService.setWithdrawalAddress(userId.toLong, currency, address) map {
        result =>
          Ok(result.toJson)
      }
  }

  def getWithdrawalAddress(currency: String, userId: String) = Action.async {
    implicit  request =>
      UserService.getWithdrawalAddress(userId.toLong, currency) map {
        result =>
          Ok(result.toJson)
      }
  }

  def updateSettings = Authenticated.async(parse.urlFormEncoded) {
    implicit request =>
    val data = request.body
    val userId = request.session.get("uid").getOrElse("")
    val email = request.session.get("username").getOrElse("")
    //val nationalId = getParam(data, "nationalId").getOrElse("")
    val realName = getParam(data, "realName").getOrElse("")
    val mobile = getParam(data, "mobile").getOrElse("")
    val uuid = getParam(data, "verifyCodeUuid").getOrElse("")
    val verifyCode = getParam(data, "verifyCode").getOrElse("")
    logger.info(s"mobile: $mobile, uuid: $uuid, verifycode: $verifyCode")
    validateParamsAndThen(
      new CachedValueValidator(ErrorCode.SmsCodeNotMatch, uuid, verifyCode),
      new StringNonemptyValidator(userId, email, realName, mobile)
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
          val mobileVerifiedCookie = Cookie(Constant.cookieNameMobileVerified, "true")
          val mobileCookie = Cookie(Constant.cookieNameMobile, mobile)
          val realNameCookie = Cookie(Constant.cookieNameRealName, realName)
          Ok(updateRes.toJson).withCookies(mobileVerifiedCookie,
            mobileCookie, realNameCookie)
        }
      } else {
        Future(Ok(result.toJson))
      }
    }
  }

  def verifyEmail(token: String) = Action.async {
    implicit request =>
    logger.info(s"verify email token: $token")
    UserService.verifyEmail(token) map {
      result =>
      if (result.success) {
        Redirect(routes.MainController.login("login.verifyEmailSucceeded"))
      } else {
        Redirect(routes.MainController.prompt("prompt.verifyEmailFailed"))
      }
    }
  }

  def forgetPassword  = Action {
    implicit request =>
    Ok(views.html.forgetPassword.render(request.session, langFromRequestCookie(request)))
  }

  def requestPasswordReset(email: String) = Action.async {
    implicit request =>
    logger.info(s"reset password email: $email")
    UserService.requestPasswordReset(email) map {
      result =>
      logger.info(s"result: $result")
      if (result.success) {
        Redirect(routes.MainController.prompt("prompt.resetPwdEmailSent"))
      } else {
        Redirect(routes.MainController.prompt("prompt.requestResetPwdFailed"))
      }
    }
  }

  def validatePasswordReset(token: String) = Action.async {
    implicit request =>
    logger.info(s"password reset token: $token")
    UserService.validatePasswordResetToken(token) map {
      result =>
      if (result.success) {
        val profile = result.data.get.asInstanceOf[UserProfile]
        logger.info(s"profile: $profile")
        Ok(views.html.resetPassword.render(token, request.session, langFromRequestCookie(request)))
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

  def resendVerifyEmail(email: String) = Action.async {
    implicit request =>
    logger.info(s"resend verify email: $email")
    UserService.resendVerifyEmail(email) map {
      result =>
      logger.info(s"result: $result")
      if (result.success) {
        Redirect(routes.MainController.prompt("prompt.resendVerifyEmailSucceedded"))
      } else {
        logger.warn(s"resend verify email failed. email: $email")
        Redirect(routes.MainController.prompt("prompt.resendVerifyEmailFailed"))
      }
    }
  }

  def accountSettingsView() = Authenticated {
    implicit request =>
      Ok(views.html.viewAccountSettings.render(langFromRequestCookie(request)))
  }

  def accountProfiles() = Authenticated {
    implicit request =>
    val email = request.session.get("username").getOrElse("")
    val referralToken = request.session.get("referralToken").getOrElse("")
    val mobileVerified = request.cookies.get(Constant.cookieNameMobileVerified).map(_.value.toString).getOrElse("")
    val mobile = request.cookies.get(Constant.cookieNameMobile).map(_.value.toString).getOrElse("")
    val realName = request.cookies.get(Constant.cookieNameRealName).map(_.value.toString).getOrElse("")
    val profileMap = Map[String, String](
      ("emailVerified" -> "true"),
      ("email" -> email),
      ("mobileVerified" -> mobileVerified),
      ("realName" -> realName),
      ("mobile" -> mobile),
      ("shareLink" -> ("https://coinport.com?rf=" + referralToken))
    )

    Ok(views.html.viewAccountProfile.render(profileMap, langFromRequestCookie(request)))
  }

  def logout = Action {
    implicit request =>
      Redirect(routes.MainController.index()).withNewSession
  }
}
