/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package models

import play.api.libs.json._
import com.coinport.coinex.data._
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json.JsString
import models.CurrencyValue._
import models.CurrencyUnit._
import models.PriceValue._

object Implicits {
  implicit def string2Currency(currencyString: String): Currency = {
    currencyString.toUpperCase match {
      case "RMB" => Currency.Rmb
      case "CNY" => Currency.Rmb
      case "BTC" => Currency.Btc
      case "USD" => Currency.Usd
      case _ => null
    }
  }

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
    def writes(cachAccount: CashAccount) = {
      val currency = cachAccount.currency
      val currencyUnit: CurrencyUnit = currency
      val available = (cachAccount.available unit currencyUnit).userValue
      val locked = (cachAccount.locked unit currencyUnit).userValue
      Json.obj(
      "currency" -> JsString(currency.toString),
      "available" -> available,
      "locked" -> locked
    )}
  }

  implicit val queryAccountResultWrites = new Writes[QueryAccountResult] {
    def writes(obj: QueryAccountResult) = Json.obj(
      "uid" -> obj.userAccount.userId,
      "RMB" -> (obj.userAccount.cashAccounts.getOrElse(Rmb, CashAccount(Rmb, 0, 0, 0)).available unit CNY2).userValue,
      "BTC" -> (obj.userAccount.cashAccounts.getOrElse(Btc, CashAccount(Btc, 0, 0, 0)).available unit MBTC).userValue,
      "accounts" -> obj.userAccount.cashAccounts.map(_._2)
    )
  }

  implicit val userOrderWrites = new Writes[UserOrder] {
    def writes(obj: UserOrder) = Json.obj(
      "tid" -> obj.id,
      "date" -> obj.submitTime,
      "type" -> obj.operation.toString.toLowerCase,
      "status" -> obj.status.getValue,
      "price" -> obj.price,
      "amount" -> obj.amount,
      "finished" -> obj.finishedQuantity,
      "remaining" -> obj.remainingQuantity,
      "finishedAmount" -> obj.finishedAmount,
      "remainingAmount" -> obj.remainingAmount,
      "total" -> obj.total
    )
  }

//  implicit val userLogWrites = new Writes[UserLog] {
//    def writes(obj: UserLog) = Json.obj(
//      "orders" -> obj.map{
//        orderInfo =>
//          val userOrder = UserOrder.fromOrderInfo(orderInfo)
//          userOrder.priceBy(Rmb)
//      },
//      "orderInfos" -> obj.orderInfos.map(_.toString)
//    )
//  }

  implicit val orderWrites = new Writes[Order] {
    def writes(obj: Order) = Json.arr(
      obj.price,
      obj.quantity,
      obj.userId
    )
  }

  implicit val marketDepthItemWrites = new Writes[MarketDepthItem] {
    def writes(obj: MarketDepthItem) = Json.obj(
      "price" -> (obj.price unit (CNY2, MBTC)).userValue,
      "amount" -> (obj.quantity unit MBTC).userValue
    )
  }

  implicit val queryMarketResultWrites = new Writes[QueryMarketResult] {
    def writes(obj: QueryMarketResult) = Json.obj(
      "asks" -> obj.marketDepth.asks,
      "bids" -> obj.marketDepth.bids
    )
  }

  implicit val marketCandleDataResultsWrites = new Writes[QueryMarketCandleDataResult] {
    def writes(obj: QueryMarketCandleDataResult) = Json.arr(
      obj.candleData._2.map( candleDataItem => Json.arr(
        candleDataItem._1,
        candleDataItem._3,
        candleDataItem._6,
        candleDataItem._5,
        candleDataItem._4,
        candleDataItem._2
        )
      )
    )
  }
}
