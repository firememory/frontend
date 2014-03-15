/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

import com.coinport.coinex.data.{OrderInfo, Order, DoSubmitOrder, Currency}
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
      order = UserOrder(uid, Buy, Btc, Rmb, Some(4000), Some(2), Some(8000)).toDoSubmitOrder
      command = DoSubmitOrder(Rmb ~> Btc, Order(uid, 0L, 8000, Some(1 / 4000D), Some(2)))
      order must equalTo(command)

      // buy 2 BTC at 4000 RMB/BTC, same to above
      order = UserOrder(uid, Buy, Btc, Rmb, Some(4000), Some(2), None).toDoSubmitOrder
      command = DoSubmitOrder(Rmb ~> Btc, Order(uid, 0L, 4000 * 2, Some(1 / 4000D), Some(2)))
      order must equalTo(command)

      // market order: buy some BTC at any price, spending 8000 RMB
      order = UserOrder(uid, Buy, Btc, Rmb, None, None, Some(8000)).toDoSubmitOrder
      command = DoSubmitOrder(Rmb ~> Btc, Order(uid, 0L, 8000, None, None))
      order must equalTo(command)

      // limited market order: buy some BTC at 4000 RMB/BTC, spending 8000 RMB
      order = UserOrder(uid, Buy, Btc, Rmb, Some(4000), None, Some(8000)).toDoSubmitOrder
      command = DoSubmitOrder(Rmb ~> Btc, Order(uid, 0L, 8000, Some(1 / 4000D), None))
      order must equalTo(command)
      // limited market order: buy 2 BTC, at any price, spending 8000 RMB
      order = UserOrder(uid, Buy, Btc, Rmb, None, Some(2), Some(8000)).toDoSubmitOrder
      command = DoSubmitOrder(Rmb ~> Btc, Order(uid, 0L, 8000, None, Some(2)))
      order must equalTo(command)

      // sell orders

      // sell 2 BTC at 5000 RMB/BTC, for 10000 RMB
      order = UserOrder(uid, Sell, Btc, Rmb, Some(5000), Some(2), Some(10000)).toDoSubmitOrder
      command = DoSubmitOrder(Btc ~> Rmb, Order(uid, 0L, 2, Some(5000), Some(10000)))
      order must equalTo(command)

      // sell 2 BTC at 5000 RMB/BTC
      order = UserOrder(uid, Sell, Btc, Rmb, Some(5000), Some(2), None).toDoSubmitOrder
      command = DoSubmitOrder(Btc ~> Rmb, Order(uid, 0L, 2, Some(5000), None))
      order must equalTo(command)

      // market order: sell 2 BTC at any price
      order = UserOrder(uid, Sell, Btc, Rmb, None, Some(2), None).toDoSubmitOrder
      command = DoSubmitOrder(Btc ~> Rmb, Order(uid, 0L, 2, None, None))
      order must equalTo(command)

      // limit market order: sell 2 BTC at any price, for 10000 RMB
      order = UserOrder(uid, Sell, Btc, Rmb, None, Some(2), Some(10000)).toDoSubmitOrder
      command = DoSubmitOrder(Btc ~> Rmb, Order(uid, 0L, 2, None, Some(10000)))
      order must equalTo(command)

      // sell some BTC at 5000 RMB/BTC, for 10000 RMB
      order = UserOrder(uid, Sell, Btc, Rmb, Some(5000), None, Some(10000)).toDoSubmitOrder
      command = DoSubmitOrder(Btc ~> Rmb, Order(uid, 0L, 10000 / 5000, Some(5000), Some(10000)))
      order must equalTo(command)

      // convert back
      var userOrder = UserOrder(uid, Sell, Btc, Usd, Some(1234), Some(12), None)
      userOrder must equalTo(UserOrder.fromOrderInfo(OrderInfo(Btc ~> Usd, userOrder.toDoSubmitOrder.order, userOrder.status, 0, 0)))

      // TODO: cover corner cases
    }
  }

  "price by" in {
    val uid = 123L
    var order: UserOrder = null

    order = UserOrder(uid, Sell, Btc, Rmb, Some(5000D), Some(2L), None)
    order.priceBy(Rmb) must equalTo(order)

    order = UserOrder(uid, Sell, Rmb, Btc, Some(1D / 5000D), Some(10000L), None)
    order.priceBy(Rmb) must equalTo(UserOrder(uid, Buy, Btc, Rmb, Some(5000D), Some(2L), Some(10000L)))

  }

  import models.CurrencyUnit._
  import models.CurrencyValue._

  "currency unit conversions" in {
    val a = 1 unit BTC
    val b = 1000 unit MBTC

    a must equalTo(b)

    a / b must equalTo(PriceValue(1.0))

    12.345 unit BTC must equalTo(12345 unit MBTC)
    23.45 unit CNY must equalTo (2345 unit CNY2)
    0 unit BTC must equalTo(0 unit MBTC)

    1 unit BTC to MBTC must equalTo(1000 unit MBTC)
    val sub = 1 unit BTC
    val money = 4000 unit CNY
    money / sub must equalTo(PriceValue(4000, (CNY, BTC)))
  }
}
