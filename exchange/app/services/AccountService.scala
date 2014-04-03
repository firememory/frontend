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
import scala.concurrent.ExecutionContext.Implicits.global
import com.coinport.coinex.data.Currency.{Btc, Rmb}
import com.coinport.coinex.data.Implicits._

object AccountService extends AkkaService {
  def getAccount(uid: Long): Future[ApiResult] = {
    Router.backend ? QueryAccount(uid) map {
      case result: QueryAccountResult =>
        val userAccount: models.UserAccount = result.userAccount
        ApiResult(true, 0, "", Some(userAccount))
    }
  }

  def deposit(uid: Long, currency: Currency, amount: Double): Future[ApiResult] = {
    val internalAmount: Long = currency match {
      case Rmb =>
        amount unit CNY to CNY2
      case Btc =>
        amount unit BTC to MBTC
      case _ =>
        0L
    }
    val deposit = Deposit(0L, uid.toLong, currency, internalAmount, TransferStatus.Pending)
    Router.backend ? DoRequestCashDeposit(deposit) map {
      case result: RequestCashDepositSucceeded =>
        // TODO: confirm by admin dashboard
        Router.backend ! AdminConfirmCashDepositSuccess(result.deposit)

        ApiResult(true, 0, "充值申请已提交", Some(result))
      case failed: RequestCashDepositFailed =>
        ApiResult(false, 1, "充值失败", Some(failed))
    }
  }

  def submitOrder(userOrder: UserOrder): Future[ApiResult] = {
    val command = userOrder.toDoSubmitOrder
    Router.backend ? command map {
      case result: OrderSubmitted =>
        ApiResult(true, 0, "订单提交成功", Some(UserOrder.fromOrderInfo(result.originOrderInfo)))
      case failed: SubmitOrderFailed =>
        val message = failed.error match {
          case ErrorCode.InsufficientFund => "余额不足"
          case error => "未知错误-" + error
        }
        ApiResult(false, failed.error.getValue, message)
      case x =>
        ApiResult(false, -1, x.toString)
    }
  }

  def cancelOrder(id: Long, uid: Long): Future[ApiResult] = {
    println("cancel order: " + id)
    Router.backend ? DoCancelOrder(Btc ~> Rmb, id, uid) map {
      case result: OrderCancelled => ApiResult(true, 0, "订单已撤销", Some(result.order))
      case x => ApiResult(false, -1, x.toString)
    }
  }

  def getOrders(uid: Long): Future[ApiResult] = {
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
