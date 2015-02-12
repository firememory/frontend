import filters._
import play.filters.gzip.GzipFilter
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class CorsFilter extends EssentialFilter {
  def apply(next: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader) = {
      next(requestHeader).map { result =>
        result.withHeaders(
          "Access-Control-Allow-Methods" -> "POST, GET, PUT, DELETE, OPTIONS",
          "Access-Control-Allow-Origin" -> requestHeader.headers.get("Origin").getOrElse("*"),
          "Access-Control-Allow-Credentials" -> "true",
          "Access-Control-Allow-Headers" -> "Origin, Authorization, X-XSRF-TOKEN, X-Requested-With, Content-Type, Accept, Referrer, User-Agent")
      }
    }
  }
}

object Global extends WithFilters(new GzipFilter(), new CorsFilter()) with GlobalSettings {

  override def onStart(app: Application) {
    println("Application has started")
  }

  override def onStop(app: Application) {
    println("Application shutdown...")
  }

  override def doFilter(next: EssentialAction): EssentialAction = {
    Filters(super.doFilter(next), CoinportFilter)
  }

  override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(NotFound(
      views.html.notFoundPage(request.path)))
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
