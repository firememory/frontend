import akka.actor.ActorSystem
import akka.util.Timeout
import com.coinport.coinex.data.Currency.Btc
import com.coinport.coinex.LocalRouters
import com.typesafe.config.ConfigFactory
import play.api._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    println("Application has started")
  }

  override def onStop(app: Application) {
    println("Application shutdown...")
  }
}