/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package models

import play.api.libs.json._
import com.coinport.coinex.data._
import com.coinport.coinex.data.Currency._
import play.api.libs.functional.syntax._
import models.CurrencyValue._
import models.CurrencyUnit._
import play.api.libs.json.JsString
import org.json4s.ext.EnumNameSerializer

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
      "accounts" -> obj.userAccount.cashAccounts.map(_._2).toSeq
    )
  }

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

  implicit val queryMarketResultWrites = new Writes[QueryMarketDepthResult] {
    def writes(obj: QueryMarketDepthResult) = Json.obj(
      "asks" -> obj.marketDepth.asks,
      "bids" -> obj.marketDepth.bids
    )
  }

  implicit val candleDataItemWrites = new Writes[CandleDataItem] {
    def writes(candleDataItem: CandleDataItem) = {
      Json.arr(
        candleDataItem.timestamp,
        (candleDataItem.open unit (CNY2, MBTC) to (CNY, BTC)).value,
        (candleDataItem.high unit (CNY2, MBTC) to (CNY, BTC)).value,
        (candleDataItem.low unit (CNY2, MBTC) to (CNY, BTC)).value,
        (candleDataItem.close unit (CNY2, MBTC) to (CNY, BTC)).value,
        (candleDataItem.volumn unit MBTC).userValue
      )
    }
  }

  implicit val marketCandleDataResultsWrites = new Writes[Seq[CandleDataItem]] {
    def writes(candles: Seq[CandleDataItem]) = {
       Json.toJson(candles)
    }
  }

  implicit val TransactionDataResultsWrites = new Writes[Seq[TransactionItem]] {
    def writes(items: Seq[TransactionItem]) = {
      Json.toJson(
        items.map(item =>
          Json.obj(
            "time" -> item.timestamp,
            "price" -> (item.price unit (CNY2, MBTC) to (CNY, BTC)).value,
            "amount" -> (item.volumn unit MBTC).userValue,
            "total" -> (item.amount unit CNY2).userValue,
            "maker" -> item.maker,
            "taker" -> item.taker,
            "sell" -> item.sameSide
          )
        )
      )
    }
  }

  import org.json4s._
  import org.json4s.JsonDSL._
  import org.json4s.native.Serialization
  import org.json4s.native.Serialization.write

  implicit val simpleApiResultWrites = new Writes[ApiResult] {
    implicit val formats = Serialization.formats(NoTypeHints) +
      new EnumNameSerializer(OperationEnum)

    def writes(result: ApiResult) = {
      Json.parse(write(result))
    }
  }

  implicit def fromOrderSubmitted(obj: OrderSubmitted): SubmitOrderResult = {
    val orderInfo = obj.originOrderInfo
    val order = UserOrder.fromOrderInfo(orderInfo)
    SubmitOrderResult(order)
  }
}
