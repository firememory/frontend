package controllers

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._
import scala.collection.mutable.SortedSet
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import utils.Constant._
import com.coinport.coinex.api.model._
import com.coinport.coinex.data.UserStatus
import com.coinport.coinex.data.ApiSecret
import utils.MHash
import com.coinport.coinex.api.service.UserService
import services.CacheService
import java.security.MessageDigest
import com.google.common.io.BaseEncoding

case class RequestWithUserId[A](val userId: Long, request: Request[A]) extends WrappedRequest[A](request)

trait AuthenticateHelper {
  val ajaxRequestHeaderKey = "ajaxRequestKey"
  val ajaxRequestHeadervalue = "value"

  val logger = Logger(this.getClass)
  val sysConfig = Play.current.configuration
  val timeoutMinutes: Int = sysConfig.getInt("session.timeout.minutes").getOrElse(60)
  val timeoutMillis: Long = timeoutMinutes * 60 * 1000

  def responseOnRequestHeader[A](request: Request[A], redirectUri: String): Future[Result] = {
    val ajaxRequestHeader = request.headers.get(ajaxRequestHeaderKey).getOrElse("")
    if (ajaxRequestHeadervalue.equals(ajaxRequestHeader)) {
      Future(Unauthorized)
    } else {
      Future(Redirect(redirectUri).withNewSession)
    }
  }
}

object Authenticated extends ActionBuilder[RequestWithUserId] with AuthenticateHelper {
  val cache = CacheService.getDefaultServiceImpl

  private def checkUserSuspended[A](uid: Long, request: Request[A], block: (RequestWithUserId[A]) => Future[Result]): Future[Result] = {
    UserService.getProfile(uid) flatMap {
      result =>
        if (result.success) {
          val user = result.data.get.asInstanceOf[User]
          if (user.status == UserStatus.Suspended) {
            Future(Unauthorized) // TODO notify user for account been suspended.
          } else {
            block(RequestWithUserId(uid, request)).map(_.withCookies(
              Cookie(cookieNameTimestamp, System.currentTimeMillis.toString, domain = Some(".coinport.com"))))
          }
        } else {
          Future(Unauthorized.withNewSession)
        }
    }
  }

  def invokeBlock[A](request: Request[A], block: (RequestWithUserId[A]) => Future[Result]) = {
    request.session.get("uid").map { uid =>
      val currTs = System.currentTimeMillis
      request.cookies.get(cookieNameTimestamp).map {
        tsCookie =>
          //logger.info(s"timestamp cookie: $tsCookie, currtime: $currTs, timeoutMillis: $timeoutMillis")
          val ts = tsCookie.value.toLong
          val csrfToken = cache.get("csrf-" + uid)

          if (currTs - ts > timeoutMillis) {
            logger.error(s"account authentication failed: timeout")
            Future(Unauthorized.withNewSession)
          } else if (!request.headers.get("X-XSRF-TOKEN").equals(Some(csrfToken))) {
            logger.error(s"account authentication failed: xsrf-token not match")
            Future(Unauthorized.withNewSession)
          } else {
            checkUserSuspended(uid.toLong, request, block)
          }
      } getOrElse {
        block(RequestWithUserId(uid.toLong, request)).map(_.withCookies(
          Cookie(cookieNameTimestamp, currTs.toString, domain = Some(".coinport.com"))))
      }
    } getOrElse {
      val userIdOpt = request.headers.get("USERID")
      val apiTokenOpt = request.headers.get("API-TOKEN")
      if (userIdOpt.isDefined && apiTokenOpt.isDefined) {
        logger.info(s"=============== api V1 used")
        logger.info(s"authenticate for api request, apiToken: $apiTokenOpt, userId: $userIdOpt")
        if (accessControl(userIdOpt, apiTokenOpt)) {
          UserService.getApiSecret(userIdOpt.get.toLong) flatMap {
            case ApiResult(success, _, _, secretOpt) if success &&
              secretOpt.isDefined && secretOpt == apiTokenOpt =>
              block(RequestWithUserId(userIdOpt.get.toLong, request))
            case e =>
              logger.error(s"apiToken invalid. res=$e")
              Future(Unauthorized)
          }
        } else {
          logger.info(s"accessControl failed.")
          Future(Unauthorized)
        }
      } else {
        logger.info(s"=============== api V2 used")
        val apiAuthInfos = request.headers.get("Authorization").getOrElse("").split(" ")
        val authType = apiAuthInfos(0)
        if (apiAuthInfos.size > 1 && authType == "Token") {
          val authPairs = apiAuthInfos(1)
          val tokenArr = new java.lang.String(BaseEncoding.base64.decode(authPairs)).split(":")
          val (token, sign) = (tokenArr(0), tokenArr(1))
          UserService.getApiSecret(token) flatMap {
            case ApiResult(success, _, _, secretOpt) if success && secretOpt.isDefined =>
              logger.info(secretOpt.get.asInstanceOf[ApiSecret].toString)
              val userId = secretOpt.get.asInstanceOf[ApiSecret].userId
              // val token = secretOpt.get.asInstanceOf[ApiSecret].identifier
              val secret = secretOpt.get.asInstanceOf[ApiSecret].secret
              if (accessControl(Some(userId.get.toString), Some(token))) {
                //TODO(xiaolu) change post json data to string
                val requestParams: String = if (request.method == "GET") combineParams(request.queryString.map(kv => kv._1 -> kv._2.head))
                else request.body.toString
                if (verifySign(requestParams, sign, secret)) {
                  block(RequestWithUserId(userId.get, request))
                } else {
                  logger.error(s"sign is invalid. ")
                  // Future(ApiResult(false, 0, "sign is invalid").toJson)
                  Future(Unauthorized)
                }
              } else {
                logger.info(s"accessControl failed.")
                Future(Unauthorized)
              }
            case errResult => {
              Future(Unauthorized)
            }
          }
        } else if (apiAuthInfos.size > 1 && authType == "Basic") {
          val authPairs = apiAuthInfos(1)
          val tokenArr = new java.lang.String(BaseEncoding.base64.decode(authPairs)).split(":")
          val (username, pwd) = (tokenArr(0), tokenArr(1))
          val hashedPwd = MHash.sha256Base64(pwd)
          val user: User = User(id = -1, email = username, password = hashedPwd)
          UserService.login(user) flatMap { result =>
            if (result.success) {
              val profile = result.data.get.asInstanceOf[User]
              block(RequestWithUserId(profile.id, request))
            } else {
              logger.error("password or username is invalid.")
              Future(Unauthorized)
            }
          }

        } else {
          logger.error("auth info is invalid.")
          Future(Unauthorized)
        }
      }
    }
  }

