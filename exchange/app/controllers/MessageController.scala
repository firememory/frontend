/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package controllers

import play.api.mvc._

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.coinport.coinex.data._
import scala.concurrent.Future
import scala.Some
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data.Implicits._
import models._
import models.OperationEnum._
import services.{MarketService, AccountService}
import com.coinport.coinex.data.ChartTimeDimension._
import play.api.libs.json.Json
import com.github.tototoshi.play2.json4s.native.Json4s
import org.json4s.{NoTypeHints, Extraction}
import org.json4s.native.Serialization
import org.json4s.ext.EnumNameSerializer

object MessageController extends Controller with Json4s {

  import akka.util.Timeout
  import scala.concurrent.duration._

  val minute = 60 * 1000
  val hour = 60 * 60 * 1000
  val day = 24 * 60 * 60 * 1000
  val week = 7 * 24 * 60 * 60 * 1000

  implicit val timeout = Timeout(2 seconds)
  implicit val formats = Serialization.formats(NoTypeHints) +
    new EnumNameSerializer(OperationEnum)

  def price = Action {
    Ok("TODO")
  }

  def depth = Action.async {
    implicit request =>
      val query = request.queryString
      val depth = getParam(query, "depth", "5").toInt

      MarketService.getDepth(Btc ~> Rmb, depth).map(result => Ok(result.toJson))
  }

  def getUserOrders = Action.async {
    implicit request =>
      session.get("uid") match {
        case Some(uid) =>
          AccountService.getOrders(uid.toLong) map {
            case result: ApiResult =>
              Ok(result.toJson)
          }
        case None =>
          Future {
            Ok("unauthorised request")
          }
      }
  }

  def submitOrder() = Action.async(parse.json) {
    implicit request =>
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
          val operation = orderType match {
            case "bid" => Buy
            case "ask" => Sell
          }
          val order = UserOrder(id.toLong, operation, Btc, Rmb, price, amount, total, submitTime = System.currentTimeMillis)

          AccountService.submitOrder(order).map(result => Ok(result.toJson))
        case None =>
          Future {
            Ok(ApiResult(false, -1, "unauthorised request").toJson)
          }
      }
  }

  def cancelOrder(id: String) = Action.async {
    implicit request =>
      session.get("uid") match {
        case Some(uid) =>
          AccountService.cancelOrder(id.toLong, uid.toLong).map(result => Ok(result.toJson()))
        case None =>
          Future {
            Ok(ApiResult(false, -1, "unauthorised request").toJson)
          }
      }
  }

  def account = Action.async {
    implicit request =>
      println("query account")
      session.get("uid") match {
        case Some(id) =>
          println("send query with uid: " + id)
          AccountService.getAccount(id.toLong) map {
            case result =>
              println("got response: " + result)
              Ok(result.toJson)
          }
        case None =>
          Future {
            Ok("unauthorised request")
          }
      }
  }

  def deposit = Action.async(parse.json) {
    implicit request =>
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
    implicit request =>
      val query = request.queryString
      val timeDimension = ChartTimeDimension(getParam(query, "period", "1").toInt)
      val fromParam = getParam(query, "from", "")
      val toParam = getParam(query, "to", "")
      val defaultTo = System.currentTimeMillis()
      // return 90 items by default
      val defaultFrom = defaultTo - getTimeSkip(timeDimension) * 180
      val from = if (fromParam.isEmpty) defaultFrom else fromParam.toLong
      val to = if (toParam.isEmpty) defaultTo else toParam.toLong

      MarketService.getHistory(Btc ~> Rmb, timeDimension, from, to) map {
        case candles: CandleData =>
          val map = candles.items.map(i => i.timestamp -> i).toMap
          val timeSkip = getTimeSkip(timeDimension)
          var open = 0.0
          val list = (from / timeSkip to to / timeSkip).map {
            key: Long =>
              map.get(key) match {
                case Some(item) =>
                  open = item.close
                  CandleDataItem(key * timeSkip, item.volumn, item.open, item.close, item.low, item.high)
                case None =>
                  CandleDataItem(key * timeSkip, 0, open, open, open, open)
              }
          }.toSeq
          Ok(Json.toJson(list))
      }
  }

  def transaction = Action.async {
    implicit request =>
      val query = request.queryString
      val limit = getParam(query, "limit", "20").toInt
      val skip = getParam(query, "skip", "0").toInt

      MarketService.getAllTransactions(Btc ~> Rmb, skip, limit) map {
        case transactionData: TransactionData =>
          Ok(Json.toJson(transactionData.items))
      }
  }

  def userTransaction = Action.async {
    implicit request =>
      session.get("uid") match {
        case Some(userId) =>
          val query = request.queryString
          val orderId = getParam(query, "oid", "-1").toInt
          val limit = getParam(query, "limit", "20").toInt
          val skip = getParam(query, "skip", "0").toInt

          MarketService.getUserTransactions(Btc ~> Rmb, userId.toLong, orderId, skip, limit) map {
            case transactionData: TransactionData =>
              Ok(Json.toJson(transactionData.items))
          }
        case None => Future {
          Ok("unauthorised request")
        }
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

  private def getParam(queryString: Map[String, Seq[String]], param: String, default: String): String = {
    queryString.get(param) match {
      case Some(seq) =>
        if (seq.isEmpty) default else seq(0)
      case None =>
        default
    }
  }
}