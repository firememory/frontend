/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

import com.coinport.coinex.data.Currency.{Btc, Rmb}
import com.coinport.coinex.data._
import models.CurrencyUnit._
import org.json4s.ext.EnumNameSerializer
import org.json4s.JsonAST.JField
import org.json4s.native.Serialization
import org.json4s._
import play.api.libs.json._
import play.api.libs.functional.syntax._

package object models {
  implicit def long2CurrencyUnit(value: Long) = new CurrencyValue(value)
  implicit def double2CurrencyUnit(value: Double) = new CurrencyValue(value)
  implicit def currencyUnit2Long(value: CurrencyValue) = value.toLong
  implicit def currencyUnit2Double(value: CurrencyValue) = value.toDouble

  implicit def double2PriceUnit(value: Double): PriceValue = new PriceValue(value)
  implicit def priceUnit2Double(value: PriceValue) = value

  // internal unit of backend
  implicit def currency2CurrencyUnit(value: Currency): CurrencyUnit = {
    value match {
      case Btc => MBTC
      case Rmb => CNY2
      case _ => NO_UNIT
    }
  }

  implicit def currencyUnit2Currency(value: CurrencyUnit): Currency = {
    value match {
      case BTC => Btc
      case MBTC => Btc
      case CNY => Rmb
      case CNY2 => Rmb
    }
  }

  implicit def string2Currency(currencyString: String): Currency = {
    currencyString.toUpperCase match {
      case "RMB" => Currency.Rmb
      case "CNY" => Currency.Rmb
      case "BTC" => Currency.Btc
      case "USD" => Currency.Usd
      case _ => null
    }
  }

  implicit def currency2String(currency: Currency): String = {
    currency match {
      case Currency.Rmb => "RMB"
      case Currency.Btc => "BTC"
      case Currency.Usd => "USD"
      case _ => currency.name.toUpperCase
    }
  }

  // (1000, btc) -> 1000 unit MBTC
  implicit def tuple2CurrencyValue(t: (Long, Currency)): CurrencyValue = {
    t._1 unit t._2
  }

  // user account conversions

  implicit def fromUserAccount(backendObj: com.coinport.coinex.data.UserAccount): UserAccount = {
    val uid = backendObj.userId
    val map: Map[String, Double] = backendObj.cashAccounts.map {
      case (k, v) =>
        val currency: String = k
        currency -> (v.available, v.currency).userValue
    }.toMap

    UserAccount(uid, accounts = map)
  }

  // Json compose / decompose

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
      )
    }
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
      "price" -> (obj.price unit(CNY2, MBTC)).userValue,
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
        (candleDataItem.open unit(CNY2, MBTC) to(CNY, BTC)).value,
        (candleDataItem.high unit(CNY2, MBTC) to(CNY, BTC)).value,
        (candleDataItem.low unit(CNY2, MBTC) to(CNY, BTC)).value,
        (candleDataItem.close unit(CNY2, MBTC) to(CNY, BTC)).value,
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
            "price" -> (item.price unit(CNY2, MBTC) to(CNY, BTC)).value,
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

  implicit val formats = Serialization.formats(NoTypeHints) + new EnumNameSerializer(OperationEnum)

  class JsonSupportWrapper(obj: Any) {
    def toJson(): JValue = {
      val json = Extraction.decompose(obj)
      json filterField {
        case JField(name, value) =>
          !name.startsWith("_") // filter fields starting with underscore
        case _ => false
      }
      json
    }
  }

  implicit def toJsonSupportWrapper(obj: Any): JsonSupportWrapper = new JsonSupportWrapper(obj)

  implicit def fromOrderSubmitted(obj: OrderSubmitted): SubmitOrderResult = {
    val orderInfo = obj.originOrderInfo
    val order = UserOrder.fromOrderInfo(orderInfo)
    SubmitOrderResult(order)
  }
}
