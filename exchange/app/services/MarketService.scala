/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package services

import com.coinport.coinex.data._
import akka.pattern.ask
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import models._
import com.coinport.coinex.data.Currency.Rmb

object MarketService extends AkkaService {
  def getDepth(marketSide: MarketSide, depth: Int): Future[ApiResult] = {
    Router.backend ? QueryMarketDepth(marketSide, depth) map {
      case result: QueryMarketDepthResult =>
        val depth: models.MarketDepth = result.marketDepth
        ApiResult(data = Some(depth))
      case x => ApiResult(false)
    }
  }

  def getHistory(marketSide: MarketSide, timeDimension: ChartTimeDimension, from: Long, to: Long): Future[ApiResult] = {
    Router.backend ? QueryCandleData(marketSide, timeDimension, from, to) map {
      case candles: CandleData =>
        val map = candles.items.map(i => i.timestamp -> i).toMap
        val timeSkip: Long = timeDimension
        var open = 0.0
        val data = (from / timeSkip to to / timeSkip).map {
          key: Long =>
            map.get(key) match {
              case Some(item) =>
                open = item.close
                CandleDataItem(key * timeSkip, item.volumn, item.open, item.close, item.low, item.high)
              case None =>
                CandleDataItem(key * timeSkip, 0, open, open, open, open)
            }
        }.toSeq
        ApiResult(data = Some(data))
      case x => ApiResult(false)
    }
  }

  def getTransactions(marketSide: MarketSide, tid: Option[Long], uid: Option[Long], orderId: Option[Long], skip: Int, limit: Int): Future[ApiResult] = {
    val cursor = Cursor(skip, limit)
    Router.backend ? QueryTransaction(marketSide, tid, uid, orderId, None, cursor, false) map {
      case result: QueryTransactionResult =>
        val items = result.transactionItems
        ApiResult(data = Some(items))
    }
  }

  def getGlobalTransactions(marketSide: MarketSide, skip: Int, limit: Int): Future[ApiResult] = getTransactions(marketSide, None, None, None, skip, limit)

  def getTransactionsByUser(marketSide: MarketSide, uid: Long, skip: Int, limit: Int): Future[ApiResult] = getTransactions(marketSide, None, Some(uid), None, skip, limit)

  def getTransactionsByOrder(marketSide: MarketSide, orderId: Long, skip: Int, limit: Int): Future[ApiResult] = getTransactions(marketSide, None, None, Some(orderId), skip, limit)

  def getTickers() = {
    Router.backend ? QueryMetrics map {
      case result: Metrics =>
        val data = result.metricsByMarket
        .filter(_._1._2.equals(Rmb))
        .map {
          case (side, metrics) =>
            val ticker: Ticker = metrics
            ticker
        }.toSeq
        ApiResult(data = Some(data))
    }
  }
}
