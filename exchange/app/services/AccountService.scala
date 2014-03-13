/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package services

import com.coinport.coinex.data._
import akka.pattern.ask
import scala.concurrent.Future
import models.UserOrder
import models.Implicits._

object AccountService extends AkkaService{
  def getAccount(uid: Long): Future[Any] = {
    Router.backend ? QueryAccount(uid)
  }

  def deposit(uid: Long, currency: Currency, amount: Long) = {
    Router.backend ? DoDepositCash(uid.toLong, currency, amount)
  }

  def submitOrder(userOrder: UserOrder) = {
    val command: DoSubmitOrder = userOrder
    Router.backend ? command
  }
}
