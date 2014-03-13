/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

import com.coinport.coinex.data.{Order, DoSubmitOrder, Currency}
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data.Implicits._
import org.specs2.mutable._
import models._
import models.Implicits._
import models.Operation._
import play.api.libs.json.Json

class ModelTest extends Specification {
  "models.Implicits" should {
    "String to Currency" in {
      val currency1: Currency = "RMB"
      val currency2: Currency = "BTC"
      val currency3: Currency = "Rmb"
      val currency4: Currency = "cny"
      val currency5: Currency = "USD"
      val currency6: Currency = "XXC"

      currency1 must equalTo(Currency.Rmb)
      currency2 must equalTo(Currency.Btc)
      currency1 must equalTo(currency3)
      currency1 must equalTo(currency4)
      currency5 must equalTo(Currency.Usd)
      currency6 must beNull
    }

    "ApiResult to JSON" in {
      val result = ApiResult(true, 0, "some message")
      Json.toJson(result).toString must equalTo("""{"success":true,"code":0,"message":"some message"}""")
    }

    "order converting" in {
      var order: DoSubmitOrder = null
      var command: DoSubmitOrder = null
      val uid = 123L

      // buy orders

      // buy 2 BTC at 4000 RMB/BTC, spending 8000 RMB
      order = UserOrder(uid, Buy, Btc, Rmb, Some(4000), Some(2), Some(8000))
      command = DoSubmitOrder(Rmb ~> Btc, Order(uid, 0L, 8000, Some(1 / 4000D), Some(2)))
      order must equalTo(command)

      // buy 2 BTC at 4000 RMB/BTC, same to above
      order = UserOrder(uid, Buy, Btc, Rmb, Some(4000), Some(2), None)
      command = DoSubmitOrder(Rmb ~> Btc, Order(uid, 0L, 4000 * 2, Some(1 / 4000D), Some(2)))
      order must equalTo(command)

      // market order: buy some BTC at any price, spending 8000 RMB
      order = UserOrder(uid, Buy, Btc, Rmb, None, None, Some(8000))
      command = DoSubmitOrder(Rmb ~> Btc, Order(uid, 0L, 8000, None, None))
      order must equalTo(command)

      // limited market order: buy some BTC at 4000 RMB/BTC, spending 8000 RMB
      order = UserOrder(uid, Buy, Btc, Rmb, Some(4000), None, Some(8000))
      command = DoSubmitOrder(Rmb ~> Btc, Order(uid, 0L, 8000, Some(1 / 4000D), None))
      order must equalTo(command)
      // limited market order: buy 2 BTC, at any price, spending 8000 RMB
      order = UserOrder(uid, Buy, Btc, Rmb, None, Some(2), Some(8000))
      command = DoSubmitOrder(Rmb ~> Btc, Order(uid, 0L, 8000, None, Some(2)))
      order must equalTo(command)

      // sell orders

      // sell 2 BTC at 5000 RMB/BTC, for 10000 RMB
      order = UserOrder(uid, Sell, Btc, Rmb, Some(5000), Some(2), Some(10000))
      command = DoSubmitOrder(Btc ~> Rmb, Order(uid, 0L, 2, Some(5000), Some(10000)))
      order must equalTo(command)

      // sell 2 BTC at 5000 RMB/BTC
      order = UserOrder(uid, Sell, Btc, Rmb, Some(5000), Some(2), None)
      command = DoSubmitOrder(Btc ~> Rmb, Order(uid, 0L, 2, Some(5000), None))
      order must equalTo(command)

      // market order: sell 2 BTC at any price
      order = UserOrder(uid, Sell, Btc, Rmb, None, Some(2), None)
      command = DoSubmitOrder(Btc ~> Rmb, Order(uid, 0L, 2, None, None))
      order must equalTo(command)

      // limit market order: sell 2 BTC at any price, for 10000 RMB
      order = UserOrder(uid, Sell, Btc, Rmb, None, Some(2), Some(10000))
      command = DoSubmitOrder(Btc ~> Rmb, Order(uid, 0L, 2, None, Some(10000)))
      order must equalTo(command)

      // sell some BTC at 5000 RMB/BTC, for 10000 RMB
      order = UserOrder(uid, Sell, Btc, Rmb, Some(5000), None, Some(10000))
      command = DoSubmitOrder(Btc ~> Rmb, Order(uid, 0L, 10000 / 5000, Some(5000), Some(10000)))
      order must equalTo(command)

      // TODO: cover corner cases
    }
  }
}
