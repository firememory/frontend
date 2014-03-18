/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package services

import com.coinport.coinex.data._
import akka.pattern.ask
import scala.concurrent.Future
import models.UserOrder
import models.CurrencyUnit._
import models.CurrencyValue._
import com.coinport.coinex.data.Currency.{Btc, Rmb}

object AccountService extends AkkaService{
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
    Router.backend ? DoDepositCash(uid.toLong, currency, amount1)
  }

  def submitOrder(userOrder: UserOrder) = {
    val command = userOrder.toDoSubmitOrder
    println("post " + command)
    Router.backend ? command
  }

  def getOrders(uid: Long) = {
    println("query orders of user " + uid)
    Router.backend ? QueryUserOrders(uid)
  }
}
