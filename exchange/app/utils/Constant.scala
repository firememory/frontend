package utils

import play.api.Play
import com.coinport.coinex.data.Implicits._
import com.coinport.coinex.data.{Currency, MarketSide}
import com.coinport.coinex.api.model._

object Constant {
  private lazy val config = Play.current.configuration

  val cookieNameTimestamp = "COINPORT_COOKIE_TIMESTAMP"
  val cookieNameMobile = "COINPORT_COOKIE_MOBILE"
  val cookieNameMobileVerified = "COINPORT_COOKIE_MOBILE_VERIFIED"
  val cookieNameRealName = "COINPORT_COOKIE_REAL_NAME"
  val cookieGoogleAuthSecret = "CP_GAS"
  val securityPreference = "CP_SP"

  def markets = {
    config.getList("exchange.markets").get.unwrapped()
  }

  def coins = {
    config.getList("exchange.coins").get.unwrapped()
  }

  def marketSides: Seq[MarketSide] = {
    markets.toArray.toSeq.map {
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
}
