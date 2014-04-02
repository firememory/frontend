/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package services

import com.coinport.coinex.data._
import akka.pattern.ask
import scala.concurrent.Future
import models._
import models.CurrencyUnit._
import models.CurrencyValue._
import scala.concurrent.ExecutionContext.Implicits.global
import com.coinport.coinex.data.Currency.{Btc, Rmb}
import com.coinport.coinex.data.Implicits._

object AccountService extends AkkaService {
  def getAccount(uid: Long): Future[Any] = {
    Router.backend ? QueryAccount(uid)
  }

  def deposit(uid: Long, currency: Currency, amount: Double) = {
    val amount1: Long = currency match {
      case Rmb =>
        amount unit CNY to CNY2
      case Btc =>
        amount unit BTC to MBTC
      case _ =>
        0L
    }
    Router.backend ? DoRequestCashDeposit(uid.toLong, currency, amount1)
  }

  def submitOrder(userOrder: UserOrder) = {
    val command = userOrder.toDoSubmitOrder
    Router.backend ? command map {
      case result: OrderSubmitted =>
        ApiResult(true, 0, "订单提交成功", Some(UserOrder.fromOrderInfo(result.originOrderInfo)))
      case failed: SubmitOrderFailed =>
        ApiResult(true, 1, failed.error.toString)
      case x =>
        ApiResult(false, -1, x.toString)
    }
  }

  def cancelOrder(id: Long, uid: Long) = {
    println("cancel order: " + id)
    Router.backend ? DoCancelOrder(Btc ~> Rmb, id, uid) map {
      case result: OrderCancelled => ApiResult(true, 0, "订单已撤销", Some(result.order))
      case x => ApiResult(false, -1, x.toString)
    }
  }

  def getOrders(uid: Long) = {
    Router.backend ? QueryUserOrders(uid) map {
      case result: QueryUserOrdersResult =>
        val data = result.orders.map {
          o =>
            UserOrder.fromOrderInfo(o)
        }.toSeq
        ApiResult(true, 0, "", Some(data))
      case x => ApiResult(false, -1, x.toString)
    }
  }
}
