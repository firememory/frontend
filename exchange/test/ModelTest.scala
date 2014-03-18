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
      order = UserOrder(uid, Buy, Btc, Rmb, Some(4100.0), Some(2), Some(8200)).toDoSubmitOrder
      // sell 800000 CNY2 at price of 1000 / 400000 CNY2 per MBTC for 2000 MBTC
      command = DoSubmitOrder(Rmb ~> Btc, Order(uid, 0L, 820000, Some(1000.0 / 410000.0), Some(2000)))
      order must equalTo(command)

      // buy 2 BTC at 4000 RMB/BTC, same to above
      order = UserOrder(uid, Buy, Btc, Rmb, Some(4000), Some(2), None).toDoSubmitOrder
      // sell 800000 CNY2 at price of 1000 / 400000 CNY2 per MBTC for 2000 MBTC
      command = DoSubmitOrder(Rmb ~> Btc, Order(uid, 0L, 800000, Some(1000.0 / 400000), Some(2000)))
      order must equalTo(command)

      // market order: buy some BTC at any price, spending 8000 RMB
      order = UserOrder(uid, Buy, Btc, Rmb, None, None, Some(8000)).toDoSubmitOrder
      // sell 800000 CNY2 at any price for some MBTC
      command = DoSubmitOrder(Rmb ~> Btc, Order(uid, 0L, 800000, None, None))
      order must equalTo(command)

      // limited market order: buy some BTC at 4000 RMB/BTC, spending 8000 RMB
      order = UserOrder(uid, Buy, Btc, Rmb, Some(4000), None, Some(8000)).toDoSubmitOrder
      // sell 800000 CNY2 at 1000 / 400000 CNY2 per MBTC for some MBTC
      command = DoSubmitOrder(Rmb ~> Btc, Order(uid, 0L, 800000, Some(1000.0 / 400000), None))
      order must equalTo(command)
      // limited market order: buy 2 BTC, at any price, spending 8000 RMB
      order = UserOrder(uid, Buy, Btc, Rmb, None, Some(2), Some(8000)).toDoSubmitOrder
      // sell 800000 CNY2 at any price for 2000 MBTC
      command = DoSubmitOrder(Rmb ~> Btc, Order(uid, 0L, 800000, None, Some(2000)))
      order must equalTo(command)

      // sell orders

      // sell 2 BTC at 5000 RMB/BTC, for 10000 RMB
      order = UserOrder(uid, Sell, Btc, Rmb, Some(5000), Some(2), Some(10000)).toDoSubmitOrder
      // sell 2000 MBTC at 5000 * 100 / 1000 CNY2/MBTC, for 10000 * 100 CNY2
      command = DoSubmitOrder(Btc ~> Rmb, Order(uid, 0L, 2000, Some(5000 * 100 / 1000), Some(10000 * 100)))
      order must equalTo(command)

      // sell 2 BTC at 5000 RMB/BTC
      order = UserOrder(uid, Sell, Btc, Rmb, Some(5000), Some(2), None).toDoSubmitOrder
      // sell 2000 MBTC at 500 CNY2/MBTC
      command = DoSubmitOrder(Btc ~> Rmb, Order(uid, 0L, 2000, Some(5000 * 100 / 1000), None))
      order must equalTo(command)

      // market order: sell 2 BTC at any price
      order = UserOrder(uid, Sell, Btc, Rmb, None, Some(2), None).toDoSubmitOrder
      // sell 2000 MBTC at any price
      command = DoSubmitOrder(Btc ~> Rmb, Order(uid, 0L, 2000, None, None))
      order must equalTo(command)

      // limit market order: sell 2 BTC at any price, for 10000 RMB
      order = UserOrder(uid, Sell, Btc, Rmb, None, Some(2), Some(10000)).toDoSubmitOrder
      // sell 2000 BTC at any price, for 1000000 CNY2
      command = DoSubmitOrder(Btc ~> Rmb, Order(uid, 0L, 2000, None, Some(1000000)))
      order must equalTo(command)

      // sell some BTC at 5000 RMB/BTC, for 10000 RMB
      order = UserOrder(uid, Sell, Btc, Rmb, Some(5000), None, Some(10000)).toDoSubmitOrder
      // sell some BTC at 500 CNY2/MBTC, for 1000000 CNY2
      command = DoSubmitOrder(Btc ~> Rmb, Order(uid, 0L, 1000000 / 500, Some(500), Some(1000000)))
      order must equalTo(command)

      // convert back
      var userOrder = UserOrder(uid, Sell, Btc, Rmb, Some(1234), Some(12), None)
      userOrder must equalTo(UserOrder.fromOrderInfo(OrderInfo(Btc ~> Rmb, userOrder.toDoSubmitOrder.order, 0, 0, userOrder.status)))

      userOrder = UserOrder(uid, Buy, Btc, Rmb, Some(1234), Some(12), Some(1234 * 12))
      userOrder must equalTo(UserOrder.fromOrderInfo(OrderInfo(Rmb ~> Btc, userOrder.toDoSubmitOrder.order, 0, 0, userOrder.status)))

      // TODO: cover corner cases
    }
  }

//  "price by" in {
//    val uid = 123L
//    var order: UserOrder = null
//
//    order = UserOrder(uid, Sell, Btc, Rmb, Some(5000D), Some(2L), None)
//    order.priceBy(Rmb) must equalTo(order)
//
//    order = UserOrder(uid, Sell, Rmb, Btc, Some(1D / 5000D), Some(10000L), None)
//    order.priceBy(Rmb) must equalTo(UserOrder(uid, Buy, Btc, Rmb, Some(5000D), Some(2L), Some(10000L)))
//
//  }

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

    // price conversions
    PriceValue(4000, (CNY, BTC)) to (CNY, MBTC) must equalTo(PriceValue(4, (CNY, MBTC)))
    PriceValue(4000, (CNY, BTC)) to (CNY2, MBTC) must equalTo(PriceValue(400, (CNY2, MBTC)))
    PriceValue(4000, (CNY, BTC)) to (BTC, CNY) must equalTo(PriceValue(1.0 / 4000, (BTC, CNY)))

    // multiply
    val amount = 2 unit BTC
    var price: PriceValue = 4000.0 unit (CNY, BTC)

    amount * price must equalTo(8000.0 unit CNY)
    price * amount must equalTo(8000.0 unit CNY)

    price = 4.0 unit (CNY, MBTC)
    amount * price must equalTo(8000.0 unit CNY)
    price * amount must equalTo(8000.0 unit CNY)
  }
}
