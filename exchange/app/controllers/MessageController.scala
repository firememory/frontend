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
import com.coinport.coinex.api.model._
import com.coinport.coinex.api.service._
import com.github.tototoshi.play2.json4s.native.Json4s
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem

object MessageController extends Controller with Json4s {

  import akka.util.Timeout
  import scala.concurrent.duration._

  implicit val timeout = Timeout(2 seconds)

  def depth = Action.async {
    implicit request =>
      val query = request.queryString
      val depth = getParam(query, "depth", "5").toInt

      MarketService.getDepth(Btc ~> Cny, depth).map(result => Ok(result.toJson))
  }

  def getUserOrders = Action.async {
    implicit request =>
      session.get("uid") match {
        case Some(uid) =>
          AccountService.getOrders(Some(uid.toLong), None, None, 0, 30) map {
            case result: ApiResult =>
              Ok(result.toJson)
          }
        case None => Future(Unauthorized)
      }
  }

  def getOrder(oid: String) = Action.async {
    implicit request =>
      AccountService.getOrders(None, Some(oid.toLong), None, 0, 1).map(result => Ok(result.toJson))
  }

  def submitOrder() = Authenticated.async(parse.urlFormEncoded) {
    implicit request =>
      val data = request.body
      session.get("uid") match {
        case Some(id) =>
          val orderType = getParam(data, "type", "")
          val price = getParam(data, "price").map(_.toDouble)
          val amount = getParam(data, "amount").map(_.toDouble)
          val total = getParam(data, "total").map(_.toDouble)

          val operation = orderType match {
            case "bid" => Operations.Buy
            case "ask" => Operations.Sell
          }
          val order = UserOrder(id, operation, Btc, Cny, price, amount, total, submitTime = System.currentTimeMillis)

          AccountService.submitOrder(order).map(result => Ok(result.toJson))

        case None => Future(Unauthorized)
      }
  }

  def cancelOrder(id: String) = Authenticated.async {
    implicit request =>
      session.get("uid") match {
        case Some(uid) =>
          AccountService.cancelOrder(id.toLong, uid.toLong).map(result => Ok(result.toJson()))
        case None => Future(Unauthorized)
      }
  }

  def account(uid: String) = Action.async {
    implicit request =>
      AccountService.getAccount(uid.toLong) map {
        case result =>
          Ok(result.toJson)
      }
  }

  def asset(uid: String) = Action.async {
    implicit request =>
      val to = System.currentTimeMillis()
      val from = to - 30 * 60 * 1000

      MarketService.getAsset(uid.toLong, from, to, Currency.Cny).map(rv => Ok(rv.toJson))
  }

  def deposit = Authenticated.async(parse.urlFormEncoded) {
    implicit request =>
      val data = request.body
      val username = session.get("username").getOrElse(null)
      val uid = session.get("uid").getOrElse(null)
      if (username == null || uid == null) {
        Future(Unauthorized)
      } else {
        val amount = getParam(data, "amount", "0.0").toDouble
        val currency: Currency = getParam(data, "currency", "")
        AccountService.deposit(uid.toLong, currency, amount) map {
          case result => Ok(result.toJson)
        }
      }
  }

  def withdrawal = Authenticated.async(parse.urlFormEncoded) {
    implicit request =>
      val data = request.body
      val username = session.get("username").getOrElse(null)
      val uid = session.get("uid").getOrElse(null)
      if (username == null || uid == null) {
        Future(Unauthorized)
      } else {
        val amount = getParam(data, "amount", "0.0").toDouble
        val currency: Currency = getParam(data, "currency", "")
        AccountService.withdrawal(uid.toLong, currency, amount) map {
          case result => Ok(result.toJson)
        }
      }
  }

  def transfers(currency: String, uid: String) = Action.async {
    implicit request =>
      val query = request.queryString
      val status = getParam(query, "status").map(s => TransferStatus.get(s.toInt).getOrElse(TransferStatus.Succeeded))
      val types = getParam(query, "type").map(s => TransferType.get(s.toInt).getOrElse(TransferType.Deposit))
      TransferService.getTransfers(Some(uid.toLong), Some(currency), status, None, types, Cursor(0, 100)) map {
        case result =>
          Ok(result.toJson)
      }
  }

  def history = Action.async {
    implicit request =>
      val query = request.queryString
      val timeDimension = ChartTimeDimension(getParam(query, "period", "1").toInt)
      val defaultTo = System.currentTimeMillis()
      // return 90 items by default
      val defaultFrom = defaultTo - timeDimension * 180
      val fromParam = getParam(query, "from", defaultFrom.toString)
      val toParam = getParam(query, "to", defaultTo.toString)

      val from = fromParam.toLong
      val to = toParam.toLong

      MarketService.getHistory(Btc ~> Cny, timeDimension, from, to).map(result => Ok(result.toJson))
  }

  def transactions = Action.async {
    implicit request =>
      val query = request.queryString
      val limit = getParam(query, "limit", "20").toInt
      val skip = getParam(query, "skip", "0").toInt

      MarketService.getGlobalTransactions(Btc ~> Cny, skip, limit).map(result => Ok(result.toJson))
  }

  def transaction(side: String, tid: String) = Action.async {
    implicit request =>
      MarketService.getTransactions(side, Some(tid.toLong), None, None, 0, 1).map(result => Ok(result.toJson))
  }

  def orderTransaction(side: String, oid: String) = Action.async {
    implicit request =>
      val query = request.queryString
      val limit = getParam(query, "limit", "20").toInt
      val skip = getParam(query, "skip", "0").toInt

      MarketService.getTransactionsByOrder(side, oid.toLong, skip, limit).map(result => Ok(result.toJson))
  }

  def userTransaction(side: String, uid: String) = Action.async {
    implicit request =>
      val query = request.queryString
      val limit = getParam(query, "limit", "20").toInt
      val skip = getParam(query, "skip", "0").toInt

      MarketService.getTransactionsByUser(side, uid.toLong, skip, limit).map(result => Ok(result.toJson))
  }

  def ticker(market: String) = Action.async {
    implicit request =>
      val side: MarketSide = market
      MarketService.getTickers(Set(side)).map(result => Ok(result.toJson))
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