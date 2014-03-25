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
import com.coinport.coinex.data.ChartTimeDimension._

object MessageController extends Controller {
  import akka.util.Timeout
  import scala.concurrent.duration._
  import akka.pattern.ask

  val minute = 60 * 1000
  val hour = 60 * 60 * 1000
  val day = 24 * 60 * 60 * 1000
  val week = 7 * 24 * 60 * 60 * 1000

  implicit val timeout = Timeout(2 seconds)
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

  def history = Action.async {

    //    val from = (json \ "from").as[Long]
    //    val to = (json \ "to").as[Long]
    //    val timeDimension = ChartTimeDimension.get((json \ "time").as[Int]).getOrElse(OneMinute)
    val from = System.currentTimeMillis() - 60 * 60 * 1000
    val to = System.currentTimeMillis()
    val timeDimension = OneMinute

    MarketService.getHistory(Btc ~> Rmb, timeDimension, from, to) map {
      case candles: CandleData =>
        val map = candles.items.map(i => i.timestamp -> i).toMap
        val timeSkiper = getTimeSkip(timeDimension)
        var open = 0.0
        val list = (from / timeSkiper to to / timeSkiper).map{key: Long =>
          map.get(key) match {
            case Some(item) =>
              open = item.close
              CandleDataItem(key, item.volumn, item.open, item.close, item.low, item.high)
            case None =>
              CandleDataItem(key, 0, open, open, open, open)
          }
        }.toSeq
        Ok(Json.toJson(list))
    }
  }

  def transaction = Action.async {
    val from = 0
    val num = 100

    MarketService.getAllTransactions(Btc ~> Rmb, from, num) map {
      case transactionData: TransactionData =>
        Ok(Json.toJson(transactionData.items))
    }
  }

  private def getTimeSkip(dimension: ChartTimeDimension) = dimension match {
    case OneMinute => minute
    case ThreeMinutes => 3 * minute
    case FiveMinutes => 5 * minute
    case FifteenMinutes => 15 * minute
    case ThirtyMinutes => 30 * minute
    case OneHour => hour
    case TwoHours => 2 * hour
    case FourHours => 4 * hour
    case SixHours => 6 * hour
    case TwelveHours => 12 * hour
    case OneDay => day
    case ThreeDays => 3 * day
    case OneWeek => week
  }
}