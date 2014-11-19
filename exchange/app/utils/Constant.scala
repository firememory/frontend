package utils

import play.api.Play
import com.coinport.coinex.data.Implicits._
import com.coinport.coinex.data.{Currency, MarketSide}
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

  def markets = {
    config.getList("exchange.markets").get.unwrapped()
  }

  def cnymarkets = {
    config.getList("exchange.cnymarkets").get.unwrapped()
  }

  def allmarkets = markets.toArray.toSeq ++ cnymarkets.toArray.toSeq

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
    Agent("杨*", "招商银行", "喵喵（招行）", "62** **** **** **92", "209063895")
  )
}
