package services

import com.coinport.coinex.data._
import akka.pattern.ask
import scala.concurrent.Future
import akka.persistence.Persistent
import com.coinport.coinex.data.Currency._
import scala.Some

object AccountService extends AkkaService{
  def getAccount(uid: Long): Future[Any] = {
    Router.routers.accountView ? QueryAccount(uid)
  }

  def deposit(uid: Long, currency: Currency, amount: Long) = {
    Router.routers.accountProcessor ? Persistent(DoDepositCash(uid.toLong, currency, amount))
  }

  def submitOrder(marketSide: MarketSide, uid: Long, amount: Long, price: Double) = {
    // TODO(cm): give 0L as default id
    val tid = System.currentTimeMillis
    Router.routers.accountProcessor ? Persistent(DoSubmitOrder(marketSide, Order(uid, tid, amount, Some(price))))
  }
}
