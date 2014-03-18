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

object MarketService extends AkkaService{
  def getDepth(marketSide: MarketSide, depth: Int): Future[Any] = {
    Router.backend ? QueryMarket(marketSide, depth)
  }

  def getHistory(marketSide: MarketSide): Future[Any] = {
    Router.backend ? QueryMarketCandleData(marketSide)
  }
}
