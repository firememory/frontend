package controllers

import play.api.mvc._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import akka.pattern.ask

import akka.util.Timeout
import akka.actor._
import akka.persistence.Persistent
import akka.cluster.Cluster
import play.api.libs.json.{JsString, Writes, Json}
import com.coinport.coinex.data._
import com.coinport.coinex.{LocalRouters}
import scala.concurrent.Future
import com.coinport.coinex.data.DoDepositCash
import scala.Some
import akka.cluster.routing.ClusterRouterGroupSettings
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data.Implicits._

object MessageController extends Controller {

  implicit val timeout = Timeout(2 seconds)
//  val config = ConfigFactory.load("akka.conf")
  implicit val system = ActorSystem("coinex")//ActorSystem("coinex", config)

  implicit val cluster = Cluster(system)
  val markets = Seq(Btc ~> Rmb)

  val routers = new LocalRouters(markets)

  implicit val cashAccountWrites = new Writes[CashAccount] {
    def writes(cachAccount: CashAccount) = Json.obj(
      "currency" -> JsString(cachAccount.currency.toString),
      "available" -> cachAccount.available,
      "locked" -> cachAccount.locked
    )
  }


  implicit val queryAccountResultWrites = new Writes[QueryAccountResult] {
    def writes(obj: QueryAccountResult) = Json.obj(
      "uid" -> obj.userAccount.userId,
      "RMB" -> obj.userAccount.cashAccounts.getOrElse(Rmb, CashAccount(Rmb, 0, 0)).available,
      "BTC" -> obj.userAccount.cashAccounts.getOrElse(Btc, CashAccount(Btc, 0, 0)).available,
      "accounts" -> obj.userAccount.cashAccounts.map(_._2)
    )
  }

  implicit val orderWrites = new Writes[Order] {
    def writes(obj: Order) = Json.arr(
      obj.price,
      obj.quantity,
      obj.userId
    )
  }

  implicit val queryMarketResultWrites = new Writes[QueryMarketResult] {
    def writes(obj: QueryMarketResult) = Json.obj(
      "asks" -> obj.orders1,
      "bids" -> obj.orders2.map(_.inversePrice)
    )
  }

  def price = Action {
    Ok("TODO")
  }

  def depth = Action.async {
    routers.marketViews(Btc ~> Rmb) ! DebugDump
    routers.marketViews(Btc ~> Rmb) ? QueryMarket(Btc ~> Rmb, 5) map {
      case result: QueryMarketResult =>
        println("result: " + result)
        Ok(Json.toJson(result))
    }
  }

  def order = Action {
    Ok("TODO")
  }

  def submitOrder() = Action(parse.json) { implicit request =>
    val json = request.body
    session.get("uid") match {
      case Some(id) =>
        val orderType = (json \ "type").as[String]
        val price = (json \ "price").as[Double]
        val amount = (json \ "amount").as[Long]
        println(orderType + ": " + json)

        if (orderType equals "bid") {
          routers.accountProcessor ! DoSubmitOrder(Rmb ~> Btc, Order(id.toLong, System.currentTimeMillis, (price * amount).toLong, Some(1 / price)))
        } else {
          routers.accountProcessor ! DoSubmitOrder(Btc ~> Rmb, Order(id.toLong, System.currentTimeMillis, amount, Some(price)))
        }
        Ok("done.")
      case None =>
        Ok("unauthorised request")
    }
  }

  def account = Action.async { implicit request =>
    println("query account")
    session.get("uid") match {
      case Some(id) =>
          println("send query with uid: " + id)
          routers.accountView ? QueryAccount(id.toLong) map {
          case result: QueryAccountResult =>
            println("got response: " + result)
            Ok(Json.toJson(result))
        }
      case None =>
      Future {
        Ok("unauthorised request")
      }
    }
  }

  def deposit = Action.async(parse.json) { implicit request =>
    val json = request.body
    val username = session.get("username").getOrElse(null)
    val uid = session.get("uid").getOrElse(null)
    println("deposit by user: " + username + ", uid: " + uid + ", deposit data: " + json)
    if (username == null || uid == null) {
      Future {
        Ok("unauthorised request")
      }
    } else {
      val amount = (json \ "amount").as[Long]
      routers.accountProcessor ? Persistent(DoDepositCash(uid.toLong, Rmb, amount)) map {
      case x =>
        Ok("backend reply: " + x.toString)
      }
    }
  }
}