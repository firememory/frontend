/**
 * Copyright 2015 Coinport Inc. All Rights Reserved.
 * Author: xiaolu@coinport.com (Xiaolu Wu)
 */

package models

import com.coinport.coinex.api.model._

case class ApiV2Result(code: Int = 0, timestamp: Long = System.currentTimeMillis, data: Option[Any])

case class ApiV2History(items: Seq[ApiCandleItem])

case class ApiV2Order(
  id: String,
  operation: String,
  status: Int,
  market: String,
  price: Double,
  amount: Double,
  dealed_amount: Double,
  created: Long,
  last_updated: Option[Long]
)

case class ApiV2TransferItem(id: String, currency: String, amount: Double, status: Int, created: Long, updated: Long, address: String, txid: String)

case class ApiV2SubmitOrderResult(order_id: String = "0", code: Option[String])

case class ApiV2SubmitOrderResults(results: Seq[ApiV2SubmitOrderResult])

case class ApiV2CancelOrderResult(cancelled: Seq[Long], failed: Seq[Long])

case class ApiV2WithdrawalResult(transfer_id: Long, withdraw_status: Int)

case class ApiV2CancelWithdrawalResult(result: String)

case class ApiV2RegisterResult(uid: Long)

case class ApiV2TransfersItem(id: String, uid: String, amount: Double, status: Int, created: Long, updated: Long, operation: Int, address: String, txid: String, NxtRsString: Option[String])

case class ApiV2LoginResult(uid: Long, email: String)

case class SimpleBooleanResult(result: Boolean)

case class SendVerifyCodeResult(sendToPhone: Boolean, phoneUuid: Option[String], sendToEmail: Boolean, emailUuid: Option[String])

case class ApiV2BankCard(bankName: String, ownerName: String, cardNumber: String, branchBankName: Option[String])

case class SendMobileBindCodeResult(phoneUuid: String)

case class ApiV2ApiTokenResult(token: String, secret: Option[String])

case class ExternalTicker(high: Double, low: Double, last: Double, vol: Double, buy: Double, sell: Double)

case class RetryTime(canRetryTime: Int)

object ApiErrorCode extends Enumeration(initial = 7000) {
  type ApiErrorCode = Value
  val UnsupportCurrency, UnsupportMarket = Value
}
