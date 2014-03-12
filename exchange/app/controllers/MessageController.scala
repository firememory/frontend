package controllers

import play.api.mvc._

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import play.api.libs.json.Json
import com.coinport.coinex.data._
import scala.concurrent.Future
import scala.Some
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data.Implicits._
import com.typesafe.config.ConfigFactory
import models.Implicits._
import services.{MarketService, AccountService}

object MessageController extends Controller {
  def price = Action {
    Ok("TODO")
  }

  def depth = Action.async {
    MarketService.getDepth(Btc ~> Rmb, 5) map {
      case result: QueryMarketResult =>
        println("result: " + result)
        Ok(Json.toJson(result))
    }
  }

  def order = Action {
    Ok("TODO")
  }

  def submitOrder() = Action.async(parse.json) { implicit request =>
    val json = request.body
    session.get("uid") match {
      case Some(id) =>
        val orderType = (json \ "type").as[String]
        val price = (json \ "price").as[Double]
        val amount = (json \ "amount").as[Long]
        println(orderType + ": " + json)

        if (orderType equals "bid") {
          AccountService.submitOrder(Rmb ~> Btc, id.toLong, (price * amount).toLong, 1 / price) map {
            case x =>
              println(x)
              Ok(x.toString)
          }
        } else {
          AccountService.submitOrder(Btc ~> Rmb, id.toLong, amount, price) map {
            case x =>
              println(x)
              Ok(x.toString)
          }
        }
      case None =>
        Future{
          Ok("unauthorised request")
        }
    }
  }

  def account = Action.async { implicit request =>
    println("query account")
    session.get("uid") match {
      case Some(id) =>
          println("send query with uid: " + id)
          AccountService.getAccount(id.toLong) map {
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
      val currency = (json \ "type").as[String] match {case "RMB" => Rmb case "BTC" => Btc}
      AccountService.deposit(uid.toLong, currency, amount) map {
      case x =>
        println(x)
        Ok("backend reply: " + x.toString)
      }
    }
  }
}