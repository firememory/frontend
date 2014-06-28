import filters._
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    println("Application has started")
  }

  override def onStop(app: Application) {
    println("Application shutdown...")
  }

  override def doFilter(next: EssentialAction): EssentialAction = {
    Filters(super.doFilter(next), LoggingFilter)
  }

   override def onHandlerNotFound(request: RequestHeader) = {
     Future.successful(NotFound(
       views.html.notFoundPage(request.path)
     ))
  }

  override def onBadRequest(request: RequestHeader, error: String) = {
    Future.successful(BadRequest("Bad Request: " + error))
  }

  // override def onError(request: RequestHeader, ex: Throwable) = {
  //   Future.successful(InternalServerError(
  //     views.html.errorPage("internalServerErrorKey")
  //   ))
  // }

}
