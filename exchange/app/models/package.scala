/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

import com.coinport.coinex.data.ChartTimeDimension._
import com.coinport.coinex.data.Currency.{Btc, Rmb}
import com.coinport.coinex.data._
import models.CurrencyUnit._
import org.json4s.ext.EnumNameSerializer
import org.json4s.JsonAST.JField
import org.json4s.native.Serialization
import org.json4s._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.JsString
import scala.concurrent.duration._

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

  implicit def tuple2CurrencyUnit(value: (Currency, Currency)): (CurrencyUnit, CurrencyUnit) = {
    val unit1: CurrencyUnit = value._1
    val unit2: CurrencyUnit = value._2
    (unit1, unit2)
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
      case "LTC" => Currency.Ltc
      case "PTS" => Currency.Pts
      case "USD" => Currency.Usd
      case _ => null
    }
  }

  implicit def currency2String(currency: Currency): String = {
    currency match {
      case Currency.Rmb => "RMB"
      case Currency.Btc => "BTC"
      case Currency.Usd => "USD"
      case Currency.Ltc => "LTC"
      case Currency.Pts => "PTS"
      case _ => currency.name.toUpperCase
    }
  }

  // currency conversions between backend and frontend
  // example: (1000, Btc) -> 1000 unit MBTC
  implicit def tuple2CurrencyValue(t: (Long, Currency)): CurrencyValue = {
    t._1 unit t._2
  }

  // example: (300, Rmb, Btc) -> 300 unit (CNY2, MBTC)
  implicit def tuple3toPriceValue(t: (Double, Currency, Currency)): PriceValue = {
    val price: PriceValue = t._1
    price unit(t._2, t._3)
  }

  // example: (300, Btc ~> Rmb) -> 300 unit (CNY2, MBTC)
  implicit def tuple2toPriceValue(t: (Double, MarketSide)): PriceValue = {
    val price: PriceValue = t._1
    price unit(t._2._2, t._2._1)
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

  // market depth conversions
  implicit def fromMarketDepth(backendObj: com.coinport.coinex.data.MarketDepth): MarketDepth = {
    val subject = backendObj.side._1
    val currency = backendObj.side._2
    val mapper = {
      item: com.coinport.coinex.data.MarketDepthItem =>
        MarketDepthItem((item.price, currency, subject).userValue, (item.quantity, subject).userValue)
    }
    val bids = backendObj.bids.map(mapper)
    val asks = backendObj.asks.map(mapper)
    MarketDepth(bids = bids, asks = asks)
  }

  // candle data conversions
  implicit def timeDimension2MilliSeconds(dimension: ChartTimeDimension): Long = {
    val duration = dimension match {
      case OneMinute => 1 minute
      case ThreeMinutes => 3 minutes
      case FiveMinutes => 5 minutes
      case FifteenMinutes => 15 minutes
      case ThirtyMinutes => 30 minutes
      case OneHour => 1 hour
      case TwoHours => 2 hours
      case FourHours => 4 hours
      case SixHours => 6 hours
      case TwelveHours => 12 hours
      case OneDay => 1 day
      case ThreeDays => 3 days
      case OneWeek => 7 days
    }
    duration.toMillis
  }

  // ticker conversions
  implicit def metrics2Ticker(metrics: MetricsByMarket): Ticker = {
    val side = metrics.side
    val currency: String = side._1
    val subject = side._1
    val price = (metrics.price, side).userValue
    val high = metrics.high.map(v => (v, side).userValue)
    val low = metrics.low.map(v => (v, side).userValue)
    val volume = (metrics.volume.getOrElse(0L), subject).userValue
    val gain = metrics.gain
    val trend = Some(metrics.direction.toString.toLowerCase)

    Ticker(
      currency = currency,
      price = price,
      volume = volume,
      high = high,
      low = low,
      gain = gain,
      trend = trend
    )
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

  implicit val TransactionDataResultsWrites = new Writes[Seq[TransactionItem]] {
    def writes(items: Seq[TransactionItem]) = {
      Json.toJson(
        items.map(item =>
          Json.obj(
            "time" -> item.timestamp,
            "price" -> (item.price unit(CNY2, MBTC) to(CNY, BTC)).value,
            "amount" -> (item.volume unit MBTC).userValue,
            "total" -> (item.amount unit CNY2).userValue,
            "maker" -> item.maker,
            "taker" -> item.taker,
            "sell" -> item.sameSide
          )
        )
      )
    }
  }

  class CandleDataItemSerializer extends CustomSerializer[CandleDataItem](
    format => ( {
      null // deserializer not implemented
    }, {
      case candleDataItem: CandleDataItem =>
        JArray(List(
          JDecimal(candleDataItem.timestamp),
          JDouble((candleDataItem.open unit(CNY2, MBTC) to(CNY, BTC)).value),
          JDouble((candleDataItem.high unit(CNY2, MBTC) to(CNY, BTC)).value),
          JDouble((candleDataItem.low unit(CNY2, MBTC) to(CNY, BTC)).value),
          JDouble((candleDataItem.close unit(CNY2, MBTC) to(CNY, BTC)).value),
          JDouble((candleDataItem.volumn unit MBTC).userValue)
        ))
    })
  )

  implicit val formats = Serialization.formats(NoTypeHints) + new EnumNameSerializer(OperationEnum) + new CandleDataItemSerializer()

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
