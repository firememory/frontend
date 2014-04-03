/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

import com.coinport.coinex.data.Implicits._
import com.coinport.coinex.data.Currency._
import org.specs2.mutable._
import models._

class MarketConversionTest extends Specification {
  "market conversions" should {
    "market depth conversion" in {
      val bids = List(
        com.coinport.coinex.data.MarketDepthItem(450, 1500),
        com.coinport.coinex.data.MarketDepthItem(300, 2000),
        com.coinport.coinex.data.MarketDepthItem(200, 3000)
      )
      val asks = List(
        com.coinport.coinex.data.MarketDepthItem(550, 4500),
        com.coinport.coinex.data.MarketDepthItem(600, 5000),
        com.coinport.coinex.data.MarketDepthItem(700, 6000)
      )
      val backendObj = com.coinport.coinex.data.MarketDepth(Btc ~> Rmb,  asks = asks, bids = bids)
      val marketDepth: models.MarketDepth = backendObj
      val expect = models.MarketDepth(
        bids = List(MarketDepthItem(4500.0, 1.5), MarketDepthItem(3000.0, 2.0), MarketDepthItem(2000.0, 3.0)),
        asks = List(MarketDepthItem(5500.0, 4.5), MarketDepthItem(6000.0, 5.0), MarketDepthItem(7000.0, 6.0))
      )

      marketDepth must equalTo(expect)
    }
  }
}