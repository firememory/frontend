/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package services

import com.coinport.coinex.data._
import akka.pattern.ask
import scala.concurrent.Future

object MarketService extends AkkaService{
  def getDepth(marketSide: MarketSide, depth: Int): Future[Any] = {
    Router.backend ? QueryMarket(marketSide, depth)
  }
}
