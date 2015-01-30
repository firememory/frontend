/**
 * Copyright (C) 2015 Coinport Inc.
 * Author: Xiaolu Wu (xiaolu@coinport.com)
 */

package controllers

import play.api.mvc._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.coinport.coinex.data._
import scala.concurrent.Future
import scala.concurrent.Await
import scala.Some
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data.Implicits._
import com.coinport.coinex.api.model._
import com.coinport.coinex.api.service._
import com.github.tototoshi.play2.json4s.native.Json4s
import controllers.ControllerHelper._
import utils.Constant
import utils.HdfsAccess
import controllers.GoogleAuth.GoogleAuthenticator
import models._

object ApiV2Controller extends Controller with Json4s with AccessLogging {

  import akka.util.Timeout
  import scala.concurrent.duration._

  implicit val timeout = Timeout(2 seconds)
  val logger = Logger(this.getClass)

  def preflight(all: String) = Action {
    Ok("").withHeaders("Access-Control-Allow-Origin" -> "*",
      "Allow" -> "*",
      "Access-Control-Allow-Methods" -> "POST, GET, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "Origin, X-Requested-With, Content-Type, Accept, Referrer, User-Agent");
  }

  def tickers(currency: String) = if (currency.equalsIgnoreCase(Currency.Cny.toString)) tickerBasic(Constant.cnyMarketSides)
    else if (currency.equalsIgnoreCase(Currency.Btc.toString)) tickerBasic(Constant.btcMarketSides)
    else tickerBasic(Constant.allMarketSides)

  def allTickers() = tickerBasic(Constant.allMarketSides)

  def ticker(market: String) = {
   val out = market.split("-")(0)
   val in = market.split("-")(1)
   tickerBasic(Seq(string2RichMarketSide(market)))
  }

  private def tickerBasic(sides: Seq[MarketSide]) = Action.async {
    implicit request =>
      val r = MarketService.getJsonTickers(sides)
      r.map(result => Ok(result.toJson))
  }

  def reserves() = Action.async {
    implicit request =>
      val reserves = Constant.supportReserveCoins map { case c =>
        val reserveFuture = OpenService.getCurrencyReserve(c)
        val reserveApiResult = Await.result(reserveFuture, 5 seconds).asInstanceOf[ApiResult]
        if (reserveApiResult.success) {
          val reserve = reserveApiResult.data.get.asInstanceOf[ApiCurrencyReserve]
          c.toString.toUpperCase -> Seq(reserve.hot.value, reserve.cold.value, reserve.user.value, reserve.total.value)
        } else {
          c.toString.toUpperCase -> Seq.empty
        }
      }
      val result = ApiResult(true, 0, "", Some(reserves.toMap))
      Future(Ok(result.toJson))
  }

  def reserve(currency: String) = Action.async {
    implicit request =>
      BitwayService.getReserve(currency).map(result =>
        Ok(result.toJson))
  }

  def balanceSnapshotFiles(currency: String) = Action {
    implicit request =>
      val pager = ControllerHelper.parseApiV2PagingParam()
      val path = "csv/asset/" + currency.toLowerCase
      val files = HdfsAccess.listFiles(path)
        .sortWith((a, b) => a.updated > b.updated)

      val from = Math.min(pager.skip, files.length - 1)
      val until = pager.skip + pager.limit

      val items = files.slice(from, until)

      val jsonFormated = items map { f =>
        Seq(f.name, f.size, f.updated)
      }

      val downloadPreUrl = "https://exchange.coinport.com/download/" + path + "/"

      val data = ApiV2PagingWrapper(
        hasMore = from < files.length - 1,
        currency = currency,
        path = downloadPreUrl,
        items = jsonFormated
      )
      Ok(ApiResult(data = Some(data)).toJson)
  }

  //TODO(xiaolu) unfinished api
  def transfers(currency: String) = Action {
    Ok("unfinished")
  }

  def trades(market: String) = Action.async {
    implicit request =>
      val pager = ControllerHelper.parseApiV2PagingParam()
      MarketService.getGlobalTransactions(Some(market), pager.skip, pager.limit).map(
        result => {
          if (result.success) {
            val pageWrapper = result.data.get.asInstanceOf[ApiPagingWrapper]
            val txs = pageWrapper.items.asInstanceOf[Seq[ApiTransaction]]
            val apiV2Txs = txs.map { t =>
              ApiV2Transaction(t.id, t.timestamp, t.price.value, t.subjectAmount.value, t.maker, t.taker, t.sell, t.tOrder.oid, t.mOrder.oid, Some(market))
            }
            val hasMore = pager.limit == txs.size
            val timestamp = System.currentTimeMillis
            val updated = result.copy(data = Some(ApiV2TradesPagingWrapper(timestamp, hasMore, market, apiV2Txs)))
            Ok(updated.toJson)
          } else {
            Ok(result.toJson)
          }
        })
  }

  def v2Depth(market: String) = Action.async {
    implicit request =>
      val query = request.queryString
      val limit = getParam(query, "limit", "100").toInt min 200

      MarketService.getDepth(market, limit).map{ result =>
        val depth = result.data.get.asInstanceOf[ApiMarketDepth]
        Ok(result.copy(data = Some(toV2MarketDepth(depth))).toJson)
      }
  }

  private def toV2MarketDepth(d : ApiMarketDepth) = {
    val asks = d.asks.map(i => Seq(i.price.value, i.amount.value))
    val bids = d.bids.map(i => Seq(i.price.value, i.amount.value))
    Map("asks" -> asks, "bids" -> bids)
  }

  def kline(market: String) = Action.async {
    implicit request =>
      val query = request.queryString
      val timeDimension = intervalToCharTimeDimension(getParam(query, "interval", "5m").toString)
      val defaultTo = System.currentTimeMillis()
      // return 90 items by default
      val defaultFrom = defaultTo - timeDimension * 90
      val fromParam = getParam(query, "start", defaultFrom.toString)
      val toParam = getParam(query, "end", defaultTo.toString)

      val to = toParam.toLong
      val from = fromParam.toLong max (to - timeDimension * 180)

      MarketService.getHistory(market, timeDimension, from, to).map{ result =>
        val apiHistory = result.data.get.asInstanceOf[ApiHistory]
        val updated = result.copy(data = Some(ApiV2History(items = apiHistory.candles)))
        Ok(updated.toJson)
      }
  }

  private def intervalToCharTimeDimension(interval : String): ChartTimeDimension = {
    interval match {
      case "1m" => ChartTimeDimension.OneMinute
      case "5m" => ChartTimeDimension.FiveMinutes
      case "15m" => ChartTimeDimension.FifteenMinutes
      case "30m" => ChartTimeDimension.ThirtyMinutes
      case "1h" => ChartTimeDimension.OneHour
      case "2h" => ChartTimeDimension.TwoHours
      case "4h" => ChartTimeDimension.FourHours
      case "6h" => ChartTimeDimension.SixHours
      case "1d" => ChartTimeDimension.OneDay
    }
  }

  def profile() = Authenticated.async {
    implicit request =>
      val apiTokenPair = request.headers.get("auth")
      val tokenArr = apiTokenPair.getOrElse("").split(":")
      val token = tokenArr(0)
      val secret = tokenArr(1)
      val apiSecretFuture = UserService.getApiSecret(token)
      val apiSecretResult = Await.result(apiSecretFuture, 5 seconds).asInstanceOf[ApiResult]
      if (apiSecretResult.success) {
        val userId = apiSecretResult.data.get.asInstanceOf[ApiSecret].userId
        UserService.getProfileApiV2(userId.get) map { p =>
          if (p.success) {
            val pf = p.data.get.asInstanceOf[ApiV2Profile]
            Ok(p.copy(data = Some(pf.copy(apiToken = Some(token), apiSecret = Some(secret)))).toJson)
          } else {
            Ok(p.toJson)
          }
        }
      } else {
        Future(Ok(apiSecretResult.toJson))
      }
  }

  def balance() = Authenticated.async {
    implicit request =>
      val apiSecretResult = getUserIdFromTokenPair(request.headers.get("auth").getOrElse(""))
      if (apiSecretResult.success) {
        val userId = apiSecretResult.data.get.asInstanceOf[ApiSecret].userId
        AccountService.getAccount(userId.get) map {
          case result =>
            val apiUserAccount = result.data.get.asInstanceOf[ApiUserAccount]
            val balance = apiUserAccount.accounts.map(
              a => (a._1 ->
                Seq(a._2.available.value, a._2.locked.value, a._2.pendingWithdrawal.value, a._2.total.value)))
            // logger.info(s"account result: $result")
            Ok(result.copy(data = Some(balance.toMap)).toJson)
        }
      } else {
        Future(Ok(apiSecretResult.toJson))
      }
  }

  private def getUserIdFromTokenPair(pair: String) = {
    val tokenArr = pair.split(":")
    val token = tokenArr(0)
    val secret = tokenArr(1)
    val apiSecretFuture = UserService.getApiSecret(token)
    Await.result(apiSecretFuture, 5 seconds).asInstanceOf[ApiResult]
  }

  def userTrades() = Authenticated.async {
    implicit request =>
      val pager = ControllerHelper.parseApiV2PagingParam()
      val query = request.queryString
      val market = getParam(query, "market")
      val marketSide = if (market.isDefined) Some(string2RichMarketSide(market.get)) else None
      val apiSecretResult = getUserIdFromTokenPair(request.headers.get("auth").getOrElse(""))
      if (apiSecretResult.success) {
        val userId = apiSecretResult.data.get.asInstanceOf[ApiSecret].userId
        MarketService.getTransactionsByUser(marketSide, userId.get, pager.skip, pager.limit).map(
          result => {
            if (result.success) {
              val pageWrapper = result.data.get.asInstanceOf[ApiPagingWrapper]
              val txs = pageWrapper.items.asInstanceOf[Seq[ApiTransaction]]
              val apiV2Txs = txs.map { t =>
                val marketInResult = if (marketSide.isDefined) None else Some(t.subjectAmount.currency.toUpperCase + "-" + t.currencyAmount.currency.toUpperCase)
                ApiV2Transaction(t.id, t.timestamp, t.price.value, t.subjectAmount.value, t.maker, t.taker, t.sell, t.tOrder.oid, t.mOrder.oid, marketInResult)
              }
              val hasMore = pager.limit == txs.size
              val timestamp = System.currentTimeMillis
              val updated = result.copy(data = Some(ApiV2TradesPagingWrapper(timestamp, hasMore, market.getOrElse(""), apiV2Txs)))
              Ok(updated.toJson)
            } else {
              Ok(result.toJson)
            }
          }
        )
      } else {
        Future(Ok(apiSecretResult.toJson))
      }
  }

  def userOrders() = Authenticated.async {
    implicit request =>
      val pager = ControllerHelper.parseApiV2PagingParam()
      val query = request.queryString
      val market = getParam(query, "market")
      val marketSide = if (market.isDefined) Some(string2RichMarketSide(market.get)) else None
      val apiSecretResult = getUserIdFromTokenPair(request.headers.get("auth").getOrElse(""))
      val status = getParam(request.queryString, "order_status") match {
        case None => Nil
        case Some(s) =>
          if (s == "1") Seq(OrderStatus.PartiallyExecuted, OrderStatus.Pending)
          else if (s == "2") Seq(OrderStatus.FullyExecuted, OrderStatus.PartiallyExecutedThenCancelledByMarket)
          else if (s == "3") Seq(OrderStatus.FullyExecuted, OrderStatus.PartiallyExecutedThenCancelledByMarket,
            OrderStatus.Cancelled, OrderStatus.CancelledByMarket)
          else Nil
      }
      //TODO(xiaolu) not support ids query now.
      // val ids = getParam(query, "ids", "").split(",")
      if (apiSecretResult.success) {
        val userId = apiSecretResult.data.get.asInstanceOf[ApiSecret].userId
        AccountService.getOrders(marketSide, Some(userId.get.toLong), None, status, pager.skip, pager.limit) map {
          case result: ApiResult =>
            if (result.success) {
              val apiOrders = result.data.get.asInstanceOf[ApiPagingWrapper].items.asInstanceOf[Seq[ApiOrder]]
              val apiV2Orders = apiOrders.map(o => ApiV2Order(
              o.id, o.operation.toLowerCase, o.status,
              o.subject.toUpperCase + "-" + o.currency.toUpperCase,
              o.price.get.value,
              o.amount.get.value,
              o.finishedQuantity.value,
              o.submitTime,
              None))
              val hasMore = pager.limit == apiV2Orders.size
              Ok(result.copy(data = Some(ApiV2OrderPagingWrapper(hasMore, apiV2Orders))).toJson)
            } else {
              Ok(result.toJson)
            }
        }
      } else {
        Future(Ok(apiSecretResult.toJson))
      }
  }

  def depositHistory() = userTransfers(TransferType.Deposit)

  def withdrawalHistory() = userTransfers(TransferType.Withdrawal)

  private def userTransfers(ttype: TransferType) = Authenticated.async {
    implicit request =>
      val query = request.queryString
      val currency = getParam(query, "currency", "ALL")
      val apiSecretResult = getUserIdFromTokenPair(request.headers.get("auth").getOrElse(""))
      if (apiSecretResult.success) {
        val userId = apiSecretResult.data.get.asInstanceOf[ApiSecret].userId
        val types = Seq(ttype)
        val pager = ControllerHelper.parseApiV2PagingParam()

        val typeList = if (types.toSet.contains(TransferType.Deposit)) types :+ TransferType.DepositHot else types

        TransferService.getTransfers(userId, Currency.valueOf(currency), None, None, typeList, Cursor(pager.skip, pager.limit)) map {
          case result => 
            if (result.success) {
              val transfers = result.data.get.asInstanceOf[ApiPagingWrapper].items.asInstanceOf[Seq[ApiTransferItem]]
              val v2Transfers = transfers.map(t => ApiV2TransferItem(t.id, t.amount.currency.toUpperCase, t.amount.value, t.status, t.created, t.updated, t.address))
              val hasMore = pager.limit == v2Transfers.size
              if (ttype == TransferType.Deposit)
                Ok(result.copy(data = Some(ApiV2DepositsPagingWrapper(hasMore, v2Transfers))).toJson)
              else
                Ok(result.copy(data = Some(ApiV2WithdrawalsPagingWrapper(hasMore, v2Transfers))).toJson)
            } else {
              Ok(result.toJson)
            }
        }
      } else {
        Future(Ok(apiSecretResult.toJson))
      }
  }

}
