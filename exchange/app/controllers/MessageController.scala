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

  implicit val timeout = Timeout(2 seconds)

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
          AccountService.getOrders(Btc ~> Rmb, Some(uid.toLong), None, None, 0, 30) map {
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
      val defaultFrom = defaultTo - timeDimension * 180
      val from = if (fromParam.isEmpty) defaultFrom else fromParam.toLong
      val to = if (toParam.isEmpty) defaultTo else toParam.toLong

      MarketService.getHistory(Btc ~> Rmb, timeDimension, from, to).map(result => Ok(result.toJson))
  }

  def transaction = Action.async {
    implicit request =>
      val query = request.queryString
      val limit = getParam(query, "limit", "20").toInt
      val skip = getParam(query, "skip", "0").toInt

      MarketService.getGlobalTransactions(Btc ~> Rmb, skip, limit).map(result => Ok(result.toJson))
  }

  def userTransaction = Action.async {
    implicit request =>
      session.get("uid") match {
        case Some(userId) =>
          val query = request.queryString
//          val orderId = getParam(query, "oid").map(_.toInt)
          val limit = getParam(query, "limit", "20").toInt
          val skip = getParam(query, "skip", "0").toInt

          MarketService.getTransactionsByUser(Btc ~> Rmb, userId.toLong, skip, limit).map(result => Ok(result.toJson))
        case None => Future {
          Ok("unauthorised request")
        }
      }
  }

  def tickers = Action.async { implicit request =>
    MarketService.getTickers().map(result => Ok(result.toJson))
  }

  private def getParam(queryString: Map[String, Seq[String]], param: String): Option[String] = {
    queryString.get(param).map(_(0))
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