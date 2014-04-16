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
import java.io.File
import models.FileItem
import play.core.Router.Route

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
          AccountService.getOrders(Some(uid.toLong), None, None, 0, 30) map {
            case result: ApiResult =>
              Ok(result.toJson)
          }
        case None => Future(Unauthorized)
      }
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
          val order = UserOrder(id.toLong, operation, Btc, Rmb, price, amount, total, submitTime = System.currentTimeMillis)

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

  def depositHistory(currency: String, uid: String) = Action.async {
    implicit request =>
      DWService.getDeposits(Some(uid.toLong), Some(currency), None, None, Cursor(0, 100)) map {
        case result =>
          Ok(result.toJson)
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

  def withdrawalHistory(currency: String, uid: String) = Action.async {
    implicit request =>
      DWService.getWithdrawal(Some(uid.toLong), Some(currency), None, None, Cursor(0, 100)) map {
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

      MarketService.getHistory(Btc ~> Rmb, timeDimension, from, to).map(result => Ok(result.toJson))
  }

  def transaction = Action.async {
    implicit request =>
      val query = request.queryString
      val limit = getParam(query, "limit", "20").toInt
      val skip = getParam(query, "skip", "0").toInt

      MarketService.getGlobalTransactions(Btc ~> Rmb, skip, limit).map(result => Ok(result.toJson))
  }

  def orderTransaction(oid: String) = Action.async {
    implicit request =>
      val query = request.queryString
      val limit = getParam(query, "limit", "20").toInt
      val skip = getParam(query, "skip", "0").toInt

      MarketService.getTransactionsByOrder(Btc ~> Rmb, oid.toLong, skip, limit).map(result => Ok(result.toJson))
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
        case None => Future(Unauthorized)
      }
  }

  def ticker(market: String) = Action.async {
    implicit request =>
      val side: MarketSide = market
      MarketService.getTickers(Set(side)).map(result => Ok(result.toJson))
  }

  def exportDataFiles(dataType: String) = Action {
    implicit request =>
      // TODO: load path from config
      val dir = new File("/data/export/")
      val files = dir.listFiles
        .filter(_.getName contains "_" + dataType + "_")
        .map(file => FileItem(file.getName, file.length, file.lastModified))

      val result = ApiResult(data = Some(files))

      Ok(result.toJson)
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