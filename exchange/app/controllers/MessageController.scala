/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package controllers

import play.api.mvc._

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import play.api.libs.json.Json
import com.coinport.coinex.data._
import scala.concurrent.Future
import scala.Some
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data.Implicits._
import models.Implicits._
import models.Operation._
import services.{MarketService, AccountService}
import models.{ApiResult, UserOrder}

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

  def getUserOrders = Action.async { implicit request =>
    session.get("uid") match {
      case Some(uid) =>
        AccountService.getOrders(uid.toLong) map {
          case result: QueryUserOrdersResult =>
            println("got " + result)
            Ok(Json.toJson(result.orders.map(o => UserOrder.fromOrderInfo(o))))
          case x =>
            println("response: " + x.toString)
            Ok(Json.toJson(ApiResult(false, -1, x.toString)))
        }
      case None =>
        Future{
          Ok("unauthorised request")
        }
    }
  }

  def submitOrder() = Action.async(parse.json) { implicit request =>
    val json = request.body
    session.get("uid") match {
      case Some(id) =>
        val orderType = (json \ "type").as[String]
        val priceField = (json \ "price").as[Double]
        val amountField = (json \ "amount").as[Double]
        val totalField = (json \ "total").as[Double]

        val price = if (priceField <= 0) None else Some(priceField)
        val amount = if (amountField <= 0) None else Some(amountField)
        val total = if (totalField <= 0) None else Some(totalField)

        println(orderType + ": " + json)

        val operation = orderType match {case "bid" => Buy case "ask" => Sell}
        val order = UserOrder(id.toLong, operation, Btc, Rmb, price, amount, total, OrderStatus.Pending, System.currentTimeMillis)

        AccountService.submitOrder(order) map {
          case x =>
            println(x)
            Ok(x.toString)
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
      val amount = (json \ "amount").as[Double]
      val currency: Currency = (json \ "type").as[String]
      AccountService.deposit(uid.toLong, currency, amount) map {
      case x =>
        println(x)
        Ok("backend reply: " + x.toString)
      }
    }
  }

  def history = Action.async { implicit request =>
    MarketService.getHistory(Btc ~> Rmb) map {
      case result: QueryMarketCandleDataResult  =>
        println("result: " + result)
        Ok(Json.toJson(result))
    }
  }
}