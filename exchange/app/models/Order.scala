package models

import com.coinport.coinex.data._
import com.coinport.coinex.data.Implicits._
import scala.Some

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
  //  buy: money   out, subject in
  // sell: subject out, money   in
  val marketSide = operation match {case Buy => currency ~> subject case Sell => subject ~> currency}

  def toDoSubmitOrder(): DoSubmitOrder = {
    operation match {
      case Buy =>
        // convert price
        val newPrice = price match {
          case Some(value: Double) => Some(1D / value)
          case None => None
        }
        // regard total as quantity
        val quantity = total.getOrElse((amount.get * price.get)).toLong
        val limit = amount.map(_.toLong)
        DoSubmitOrder(marketSide, Order(uid, id, quantity , newPrice, limit))
      case Sell =>
        // TODO: handle None total or price
        val quantity: Long = amount.getOrElse[Double]((total.get / price.get).toLong).toLong
        val limit = total match {case Some(total) => Some(total.toLong) case None => None}
        DoSubmitOrder(marketSide, Order(uid, id, quantity, price, limit))
    }
  }

  def priceBy(currencyUnit: Currency) = {
    if (currencyUnit equals currency) {
      this
    } else {
      val price1 = price.map(1D / _)
      val amount1 = amount.map(v => (v * price.get))
      val total1 = amount
      UserOrder(uid, reverse(operation), currency, subject, price1, amount1, total1, status, id, submitTime)
    }
  }
}

object UserOrder {
  def fromOrderInfo(orderInfo: OrderInfo): UserOrder = {
    // all are sell-orders
    val side = orderInfo.side
    val order = orderInfo.order
    val tid = order.id
    val timestamp = order.timestamp.getOrElse(0L)
    UserOrder(order.userId, Sell, side._1, side._2, order.price, Some(order.quantity), order.takeLimit.map(_.toDouble), orderInfo.status, tid, timestamp)
  }
}