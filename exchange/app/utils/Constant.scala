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

  def agents = List(
    Agent("马*", "招商银行", "水一方", "43** **** **** **92", "15321818279"),
    Agent("杨**", "建设银行", "菜头", "12** **** **** **92", "43123454123"),
    Agent("赵*", "农工商银行", "钱多多", "3* **** **** **92", "34132344441"),
    Agent("叶**", "浦发银行", "性感小野猫", "99** **** **** **** *92", "55432121211")
  )
}
