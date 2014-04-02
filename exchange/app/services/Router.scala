/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package services

import com.typesafe.config.ConfigFactory
import akka.actor.{Props, ActorSystem}
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.{Coinex, LocalRouters}
import akka.cluster.Cluster
import com.coinport.coinex.data.Implicits._
import play.libs.Akka
import actors.HelloActor

object Router extends AkkaService{
  val defaultAkkaConfig = "akka.conf"
  val akkaConfigProp = System.getProperty("akka.config")
  val akkaConfigResource = if (akkaConfigProp != null) akkaConfigProp else defaultAkkaConfig

  println("=" * 20 + "  Akka config  " + "=" * 20)
  println("  conf/" + akkaConfigResource)
  println("=" * 55)

  val config = ConfigFactory.load(akkaConfigResource)
  implicit val system = ActorSystem("coinex", config)
  implicit val cluster = Cluster(system)
  val markets = Seq(Btc ~> Rmb)

  val routers = new LocalRouters(markets)
  val backend = system.actorOf(Props(new Coinex(routers)), name = "backend")
}
