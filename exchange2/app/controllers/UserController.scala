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
import java.util.{UUID, Properties}
import java.net.{URLDecoder, URLEncoder}

import play.api.mvc._
import play.api.Logger
import play.api.libs.functional.syntax._
import com.github.tototoshi.play2.json4s.native.Json4s
import com.coinport.coinex.api.model._
import com.coinport.coinex.data._
import com.coinport.coinex.api.service._
import utils.Constant
import utils.SecurityPreferenceUtil
import ControllerHelper._
import controllers.GoogleAuth.{GoogleAuthenticator, GoogleAuthenticatorKey}
import services.CacheService

object UserController extends Controller with Json4s with AccessLogging {
  val logger = Logger(this.getClass)
  val cache = CacheService.getDefaultServiceImpl

  def login = Action.async(parse.urlFormEncoded) {
    implicit request =>
      val data = request.body
      val email = getParam(data, "username").getOrElse("")
      val password = getParam(data, "password").getOrElse("")
      val ip = request.remoteAddress //getParam(data, "ip").getOrElse("")
      //val location = getParam(data, "location").getOrElse("")
      logger.info(s"login ip: $ip")
      validateParamsAndThen(
        new StringNonemptyValidator(password),
        new EmailFormatValidator(email),
        new PasswordFormetValidator(password),
        new LoginFailedFrequencyValidator(email, ip)
      ) {
        val user: User = User(id = -1, email = email, password = password)
        UserService.login(user)
      } map {
        result =>
          //todo(kongliang): refactor return user profile
          if (result.success) {
            result.data.get match {
              case profile: User =>
                val uid = profile.id.toString
                //val userAction = UserAction(0L, succeeded.id, System.currentTimeMillis, UserActionType.Login, Some(ip), Some(location))
                //UserActionService.saveUserAction(userAction)
                val csrfToken = UUID.randomUUID().toString
                cache.put("csrf-" + uid, csrfToken)
                LoginFailedFrequencyValidator.cleanLoginFailedRecord(email, ip)

                Ok(result.toJson).withSession(
                  "username" -> profile.email,
                  "uid" -> uid,
                  //"referralToken" -> profile.referralToken.getOrElse(0L).toString,
                  Constant.cookieNameMobileVerified -> profile.mobile.isDefined.toString,
                  Constant.cookieNameMobile -> profile.mobile.getOrElse(""),
                  Constant.cookieNameRealName -> profile.realName.getOrElse(""),
                  Constant.cookieGoogleAuthSecret -> profile.googleAuthenticatorSecret.getOrElse(""),
                  Constant.securityPreference -> profile.securityPreference.getOrElse("01"),
                  Constant.userRealName -> profile.realName2.getOrElse("")
                ).withCookies(
                    Cookie("XSRF-TOKEN", csrfToken, None, "/", None, false, false)
                )
              case _ =>
                LoginFailedFrequencyValidator.putLoginFailedRecord(email, ip)
                Ok(result.toJson)
            }
          } else {
            val count = LoginFailedFrequencyValidator.getLoginFailedCount(email, ip)
            if (result.code != ErrorCode.LoginFailedAndLocked.value) {
              LoginFailedFrequencyValidator.putLoginFailedRecord(email, ip)
              val newRes = ApiResult(result.success, result.code, result.message, Some(4 - count))
              Ok(newRes.toJson)
            } else {
              Ok(result.toJson)
            }
          }
      }
  }

  def register = Action.async(parse.urlFormEncoded) {
    implicit request =>
      val data = request.body
      //val uuid = getParam(data, "uuid").getOrElse("")
      //val text = getParam(data, "text").getOrElse("")
      val email = getParam(data, "email").getOrElse("")
      val password = getParam(data, "password").getOrElse("")
      val nationalId = getParam(data, "nationalId")
      val realName = getParam(data, "realName")
      var rf = getParam(data, "rf")
      rf = if (rf == Some("")) None else rf
      validateParamsAndThen(
        //new CachedValueValidator(ErrorCode.CaptchaNotMatch, true, uuid, text),
        new StringNonemptyValidator(/*uuid, text,*/ email, password),
        new EmailFormatValidator(email),
        new PasswordFormetValidator(password)
      ) {
        //popCachedValue(uuid)
        val user: User = User(id = -1, email = email, password = password, referedToken = rf)
        UserService.register(user)
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
          //println(s"all deposit address: $result")
          Ok(result.toJson)
      }
  }

