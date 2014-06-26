package utils

import play.api.Play
import com.coinport.coinex.data.Implicits._
import com.coinport.coinex.data.MarketSide

object Constant {
  private lazy val config = Play.current.configuration

  val cookieNameTimestamp = "COINPORT_COOKIE_TIMESTAMP"

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
}