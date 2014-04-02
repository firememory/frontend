/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package services

import com.coinport.coinex.data._
import akka.pattern.ask
import scala.concurrent.Future
import com.coinport.coinex.data.Currency.{Rmb, Btc}
import com.coinport.coinex.data.Implicits._

object MarketService extends AkkaService {
  def getDepth(marketSide: MarketSide, depth: Int): Future[Any] = {
    Router.backend ? QueryMarketDepth(marketSide, depth)
  }

  def getHistory(marketSide: MarketSide, timeDimension: ChartTimeDimension, from: Long, to: Long): Future[Any] = {
    Router.backend ? QueryCandleData(marketSide, timeDimension, from, to)
  }

  def getAllTransactions(marketSide: MarketSide, from: Long, num: Int): Future[Any] = {
    Router.backend ? QueryTransactionData(marketSide, from, num)
  }

  def getUserTransactions(marketSide: MarketSide, userId: Long, orderId: Long, from: Long, num: Int): Future[Any] = {
    Router.backend ? QueryUserTransaction(marketSide, userId, Some(orderId), from, num)
  }
}
