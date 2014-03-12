import play.api._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    println("Application has started")
  }

  override def onStop(app: Application) {
    println("Application shutdown...")
  }
}