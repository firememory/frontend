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
import controllers.ControllerHelper._

object ApiController extends Controller with Json4s {

  import akka.util.Timeout
  import scala.concurrent.duration._

  implicit val timeout = Timeout(2 seconds)

  def depth(market: String) = Action.async {
    implicit request =>
      val query = request.queryString
      val depth = getParam(query, "depth", "5").toInt

      MarketService.getDepth(market, depth).map(result => Ok(result.toJson))
  }

  def getUserOrders(market: String) = Action.async {
    implicit request =>
      val pager = ControllerHelper.parsePagingParam()
      val status = ControllerHelper.getParam(request.queryString, "status").map(s => OrderStatus.get(s.toInt).getOrElse(OrderStatus.Pending))
      request.session.get("uid") match {
        case Some(uid) =>
          val marketSide: Option[MarketSide] = if (market.isEmpty || market == "all") None else Some(market)
          AccountService.getOrders(marketSide, Some(uid.toLong), None, status, pager.skip, pager.limit) map {
            case result: ApiResult =>
              Ok(result.toJson)
          }
        case None => Future(Unauthorized)
      }
  }

  def getOrder(oid: String) = Action.async {
    implicit request =>
      AccountService.getOrders(None, None, Some(oid.toLong), None, 0, 1).map(result => Ok(result.toJson))
  }

  def recentOrders(market: String) = Action.async {
    implicit request =>
      AccountService.getOrders(Some(market), None, None, None, 0, 15).map(result => Ok(result.toJson))
  }

  def orders(market: String) = Action.async {
    implicit request =>
      val pager = ControllerHelper.parsePagingParam()
      AccountService.getOrders(Some(market), None, None, None, pager.skip, pager.limit).map(result => Ok(result.toJson))
  }


  def submitOrder(market: String) = Authenticated.async(parse.urlFormEncoded) {
    implicit request =>
      val subject = market.substring(0, 3);
      val currency = market.substring(3);
      val data = request.body
      request.session.get("uid") match {
        case Some(id) =>
          val orderType = getParam(data, "type", "")
          val price = getParam(data, "price").map(_.toDouble)
          val amount = getParam(data, "amount").map(_.toDouble)
          val total = getParam(data, "total").map(_.toDouble)

          val operation = orderType match {
            case "bid" => Operations.Buy
            case "ask" => Operations.Sell
          }
          val order = UserOrder(id, operation, subject, currency, price, amount, total, submitTime = System.currentTimeMillis)
          AccountService.submitOrder(order).map(result => Ok(result.toJson))

        case None => Future(Unauthorized)
      }
  }

