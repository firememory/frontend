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

object MarketService extends AkkaService {
  def getDepth(marketSide: MarketSide, depth: Int): Future[ApiResult] = {
    Router.backend ? QueryMarketDepth(marketSide, depth) map {
      case result: QueryMarketDepthResult =>
        val depth: models.MarketDepth = result.marketDepth
        ApiResult(true, 0, "", Some(depth))
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
        ApiResult(true, 0, "", Some(data))
      case x => ApiResult(false)
    }
  }

  def getAllTransactions(marketSide: MarketSide, from: Long, num: Int): Future[Any] = {
    Router.backend ? QueryTransactionData(marketSide, from, num)
  }

  def getUserTransactions(marketSide: MarketSide, userId: Long, orderId: Long, from: Long, num: Int): Future[Any] = {
    Router.backend ? QueryUserTransaction(marketSide, userId, Some(orderId), from, num)
  }
}
