package models

import play.api.libs.json.{JsString, Json, Writes}
import com.coinport.coinex.data.{QueryMarketResult, Order, QueryAccountResult, CashAccount}
import com.coinport.coinex.data.Currency.{Btc, Rmb}
import com.coinport.coinex.data.Implicits._

object Implicits {
  implicit val cashAccountWrites = new Writes[CashAccount] {
    def writes(cachAccount: CashAccount) = Json.obj(
      "currency" -> JsString(cachAccount.currency.toString),
      "available" -> cachAccount.available,
      "locked" -> cachAccount.locked
    )
  }


  implicit val queryAccountResultWrites = new Writes[QueryAccountResult] {
    def writes(obj: QueryAccountResult) = Json.obj(
      "uid" -> obj.userAccount.userId,
      "RMB" -> obj.userAccount.cashAccounts.getOrElse(Rmb, CashAccount(Rmb, 0, 0)).available,
      "BTC" -> obj.userAccount.cashAccounts.getOrElse(Btc, CashAccount(Btc, 0, 0)).available,
      "accounts" -> obj.userAccount.cashAccounts.map(_._2)
    )
  }

  implicit val orderWrites = new Writes[Order] {
    def writes(obj: Order) = Json.arr(
      obj.price,
      obj.quantity,
      obj.userId
    )
  }

  implicit val queryMarketResultWrites = new Writes[QueryMarketResult] {
    def writes(obj: QueryMarketResult) = Json.obj(
      "asks" -> obj.orders1,
      "bids" -> obj.orders2.map(_.inversePrice)
    )
  }
}
