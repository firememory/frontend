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
import java.net.{URLDecoder, URLEncoder}

import play.api.mvc._
import play.api.Logger
import play.api.libs.functional.syntax._
import com.github.tototoshi.play2.json4s.native.Json4s
import com.coinport.coinex.api.model._
import com.coinport.coinex.data._
import com.coinport.coinex.api.service._
import utils.Constant
import ControllerHelper._
import controllers.GoogleAuth.{GoogleAuthenticator, GoogleAuthenticatorKey}

object UserController extends Controller with Json4s with AccessLogging {
  val logger = Logger(this.getClass)

  def login = Action.async(parse.urlFormEncoded) {
    implicit request =>
      val data = request.body
      val email = getParam(data, "username").getOrElse("")
      val password = getParam(data, "password").getOrElse("")
      //logger.info(s"email: $email, password: $password")
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
          //todo(kongliang): refactor return user profile
          if (result.success) {
            result.data.get match {
              case succeeded: LoginSucceeded =>
                Ok(result.toJson).withSession(
                  "username" -> succeeded.email,
                  "uid" -> succeeded.id.toString,
                  "referralToken" -> succeeded.referralToken.getOrElse(0L).toString,
                  Constant.cookieNameMobileVerified -> succeeded.mobile.isDefined.toString,
                  Constant.cookieNameMobile -> succeeded.mobile.getOrElse(""),
                  Constant.cookieNameRealName -> succeeded.realName.getOrElse(""),
                  Constant.cookieGoogleAuthSecret -> succeeded.googleSecret.getOrElse(""),
			      Constant.securityPreference -> succeeded.googleSecret.getOrElse("")
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
      val rf = getParam(data, "rf")
      validateParamsAndThen(
        new StringNonemptyValidator(uuid, text, email, password),
        new EmailFormatValidator(email),
        //new EmailWithInviteCodeValidator(email),
        new PasswordFormetValidator(password)
      ) {
        if (!CaptchaController.validate(uuid, text))
          Future {
            ApiResult(false, ErrorCode.CaptchaNotMatch.value, "")
          }
        else {
          val user: User = User(id = -1, email = email, password = password, referedToken = rf)
          UserService.register(user)
        }
      } map {
        result => Ok(result.toJson)
      }
  }

  def getUserProfile(userId: String) = Action.async {
    implicit request =>
      UserService.getProfile(userId.toLong) map {
        result =>
          Ok(result.toJson)
      }
  }

  def getBatchDepositAddress(userId: String) = Action.async {
    implicit request =>
      UserService.getDepositAddress(Constant.currencySeq, userId.toLong) map {
        result =>
          Ok(result.toJson)
      }
  }

  def getDepositAddress(currencyStr: String, userId: String) = Action.async {
    implicit request =>
      val cur: Currency = currencyStr
      UserService.getDepositAddress(Seq(cur), userId.toLong) map {
        result =>
          Ok(result.toJson)
      }
  }

  def setWithdrawalAddress(currency: String, userId: String, address: String) = Action.async {
    implicit request =>
      UserService.setWithdrawalAddress(userId.toLong, currency, address) map {
        result =>
          Ok(result.toJson)
      }
  }

  def getWithdrawalAddress(currency: String, userId: String) = Action.async {
    implicit request =>
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
                val newSession = request.session + (Constant.cookieNameMobileVerified -> "true") + (Constant.cookieNameMobile -> mobile) + (Constant.cookieNameRealName -> realName)
                Ok(updateRes.toJson).withSession(newSession)
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
            //val profile = result.data.get.asInstanceOf[UserProfile]
            //logger.info(s"profile: $profile")
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

  def doChangePassword() = Action.async(parse.urlFormEncoded) {
    implicit request =>
      val data = request.body
      val email = request.session.get("username").getOrElse("")
      val oldPassword = getParam(data, "oldPassword").getOrElse("")
      val newPassword = getParam(data, "newPassword").getOrElse("")
      logger.info(s"change password: email: $email, old: $oldPassword, new: $newPassword")
      UserService.changePassword(email, oldPassword, newPassword) map {
        result =>
          Ok(result.toJson)
      }
  }

  def resendVerifyEmail(email: String) = Action.async {
    implicit request =>
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
      Ok(views.html.viewAccountSettings.render(request.session, langFromRequestCookie(request)))
  }

  def accountProfiles() = Authenticated {
    implicit request =>
      Ok(views.html.viewAccountProfile.render(request.session, langFromRequestCookie(request)))
  }

  def googleauthView() = Authenticated {
    implicit request =>
      Ok(views.html.viewGoogleAuth.render(request.session, langFromRequestCookie(request)))
  }

  def logout = Action {
    implicit request =>
      Redirect(routes.MainController.index()).withNewSession
  }

  def getGoogleAuth = Action {
    implicit request =>
      val session = request.session
      session.get("uid") match {
        case Some(uid) =>
          var secret = session.get(Constant.cookieGoogleAuthSecret).getOrElse("")
          if (secret.isEmpty) {
            val email = request.session.get("username").getOrElse("")
            val googleAuthenticator = new GoogleAuthenticator()
            val key = googleAuthenticator.createCredentials(uid + "//" + email)
            secret = key.getKey()
          }

          val url = GoogleAuthenticatorKey.getQRBarcodeURL("COINPORT", uid, secret)

          Ok(ApiResult(true, 0, "secret", Some(Map("authUrl" -> url, "secret" -> secret))).toJson())

        case None => Unauthorized
      }
  }

  def bindGoogleAuth = Action.async(parse.urlFormEncoded) {
    implicit request =>
      val data = request.body
      val userId = request.session.get("uid").getOrElse("")
      //      val uuid = getParam(data, "uuid").getOrElse("")
      //      val emailCode = getParam(data, "emailcode").getOrElse("")
      val googleCode = getParam(data, "googlecode").getOrElse("")
      val googleSecret = getParam(data, "googlesecret").getOrElse("")

      validateParamsAndThen(
        //        new CachedValueValidator(ErrorCode.InvalidEmailVerifyCode, uuid, emailCode),
        new GoogleAuthValidator(ErrorCode.InvalidGoogleVerifyCode, googleSecret, googleCode)
      ) {
        UserService.bindGoogleAuth(userId.toLong, googleSecret)
      } map {
        result =>
          if (result.success) {
            val newSession = request.session + (Constant.cookieGoogleAuthSecret -> googleSecret)
            Ok(result.toJson).withSession(newSession)
          } else Ok(result.toJson())
      }
  }

  def unbindGoogleAuth = Action.async(parse.urlFormEncoded) {
    implicit request =>
      val data = request.body
      val userId = request.session.get("uid").getOrElse("")
      val googleSecret = request.session.get(Constant.cookieGoogleAuthSecret).getOrElse("")

      //      val uuid = getParam(data, "uuid").getOrElse("")
      //      val emailCode = getParam(data, "emailcode").getOrElse("")
      val googleCode = getParam(data, "googlecode").getOrElse("")

      validateParamsAndThen(
        //        new CachedValueValidator(ErrorCode.InvalidEmailVerifyCode, uuid, emailCode),
        new GoogleAuthValidator(ErrorCode.InvalidGoogleVerifyCode, googleSecret, googleCode)
      ) {
        UserService.unbindGoogleAuth(userId.toLong)
      } map {
        result =>
          if (result.success) {
            val newSession = request.session - Constant.cookieGoogleAuthSecret
            Ok(result.toJson()).withSession(newSession)
          } else Ok(result.toJson())
      }
  }

  def setPreferencePhone = Action.async(parse.urlFormEncoded) {
    implicit request =>
      val data = request.body
      val uuid = getParam(data, "uuid").getOrElse("")
      val userId = request.session.get("uid").getOrElse("")
      val phoneCode = getParam(data, "phonecode").getOrElse("")
      val preference = request.session.get(Constant.securityPreference).getOrElse("1")

      val prefer = "1" + preference.last

      validateParamsAndThen(
        new CachedValueValidator(ErrorCode.SmsCodeNotMatch, uuid, phoneCode)
      ) {
        UserService.setUserSecurityPreference(userId.toLong, prefer)
      } map {
        result =>
          if (result.success) {
            val newSession = request.session + (Constant.securityPreference -> prefer)
            Ok(result.toJson()).withSession(newSession)
          } else Ok(result.toJson())
      }
  }

  def setPreferenceEmail = Action.async(parse.urlFormEncoded) {
    implicit request =>
      val data = request.body
      val uuid = getParam(data, "uuid").getOrElse("")
      val userId = request.session.get("uid").getOrElse("")
      val emailCode = getParam(data, "emailcode").getOrElse("")
      val preference = request.session.get(Constant.securityPreference).getOrElse("1")

      val prefer = preference.head + "1"

      validateParamsAndThen(
        new CachedValueValidator(ErrorCode.InvalidEmailVerifyCode, uuid, emailCode)
      ) {
        UserService.setUserSecurityPreference(userId.toLong, prefer)
      } map {
        result =>
          if (result.success) {
            val newSession = request.session + (Constant.securityPreference -> prefer)
            Ok(result.toJson()).withSession(newSession)
          } else Ok(result.toJson())
      }
  }
}
