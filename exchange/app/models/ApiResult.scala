/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package models

case class ApiResult(success: Boolean = true, code: Int = 0, message: String = "", data: Option[Any] = None)

case class SubmitOrderResult(order: UserOrder)

case class UserAccount(uid: Long, accounts: Map[String, Double] = Map())

case class MarketDepthItem(price: Double, amount: Double)

case class MarketDepth(bids: Seq[MarketDepthItem], asks: Seq[MarketDepthItem])

case class Ticker(currency:String, price: Double, high: Option[Double] = None, low: Option[Double] = None, volume: Double, gain: Option[Double] = None, trend: Option[String] = None)