  def cancelOrder(market: String, id: String) = Authenticated.async {
    implicit request =>
      request.session.get("uid") match {
        case Some(uid) =>
          AccountService.cancelOrder(id.toLong, uid.toLong, market).map(result => Ok(result.toJson()))
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
      val from = to - (30L * 24L * 60L * 60L * 1000L).toLong

      MarketService.getAsset(uid.toLong, from, to, Currency.Btc).map(rv => Ok(rv.toJson))
  }

  def deposit = Authenticated.async(parse.urlFormEncoded) {
    implicit request =>
      val data = request.body
      val username = request.session.get("username").getOrElse(null)
      val uid = request.session.get("uid").getOrElse(null)
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

//  def withdrawal = Authenticated.async(parse.urlFormEncoded) {
//    implicit request =>
//      val data = request.body
//      val username = request.session.get("username").getOrElse(null)
//      val uid = request.session.get("uid").getOrElse(null)
//      if (username == null || uid == null) {
//        Future(Unauthorized)
//      } else {
//        val amount = getParam(data, "amount", "0.0").toDouble
//        val currency: Currency = getParam(data, "currency", "")
//        val address = getParam(data, "address", "")
//        val uuid = getParam(data, "verifyCodeUuid").getOrElse("")
//        val verifyCode = getParam(data, "phoneVerCode").getOrElse("")
//        validateParamsAndThen(
//          new StringNonemptyValidator(uuid, verifyCode),
//          new CachedValueValidator(ErrorCode.SmsCodeNotMatch, uuid, verifyCode)
//        ) {
//          AccountService.withdrawal(uid.toLong, currency, amount, address)} map {
//            case result => Ok(result.toJson)
//          }
//        }
//
//  }

def withdrawal = Authenticated.async(parse.urlFormEncoded) {
  implicit request =>
    val data = request.body
    val username = request.session.get("username").getOrElse(null)
    val uid = request.session.get("uid").getOrElse(null)
    if (username == null || uid == null) {
      Future(Unauthorized)
    } else {
      val amount = getParam(data, "amount", "0.0").toDouble
      val currency: Currency = getParam(data, "currency", "")
      val address = getParam(data, "address", "")
      UserService.setWithdrawalAddress(uid.toLong, currency, address)
      AccountService.withdrawal(uid.toLong, currency, amount, address).map {
        case result => Ok(result.toJson)
      }
    }
}
  def cancelWithdrawal(uid: String, tid: String) = Authenticated.async(parse.urlFormEncoded) {
    implicit request =>
      println("uid"+uid)
      println("tid"+tid)
      TransferService.cancelWithdrawal(uid.toLong, tid.toLong)
      Future(Ok(ApiResult.toJson))
  }

  def transfers(currency: String, uid: String) = Action.async {
    implicit request =>
      val query = request.queryString
      val status = getParam(query, "status").map(s => TransferStatus.get(s.toInt).getOrElse(TransferStatus.Succeeded))
      val types = getParam(query, "type").map(s => TransferType.get(s.toInt).getOrElse(TransferType.Deposit)) match {
        case Some(t) => Seq(t)
        case None => Seq(TransferType.Deposit, TransferType.Withdrawal)
      }
      val pager = ControllerHelper.parsePagingParam()

      val userId = if( uid.toLong > 0) Some(uid.toLong) else None
      TransferService.getTransfers(userId, Currency.valueOf(currency), status, None, types, Cursor(pager.skip, pager.limit)) map {
        case result => Ok(result.toJson)
      }
  }

  def history(market: String) = Action.async {
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

      MarketService.getHistory(market, timeDimension, from, to).map(result => Ok(result.toJson))
  }

  def transactions(market: String) = Action.async {
    implicit request =>
      val pager = ControllerHelper.parsePagingParam()
      MarketService.getGlobalTransactions(Some(market), pager.skip, pager.limit).map (
        result => Ok(result.toJson))
  }

  def transaction(tid: String) = Action.async {
    implicit request =>
      MarketService.getTransaction(tid.toLong).map(result => Ok(result.toJson))
  }

  def orderTransaction(oid: String) = Action.async {
    implicit request =>
      val pager = ControllerHelper.parsePagingParam()
      MarketService.getTransactionsByOrder(None, oid.toLong, pager.skip, pager.limit).map(result => Ok(result.toJson))
  }

  def userTransactionByMarket(side: String, uid: String) = Action.async {
    implicit request =>
      val pager = ControllerHelper.parsePagingParam()
      MarketService.getTransactionsByUser(Some(side), uid.toLong, pager.skip, pager.limit).map(result => Ok(result.toJson))
  }

  def userTransaction(uid: String) = Action.async {
    implicit request =>
      val pager = ControllerHelper.parsePagingParam()
      MarketService.getTransactionsByUser(None, uid.toLong, pager.skip, pager.limit).map(result => Ok(result.toJson))
  }

  def ticker(market: String) = Action.async {
    implicit request =>
      val side: MarketSide = market
      MarketService.getTickers(Seq(side)).map(result => Ok(result.toJson))
  }

  def tickers() = Action.async {
    implicit request =>
      val sides = Seq(Btc ~> Cny, Ltc ~> Btc, Pts ~> Btc, Dog ~> Btc)
      MarketService.getTickers(sides).map(result => Ok(result.toJson))
  }

  def ccNetworkStatus(currency: String) = Action.async {
    implicit request =>
      BitwayService.getNetworkStatus(currency).map(result => Ok(result.toJson))
  }

  def wallets(currency: String, walletsType: String) = Action.async {
    implicit request =>
      BitwayService.getWallets(currency, CryptoCurrencyAddressType.valueOf(walletsType).get).map(result =>
      Ok(result.toJson))
  }

  def currencyReserve(currency: String) = Action.async {
    implicit request =>
      OpenService.getCurrencyReserve(currency).map(result =>
      Ok(result.toJson))
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
