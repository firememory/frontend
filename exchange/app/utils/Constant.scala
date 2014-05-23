package utils

import play.api.Play

object Constant {
  private lazy val config = Play.current.configuration

  val cookieNameTimestamp = "COINPORT_COOKIE_TIMESTAMP"

  def markets = {
    config.getList("exchange.markets").get.unwrapped()
  }

  def coins = {
    config.getList("exchange.coins").get.unwrapped()
  }
}