  private def accessControl(uidOpt: Option[String], tokenOpt: Option[String]) =
    if (uidOpt.isDefined && tokenOpt.isDefined) {
      val minInvokeIntervalMillis = 1000L
      val key = uidOpt.get + "-" + tokenOpt.get
      val currTs = System.currentTimeMillis
      val cachedTsStr = cache.get(key)
      val cachedTs = if (cachedTsStr == null) 0L else cachedTsStr.toLong
      logger.info(s"accessControl: Key = $key, currTs = $currTs, cachedTs = $cachedTs, interval = ${currTs - cachedTs}")

      cache.putWithTimeout(key, currTs.toString(), 60)

      (currTs - cachedTs) > minInvokeIntervalMillis
    } else {
      logger.info("accessControl error: USERID and APT-TOKEN not found.")
      false
    }

  private def verifySign(params: String, sign: String, secret: String) = {
    val signInServer = md5(params + "&secret=" + secret)
    logger.info("sign in server is : " + signInServer)
    sign == signInServer
  }

  private def combineParams(params: Map[String, String]): String = {
    val sortedParams = SortedSet.empty[String] ++ params.keySet
    sortedParams.map(item => item + "=" + params(item)).mkString("&")
  }

  private def md5(s: String) = {
    val md: MessageDigest = MessageDigest.getInstance("MD5")
    md.update(s.getBytes("UTF-8"))
    val digestBytes = md.digest()
    digestBytes.map { b =>
      if (Integer.toHexString(0xFF & b).length() == 1)
        "0" + Integer.toHexString(0xFF & b)
      else
        Integer.toHexString(0xFF & b)
    }.mkString
  }
}

object AuthenticatedOrRedirect extends ActionBuilder[Request] with AuthenticateHelper {

  private def checkUserSuspended[A](uid: Long, request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
    UserService.getProfile(uid) flatMap {
      result =>
        if (result.success) {
          val user = result.data.get.asInstanceOf[User]
          if (user.status == UserStatus.Suspended) {
            val redirectUri = "/login?msg=authenticateUserSuspended"
            responseOnRequestHeader(request, redirectUri)
            // TODO notify user for account been suspended.
          } else {
            block(request).map(_.withCookies(
              Cookie(cookieNameTimestamp, System.currentTimeMillis.toString, domain = Some(".coinport.com"))))
          }
        } else {
          val redirectUri = "/login?msg=authenticateNotLogin"
          responseOnRequestHeader(request, redirectUri)
        }
    }
  }

  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    // check login and session timeout here:
    request.session.get("uid").map { uid =>
      val currTs = System.currentTimeMillis
      request.cookies.get(cookieNameTimestamp).map {
        tsCookie =>
          //logger.info(s"timestamp cookie: $tsCookie, currtime: $currTs, timeoutMillis: $timeoutMillis")
          val ts = tsCookie.value.toLong
          if (currTs - ts > timeoutMillis) {
            val redirectUri = "/login?msg=authenticateTimeout"
            responseOnRequestHeader(request, redirectUri)
            //Future(Unauthorized)
          } else {
            checkUserSuspended(uid.toLong, request, block)
          }
      } getOrElse {
        block(request).map(_.withCookies(
          Cookie(cookieNameTimestamp, currTs.toString, domain = Some(".coinport.com"))))
      }
    } getOrElse {
      val redirectUri = "/login?msg=authenticateNotLogin"
      responseOnRequestHeader(request, redirectUri)
      //Future(Unauthorized)
    }
  }
}
