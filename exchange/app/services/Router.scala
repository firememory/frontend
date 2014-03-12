package services

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.LocalRouters
import akka.cluster.Cluster
import com.coinport.coinex.data.Implicits._

object Router extends AkkaService{
  val config = ConfigFactory.load("akka.conf")
  implicit val system = ActorSystem("coinex", config)

  implicit val cluster = Cluster(system)
  val markets = Seq(Btc ~> Rmb)

  val routers = new LocalRouters(markets)
}
