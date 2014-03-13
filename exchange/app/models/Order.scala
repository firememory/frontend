package models

import com.coinport.coinex.data.{OrderStatus, Currency}
import com.coinport.coinex.data.Implicits._

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
                       amount: Option[Long],
                       total: Option[Double],
                       status: OrderStatus = OrderStatus.Pending,
                       id: Long = 0L
                       ) {
  //  buy: money   out, subject in
  // sell: subject out, money   in
  val marketSide = operation match {case Buy => currency ~> subject case Sell => subject ~> currency}
  def priceBy(currencyUnit: Currency) = {
    if (currencyUnit equals currency) {
      this
    } else {
      val price1 = price.map(1D / _)
      val amount1 = amount.map(v => (v * price.get).toLong)
      val total1 = amount.map(_.toDouble)
      UserOrder(uid, reverse(operation), currency, subject, price1, amount1, total1, status, id)
    }
  }
}
