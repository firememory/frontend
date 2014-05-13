import filters._
import play.api._
import play.api.mvc.WithFilters

object Global extends WithFilters(LoggingFilter) {

  override def onStart(app: Application) {
    println("Application has started")
  }

  override def onStop(app: Application) {
    println("Application shutdown...")
  }
}
