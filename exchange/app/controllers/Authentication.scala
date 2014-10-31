package controllers

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import utils.Constant._
import com.coinport.coinex.api.model._
import com.coinport.coinex.data.UserStatus
import com.coinport.coinex.api.service.UserService
import services.CacheService

trait AuthenticateHelper {
  val ajaxRequestHeaderKey="ajaxRequestKey"
  val ajaxRequestHeadervalue="value"

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

object Authenticated extends ActionBuilder[Request] with AuthenticateHelper {
  val cache = CacheService.getDefaultServiceImpl

  private def checkUserSuspended[A](uid: Long, request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
    UserService.getProfile(uid) flatMap {
      result =>
      if (result.success) {
        val user = result.data.get.asInstanceOf[User]
        if (user.status == UserStatus.Suspended) {
          Future(Unauthorized) // TODO notify user for account been suspended.
        } else {
          block(request).map(_.withCookies(
            Cookie(cookieNameTimestamp, System.currentTimeMillis.toString)))
        }
      } else {
        Future(Unauthorized.withNewSession)
      }
    }
  }

  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    request.session.get("uid").map { uid =>
      val currTs = System.currentTimeMillis
      request.cookies.get(cookieNameTimestamp).map {
        tsCookie =>
        //logger.info(s"timestamp cookie: $tsCookie, currtime: $currTs, timeoutMillis: $timeoutMillis")
        val ts = tsCookie.value.toLong
        val csrfToken = cache.get("csrf-" + uid)

        if (currTs - ts > timeoutMillis) {
          Future(Unauthorized.withNewSession)
        } else {
          checkUserSuspended(uid.toLong, request, block)
        }
      } getOrElse {
        block(request).map(_.withCookies(
          Cookie(cookieNameTimestamp, currTs.toString)))
      }
    } getOrElse {
      val userIdOpt = request.headers.get("USERID")
      val apiTokenOpt = request.headers.get("API-TOKEN")
      logger.info(s"authenticate for api request, apiToken: $apiTokenOpt, userId: $userIdOpt")
      if (accessControl(userIdOpt, apiTokenOpt)) {
        UserService.getApiSecret(userIdOpt.get.toLong) flatMap {
          case ApiResult(success, _, _, secretOpt) if success &&
              secretOpt.isDefined && secretOpt == apiTokenOpt =>
            block(request)
          case e =>
            logger.error(s"apiToken invalid. res=$e")
            Future(Unauthorized)
        }
      } else {
        logger.info(s"accessControl failed.")
        Future(Unauthorized)
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
            Cookie(cookieNameTimestamp, System.currentTimeMillis.toString)))
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
          Cookie(cookieNameTimestamp, currTs.toString)))
      }
    } getOrElse {
      val redirectUri = "/login?msg=authenticateNotLogin"
      responseOnRequestHeader(request, redirectUri)
      //Future(Unauthorized)
    }
  }
}
