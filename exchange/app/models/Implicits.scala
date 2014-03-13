package models

import play.api.libs.json._
import com.coinport.coinex.data.{QueryMarketResult, Order, QueryAccountResult, CashAccount}
import com.coinport.coinex.data.Currency.{Btc, Rmb}
import com.coinport.coinex.data.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json.JsString

object Implicits {
  implicit val resultWrites: Writes[ApiResult] = (
      (JsPath \ "success").write[Boolean] and
      (JsPath \ "code").write[Int] and
      (JsPath \ "message").write[String]
    )(unlift(ApiResult.unapply))

  implicit val userReads: Reads[User] = (
      (JsPath \ "username").read[String] and
      (JsPath \ "password").read[String]
    )(User.apply _)

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
