package models

import com.coinport.coinex.data.Currency
import com.coinport.coinex.data.Implicits._

object Operation extends Enumeration {
  type Operation = Value
  val Buy, Sell = Value
}

import models.Operation._
// Order from the view of users
case class UserOrder (uid: Long, operation: Operation, subject: Currency, currency: Currency, price: Option[Double], amount: Option[Long], total: Option[Double]) {
  //  buy: money   out, subject in
  // sell: subject out, money   in
  val marketSide = operation match {case Buy => currency ~> subject case Sell => subject ~> currency}
}
