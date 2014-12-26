package utils

import play.api.Play
import com.coinport.coinex.data.Implicits._
import com.coinport.coinex.data.{ Currency, MarketSide }
import com.coinport.coinex.api.model._
import models.Agent

object Constant {
  private lazy val config = Play.current.configuration

  val cookieNameTimestamp = "COINPORT_COOKIE_TIMESTAMP"
  val cookieNameMobile = "COINPORT_COOKIE_MOBILE"
  val cookieNameMobileVerified = "COINPORT_COOKIE_MOBILE_VERIFIED"
  val cookieNameRealName = "COINPORT_COOKIE_REAL_NAME"
  val cookieGoogleAuthSecret = "CP_GAS"
  val securityPreference = "CP_SP"
  val userRealName = "U_RN"

  val coinChineseNames = Map(
    "CNY" -> "人民币",
    "BTC" -> "比特币",
    "LTC" -> "莱特币",
    "DOGE" -> "狗狗币",
    "BC" -> "黑币",
    "DRK" -> "暗黑币",
    "VRC" -> "维理币",
    "ZET" -> "泽塔币",
    "BTSX" -> "比特股",
    "NXT" -> "未来币",
    "XRP" -> "瑞波币",
    "GOOC" -> "谷壳币"
  )

  val coinEnglishNames = Map(
    "CNY" -> "ChineseYuan",
    "BTC" -> "Bitcoin",
    "LTC" -> "Litecoin",
    "DOGE" -> "Dogecoin",
    "BC" -> "Blackcoin",
    "DRK" -> "Darkcoin",
    "VRC" -> "Vericoin",
    "ZET" -> "Zetacoin",
    "BTSX" -> "BitsharesX",
    "NXT" -> "Nextcoin",
    "XRP" -> "Ripple",
    "GOOC" -> "Goocoin"
  )

  val supportedBankNames = Seq(
    "建设银行",
    "工商银行",
    "农业银行",
    "中国银行",
    "交通银行",
    "广发银行",
    "民生银行",
    "中信银行",
    "平安银行",
    "兴业银行",
    "光大银行",
    "浦发银行",
    "进出口银行",
    "华夏银行",
    "国家开发银行",
    "招商银行")

  def markets = {
    config.getList("exchange.markets").get.unwrapped()
  }

  def cnymarkets = {
    config.getList("exchange.cnymarkets").get.unwrapped()
  }

  def allmarkets = cnymarkets.toArray.toSeq ++ markets.toArray.toSeq

  def coins = {
    config.getList("exchange.coins").get.unwrapped()
  }

  def allcoins = {
    config.getList("exchange.allcoins").get.unwrapped()
  }

  def marketSides: Seq[MarketSide] = {
    markets.toArray.toSeq.map {
      case m: String =>
        val side: MarketSide = m
        side
    }
  }

  def allMarketSides: Seq[MarketSide] = {
    (markets.toArray.toSeq ++ cnymarkets.toArray.toSeq).map {
      case m: String =>
        val side: MarketSide = m
        side
    }
  }

  def btcMarketSides: Seq[MarketSide] = {
    markets.toArray.toSeq.map {
      case m: String =>
        val side: MarketSide = m
        side
    }
  }

  def cnyMarketSides: Seq[MarketSide] = {
    cnymarkets.toArray.toSeq.map {
      case m: String =>
        val side: MarketSide = m
        side
    }
  }

  def currencySeq = {
    coins.toArray.toSeq.map {
      case m: String =>
        val c: Currency = m
        c
    }
  }

  def agents = List(
    Agent("杨*", "招商银行", "喵喵（招行）", "62** **** **** **92", "209063895"))
}
