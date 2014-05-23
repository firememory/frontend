package controllers

import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import utils.Constant._
import play.api.Play

trait AuthenticateHelper {
  val ajaxRequestHeaderKey="ajaxRequestKey"
  val ajaxRequestHeadervalue="value"

  val sysConfig = Play.current.configuration
  val timeoutMinutes: Int = sysConfig.getInt("session.timeout.minutes").getOrElse(60)
  val timeoutMillis: Long = timeoutMinutes * 60 * 1000

  def responseOnRequestHeader[A](request: Request[A], redirectUri: String): Future[SimpleResult] = {
    val ajaxRequestHeader = request.headers.get(ajaxRequestHeaderKey).getOrElse("")
    if (ajaxRequestHeadervalue.equals(ajaxRequestHeader)) {
      Future(Unauthorized)
    } else {
      Future(Redirect(redirectUri))
    }
  }
}

object Authenticated extends ActionBuilder[Request] with AuthenticateHelper {

  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[SimpleResult]) = {
    // check login and session timeout here:
    request.session.get("uid").map { uid =>
      val currTs = System.currentTimeMillis
      request.cookies.get(cookieNameTimestamp).map {
        tsCookie =>
        println(s"timestamp cookie: $tsCookie, currtime: $currTs, timeoutMillis: $timeoutMillis")
        val ts = tsCookie.value.toLong
        if (currTs - ts > timeoutMillis) {
          val redirectUri = "/login?showMsg=true&msgKey=authenticateTimeout"
          responseOnRequestHeader(request, redirectUri)
        } else {
          block(request).map(_.withCookies(
            Cookie(cookieNameTimestamp, currTs.toString)))
        }
      } getOrElse {
        block(request).map(_.withCookies(
          Cookie(cookieNameTimestamp, currTs.toString)))
      }
    } getOrElse {
      val redirectUri = "/login?showMsg=true&msgKey=authenticateNotLogin"
      responseOnRequestHeader(request, redirectUri)
    }
  }
}