  def getDepositAddress(currencyStr: String, userId: String) = Action.async {
    implicit request =>
      val cur: Currency = currencyStr
      UserService.getDepositAddress(Seq(cur), userId.toLong) map {
        result =>
          //println(s"deposit address for $currencyStr: $result")
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
      //logger.info(s"mobile: $mobile, uuid: $uuid, verifycode: $verifyCode")
      validateParamsAndThen(
        new CachedValueValidator(ErrorCode.SmsCodeNotMatch, true, uuid, verifyCode),
        new StringNonemptyValidator(userId, email, realName, mobile)
      ) {
        popCachedValue(uuid)
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

  def doBindOrUpdateMobile = Authenticated.async(parse.urlFormEncoded) {
    implicit request =>
    val data = request.body
    val userId = request.session.get("uid").getOrElse("")
    val email = request.session.get("username").getOrElse("")
    val oldMobile = request.session.get("mobile").getOrElse("")

    val newMobile = getParam(data, "mobile").getOrElse("")
    val uuidOld = getParam(data, "verifyCodeUuidOld").getOrElse("")
    val verifyCodeOld = getParam(data, "verifyCodeOld").getOrElse("")
    val uuid = getParam(data, "verifyCodeUuid").getOrElse("")
    val verifyCode = getParam(data, "verifyCode").getOrElse("")

    logger.info(s"doBindOrUpdateMobile: mobileOld: $oldMobile, newMobile: $newMobile, uuid: $uuid, verifycode: $verifyCode")

    val needCheckOld = oldMobile.trim.nonEmpty
    validateParamsAndThen(
      new CachedValueValidator(ErrorCode.SmsCodeNotMatch, needCheckOld, uuidOld, verifyCodeOld),
      new CachedValueValidator(ErrorCode.SmsCodeNotMatch, true, uuid, verifyCode),
      new StringNonemptyValidator(userId, email, newMobile)
    ) {
      popCachedValue(uuidOld, uuid)
      UserService.bindOrUpdateMobile(email, newMobile)
    } map {
      updateRes =>
      val newSession = request.session + (Constant.cookieNameMobile -> newMobile) +  (Constant.cookieNameMobileVerified -> "true")
      Ok(updateRes.toJson).withSession(newSession)
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
    Ok(views.html.requestpwdresetpage.render(request.session, langFromRequestCookie(request)))
  }

  def requestPasswordReset(email: String) = Action.async {
    implicit request =>
      logger.info(s"reset password email: $email")
      validateParamsAndThen(
        new EmailFormatValidator(email)
      ) {
        UserService.requestPasswordReset(email)
      } map {
        result =>
          //logger.info(s"result: $result")
          if (result.success) {
            Redirect(routes.MainController.prompt("prompt.resetPwdEmailSent"))
          } else {
            Redirect(routes.MainController.prompt("prompt.resetPwdEmailSent"))
            //Redirect(routes.MainController.prompt("prompt.requestResetPwdFailed"))
          }
      }
  }

  def validatePasswordReset(token: String) = Action.async {
    implicit request =>
      //logger.info(s"password reset token: $token")
      UserService.validatePasswordResetToken(token) map {
        result =>
          if (result.success) {
            Ok(views.html.doPasswordReset.render(token, request.session, langFromRequestCookie(request)))
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
      //logger.info(s"newPassword: $newPassword, token: $toke")
      UserService.resetPassword(newPassword, token) map {
        result =>
        if(result.success)
          Ok(result.toJson).withNewSession
        else
          Ok(result.toJson)
      }
  }

  def doChangePassword() = Action.async(parse.urlFormEncoded) {
    implicit request =>
      val data = request.body
      val email = request.session.get("username").getOrElse("")
      val oldPassword = getParam(data, "oldPassword").getOrElse("")
      val newPassword = getParam(data, "newPassword").getOrElse("")
      UserService.changePassword(email, oldPassword, newPassword) map {
        result =>
          Ok(result.toJson)
      }
  }

  def resendVerifyEmail(email: String) = Action.async {
    implicit request =>
      validateParamsAndThen(
        new EmailFormatValidator(email)
      ) {
        UserService.resendVerifyEmail(email)
      } map {
        result =>
          //logger.info(s"result: $result")
        if (result.success) {
          Redirect(routes.MainController.prompt("prompt.resendVerifyEmailSucceedded"))
        } else {
          logger.warn(s"resend verify email failed. email: $email")
          Redirect(routes.MainController.prompt("prompt.resendVerifyEmailFailed"))
        }
      }
  }

  // def accountSettingsView() = Authenticated {
  //   implicit request =>
  //     Ok(views.html.viewAccountSettings.render(request.session, langFromRequestCookie(request)))
  // }

  // def accountProfiles() = Authenticated.async {
  //   implicit request =>
  //     UserService.getApiSecret(request.session.get("uid").get.toLong) map {
  //       result =>
  //       if (result.success) {
  //         val apiToken = result.data.getOrElse("").asInstanceOf[String]
  //         Ok(views.html.viewAccountProfile.render(apiToken, request.session, langFromRequestCookie(request)))
  //       } else {
  //         Ok(views.html.viewAccountProfile.render("", request.session, langFromRequestCookie(request)))
  //       }
  //     }
  // }

  def generateApiToken = Authenticated.async {
    implicit request =>
      UserService.generateApiSecret(request.session.get("uid").get.toLong) map {
        result =>
        Ok(result.toJson)
        // if (result.success) {
        //   val apiToken = result.data.getOrElse("").asInstanceOf[String]
        //   //Ok(views.html.viewAccountProfile.render(apiToken, request.session, langFromRequestCookie(request)))
        // } else {
        //   //Ok(views.html.viewAccountProfile.render("", request.session, langFromRequestCookie(request)))
        // }
      }
  }

  def addBankCard = Authenticated.async(parse.urlFormEncoded) {
    implicit request =>
      val uid = request.session.get("uid").get.toLong
      val data = request.body
      val bankName = getParam(data, "bankName").getOrElse("")
      val ownerName = getParam(data, "ownerName").getOrElse("")
      val cardNumber = getParam(data, "cardNumber").getOrElse("")
      val branchBankName = getParam(data, "branchBankName").getOrElse("")
      val emailCode = getParam(data, "emailCode").getOrElse("")
      val uuid = getParam(data, "verifyCodeUuidEmail").getOrElse("")

    //println(s"bankName: $bankName, ownerName: $ownerName, cardNumber: $cardNumber, emailCode: $emailCode, uuid: $uuid")

    validateParamsAndThen(
      new StringNonemptyValidator(bankName, ownerName, cardNumber, emailCode, uuid),
      new CachedValueValidator(ErrorCode.InvalidEmailVerifyCode, true, uuid, emailCode)
    ) {
      popCachedValue(uuid)
      UserService.addBankCard(uid, bankName, ownerName, cardNumber, branchBankName)
    } map {
        result =>
        Ok(result.toJson)
      }
  }

  def deleteBankCard = Authenticated.async(parse.urlFormEncoded) {
    implicit request =>
      val uid = request.session.get("uid").get.toLong
      val data = request.body
      val cardNumber = getParam(data, "cardNumber").getOrElse("")
      UserService.deleteBankCard(uid, cardNumber) map {
        result =>
        Ok(result.toJson)
      }
  }

  def queryBankCards = Authenticated.async {
    implicit request =>
      val uid = request.session.get("uid").get.toLong
      UserService.queryBankCards(uid) map {
        result =>
        Ok(result.toJson)
      }
  }

  // def googleauthView() = Action {
  //   implicit request =>
  //     Ok(views.html.viewGoogleAuth.render(request.session, langFromRequestCookie(request)))
  // }

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
      val phonePrefer = getParam(data, "phoneprefer").getOrElse("1")
      val prefer = SecurityPreferenceUtil.updateMobileVerification(
        request.session.get(Constant.securityPreference), phonePrefer)
      logger.info(s"phoneCode: $phoneCode, phonePrefer: $phonePrefer, prefer: $prefer, uuid=$uuid")

      validateParamsAndThen(
        new CachedValueValidator(ErrorCode.SmsCodeNotMatch, true, uuid, phoneCode)
      ) {
        popCachedValue(uuid)
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
      val emailPrefer = getParam(data, "emailprefer").getOrElse("1")
      val prefer = SecurityPreferenceUtil.updateEmailVerification(
        request.session.get(Constant.securityPreference), emailPrefer)
      logger.info(s"emailCode: $emailCode, emailPrefer: $emailPrefer, prefer: $prefer, uuid= $uuid")

      validateParamsAndThen(
        new CachedValueValidator(ErrorCode.InvalidEmailVerifyCode, true, uuid, emailCode)
      ) {
        popCachedValue(uuid)
        UserService.setUserSecurityPreference(userId.toLong, prefer)
      } map {
        result =>
          if (result.success) {
            val newSession = request.session + (Constant.securityPreference -> prefer)
            Ok(result.toJson()).withSession(newSession)
          } else Ok(result.toJson())
      }
  }

  def updateNickName() = Authenticated.async(parse.urlFormEncoded) {
    implicit request =>
    val data = request.body
    val userId = request.session.get("uid").get.toLong
    val nickname = getParam(data, "nickname").getOrElse("")
    val cleanNickName = nickname.replaceAll("[^a-zA-Z0-9.]", "")
    UserService.updateNickName(userId, cleanNickName).map {
      result =>
      if (result.success) {
        val newSession = request.session + (Constant.cookieNameRealName -> cleanNickName)
        Ok(result.toJson()).withSession(newSession)
      } else Ok(result.toJson())
    }
  }

  def verifyRealName() = Authenticated.async(parse.urlFormEncoded) {
    implicit request =>
    val data = request.body
    val userId = request.session.get("uid").get.toLong
    val realName = getParam(data, "realName").getOrElse("")
    val location = getParam(data, "location").getOrElse("")
    val identiType = getParam(data, "identiType").getOrElse("")
    val idNumber = getParam(data, "idNumber").getOrElse("")
    // println(s"verifyRealName: realName=$realName, location=$location, identiType=$identiType, idNumber=$idNumber")
    UserService.verifyRealName(userId, realName, location, identiType, idNumber).map {
      result =>
      if (result.success) {
        val newSession = request.session + (Constant.userRealName -> realName)
        Ok(result.toJson()).withSession(newSession)
      } else Ok(result.toJson())
    }
  }

  def logout = Action {
    implicit request =>
      Redirect(routes.MainController.index()).withNewSession
  }

}
