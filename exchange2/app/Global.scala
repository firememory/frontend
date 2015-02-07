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
          "Access-Control-Allow-Origin" -> "*",
          "Access-Control-Expose-Headers" -> "WWW-Authenticate, Server-Authorization",
          "Access-Control-Allow-Methods" -> "POST, GET, OPTIONS, PUT, DELETE",
          "Access-Control-Allow-Headers" -> "Authorization,X-XSRF-TOKEN,x-requested-with,content-type,Cache-Control,Pragma,Date")
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
