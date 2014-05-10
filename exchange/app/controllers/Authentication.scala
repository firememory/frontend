package controllers

import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AuthenticatedRequest[A](val uid: String, request: Request[A]) extends WrappedRequest[A](request)

object Authenticated extends ActionBuilder[AuthenticatedRequest] {
  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {
    request.session.get("uid").map { uid =>
      block(new AuthenticatedRequest(uid, request))
    } getOrElse {
      Future(Unauthorized)
    }
  }
}
