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
import models.ApiV2PagingWrapper
import models.ApiV2TradesPagingWrapper

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

  def transactions(market: String) = Action.async {
    implicit request =>
      val pager = ControllerHelper.parseApiV2PagingParam()
      MarketService.getGlobalTransactions(Some(market), pager.skip, pager.limit).map(
        result => {
          if (result.success) {
            val pageWrapper = result.data.get.asInstanceOf[ApiPagingWrapper]
            val txs = pageWrapper.items.asInstanceOf[Seq[ApiTransaction]]
            val apiV2Txs = txs.map { t =>
              ApiV2Transaction(t.id, t.timestamp, t.price.value, t.subjectAmount.value, t.maker, t.taker, t.sell, t.tOrder.oid, t.mOrder.oid)
            }
            val hasMore = pager.limit > txs.size
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
}
