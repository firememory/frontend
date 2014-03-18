package models

import com.coinport.coinex.data._
import com.coinport.coinex.data.Implicits._
import scala.Some
import models.CurrencyUnit._
import models.CurrencyValue._
import com.coinport.coinex.data.Currency.{Btc, Rmb}

object Operation extends Enumeration {
  type Operation = Value
  val Buy, Sell = Value

  implicit def reverse(operation: Operation) = operation match {case Buy => Sell case Sell => Buy}
}

import models.Operation._
// Order from the view of users
case class UserOrder (
                       uid: Long,
                       operation: Operation,
                       subject: Currency,
                       currency: Currency,
                       price: Option[Double],
                       amount: Option[Double],
                       total: Option[Double],
                       status: OrderStatus = OrderStatus.Pending,
                       id: Long = 0L,
                       submitTime: Long = 0L
                       ) {
  private var _finishedQuantity: Double = 0
  private var _remainingQuantity: Double = amount.getOrElse(0)
  private var _finishedAmount: Double = 0
  private var _remainingAmount: Double = total.getOrElse(0)

  def finishedQuantity(): Double = _finishedQuantity

  def finishedQuantity(value: Double) = {
    _finishedQuantity = value
    this
  }

  def remainingQuantity(): Double = _remainingQuantity

  def remainingQuantity(value: Double) = {
    _remainingQuantity = value
    this
  }

  def finishedAmount(): Double = _finishedAmount

  def finishedAmount(value: Double) = {
    _finishedAmount = value
    this
  }

  def remainingAmount(): Double = _remainingAmount

  def remainingAmount(value: Double) = {
    _remainingAmount = value
    this
  }

  //  buy: money   out, subject in
  // sell: subject out, money   in
  val marketSide = operation match {case Buy => currency ~> subject case Sell => subject ~> currency}

  def toDoSubmitOrder(): DoSubmitOrder = {
    operation match {
      case Buy =>
        // convert price
        val newPrice = price match {
          case Some(value: Double) =>
            Some(((value unit (CNY, BTC)) to (CNY2, MBTC)).inverse.value)
          case None => None
        }
        // regard total as quantity
        val quantity: Long = total match {
          case Some(t) => (t unit CNY to CNY2).toLong
          case None =>
            (((amount.get unit BTC) * (price.get unit (CNY, BTC))) to CNY2).toLong
        }
        val limit = amount.map(a => (a unit BTC to MBTC).toLong)
        DoSubmitOrder(marketSide, Order(uid, id, quantity , newPrice, limit))
      case Sell =>
        // TODO: handle None total or price
        val newPrice: Option[Double] = price.map(p => (p unit (CNY, BTC) to (CNY2, MBTC)).value)
        val quantity: Long = amount match {
          case Some(a) => (a unit BTC to MBTC).toLong
          case None =>
          if (total.isDefined && price.isDefined) {
            val totalValue = (price.get unit (CNY, BTC)).inverse * (total.get unit CNY)
            (totalValue to MBTC).toLong
          } else 0
        }
        val limit = total match {
          case Some(total) => Some((total unit CNY to CNY2).toLong)
          case None => None
        }
        DoSubmitOrder(marketSide, Order(uid, id, quantity, newPrice, limit))
    }
  }
}

object UserOrder {
  def fromOrderInfo(orderInfo: OrderInfo): UserOrder = {
    // all are sell-orders
    val side = orderInfo.side
    val order = orderInfo.order

    val unit1: CurrencyUnit = side._1
    val unit2: CurrencyUnit = side._2

    val currency: Currency = unit2

    println(orderInfo + " -> " + currency)
    currency match {
      case Rmb => // sell
        val price: Option[Double] = order.price.map {
          p =>
            val priceUnit: (CurrencyUnit, CurrencyUnit) = (unit2, unit1)
            (p unit priceUnit).userValue
        }
        val amount: Option[Double] = Some((order.quantity unit unit1).userValue)
        val total: Option[Double] = order.takeLimit.map(t => (t unit unit2).userValue)


        // finished quantity = out
        val finishedQuantity: Double = (orderInfo.outAmount unit unit1).userValue
        // remaining quantity = quantity - out
        val remainingQuantity: Double = ((order.quantity - orderInfo.outAmount) unit unit1).userValue
        // finished amount = in
        val finishedAmount: Double = (orderInfo.inAmount unit unit2).userValue
        // remaining amount = take - in
        val remainingAmount: Double = order.takeLimit match {
          case Some(t) => ((t - orderInfo.inAmount) unit unit2).userValue
          case None => 0
        }

        val status = orderInfo.status
        val tid = order.id
        val timestamp = order.timestamp.getOrElse(0L)

        UserOrder(order.userId, Sell, unit1, unit2, price, amount, total, status, tid, timestamp)
          .finishedQuantity(finishedQuantity)
          .remainingQuantity(remainingQuantity)
          .finishedAmount(finishedAmount)
          .remainingAmount(remainingAmount)

      case Btc => // buy
        val price: Option[Double] = order.price.map {
          p =>
            val priceUnit: (CurrencyUnit, CurrencyUnit) = (unit2, unit1)
            (p unit priceUnit).inverse.userValue
        }

        val amount: Option[Double] = order.takeLimit.map(t => (t unit unit2).userValue)
        val total: Option[Double] = Some((order.quantity unit unit1).userValue)

        // finished quantity = in
        val finishedQuantity: Double = (orderInfo.inAmount unit unit2).userValue
        // remaining quantity = take - in
        val remainingQuantity: Double = order.takeLimit match {
          case Some(t) => ((t - orderInfo.inAmount) unit unit2).userValue
          case None => 0
        }
        // finished amount = out
        val finishedAmount: Double = (orderInfo.outAmount unit unit1).userValue
        // remaining amount = quantity - out
        val remainingAmount: Double = ((order.quantity - orderInfo.outAmount) unit unit1).userValue

        val status = orderInfo.status
        val tid = order.id
        val timestamp = order.timestamp.getOrElse(0L)

        UserOrder(order.userId, Buy, unit2, unit1, price, amount, total, status, tid, timestamp)
          .finishedQuantity(finishedQuantity)
          .remainingQuantity(remainingQuantity)
          .finishedAmount(finishedAmount)
          .remainingAmount(remainingAmount)
    }
  }
}