/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

import com.coinport.coinex.data._
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data.Implicits._
import org.specs2.mutable._
import models._
import models.OperationEnum._
import scala.Some

class OrderConversionTest extends Specification {
  "order conversions" should {
    "user order conversion" in {
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
      var userOrder = UserOrder(uid, Sell, Btc, Rmb, Some(1234), Some(12), None, remainingQuantity = 12)
      userOrder must equalTo(UserOrder.fromOrderInfo(OrderInfo(Btc ~> Rmb, userOrder.toDoSubmitOrder.order, 0, 0, OrderStatus.Pending)))

      userOrder = UserOrder(uid, Buy, Btc, Rmb, Some(1234), Some(12), Some(1234 * 12), remainingQuantity = 12, remainingAmount = 1234 * 12)
      userOrder must equalTo(UserOrder.fromOrderInfo(OrderInfo(Rmb ~> Btc, userOrder.toDoSubmitOrder.order, 0, 0, OrderStatus.Pending)))

      // TODO: cover corner cases
    }
  }
}