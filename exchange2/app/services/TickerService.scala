package services

import models._
import dispatch._
import com.typesafe.config.ConfigFactory
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.duration.DurationInt

object TickerService {

  val tickerConfigs: Map[String, Map[String, String]] = Map(
    "BTC-CNY" -> Map(
      "OKCOIN" -> "https://www.okcoin.cn/api/v1/ticker.do?symbol=btc_cny",
      "HUOBI" -> "http://api.huobi.com/staticmarket/ticker_btc_json.js",
      "BTCCHINA" -> "https://data.btcchina.com/data/ticker?market=btccny") //TODO(xiaolu) only support btc-cny market now.
      // "LTC-CNY" -> Map(
      //   "OKCOIN" -> "https://www.okcoin.cn/api/v1/ticker.do?symbol=ltc_cny",
      //   "HUOBI" -> "http://api.huobi.com/staticmarket/ticker_ltc_json.js",
      //   "BTCCHINA" -> "https://data.btcchina.com/data/ticker?market=ltccny"
      // ),
      // "BTC-USD" -> Map(
      //   "OKCOIN" -> "https://www.okcoin.com/api/v1/ticker.do?symbol=btc_usd"
      // ),
      // "LTC-USD" -> Map(
      //   "OKCOIN" -> "https://www.okcoin.com/api/v1/ticker.do?symbol=ltc_usd"
      // )
      )

  def scheduleUpdateCache = {
    Akka.system.scheduler.schedule(30 seconds, 5 seconds) {
      tickerConfigs foreach {
        case m =>
          val market = m._1
          m._2 foreach { e =>
            val updatedValue = Ticker(e._1, e._2).ticker
            if (updatedValue.isDefined) {
              TickerCacheService.put(market + "_" + e._1, updatedValue.get)
            } else {
              throw new IllegalAccessException(s"fetch ticker data from $e._1 failed.")
            }
          }
      }
    }
  }

  scheduleUpdateCache

  def getTickers: Map[String, Map[String, ExternalTicker]] = {
    tickerConfigs map {
      case m =>
        val market = m._1
        var tickers: Map[String, ExternalTicker] = Map.empty
        m._2 foreach {
          case e =>
            val cachedValue = TickerCacheService.get(market + "_" + e._1)
            val t = if (cachedValue == null) Ticker(e._1, e._2).ticker else Some(cachedValue)
            if (t.isDefined) {
              if (cachedValue == null) TickerCacheService.put(market + "_" + e._1, t.get)
              tickers = tickers + (e._1 -> t.get)
            }
        }
        market -> tickers
    } toMap
  }
}

object TickerCacheService {

  val maximumSize = 100

  val cache: Cache[String, ExternalTicker] = CacheBuilder.newBuilder()
    .maximumSize(maximumSize)
    .expireAfterWrite(30, TimeUnit.SECONDS)
    .build()

  def put(key: String, value: ExternalTicker): Unit = cache.put(key, value)

  def get(key: String): ExternalTicker = {
    cache.getIfPresent(key)
  }
}

case class Ticker(exchange: String, tUrl: String) {

  def fetch(): String =
    Http(url(tUrl) OK as.String).either() match {
      case Left(_) => ""
      case Right(s) => s
    }

  def transfer(jsonStr: String): Option[ExternalTicker] = {
    if (jsonStr.isEmpty) None else {
      val json = Json.parse(jsonStr) \ "ticker"
      val high = getValue(json, "high")
      val low = getValue(json, "low")
      val last = getValue(json, "last")
      val vol = getValue(json, "vol")
      val buy = getValue(json, "buy")
      val sell = getValue(json, "sell")
      Some(ExternalTicker(high, low, last, vol, buy, sell))
    }
  }

  private def getValue(jv: JsValue, key: String): Double = {
    val v = (jv \ key).asOpt[Double]
    if (v.isDefined) {
      v.get
    } else {
      val vs = (jv \ key).asOpt[String]
      if (vs.isDefined)
        vs.get.toDouble
      else
        0.0
    }
  }

  def ticker() = {
    transfer(fetch)
  }
}

