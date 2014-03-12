package services

import com.coinport.coinex.data._
import akka.pattern.ask
import scala.concurrent.Future
import com.coinport.coinex.data.Currency._

object MarketService extends AkkaService{
  def getDepth(marketSide: MarketSide, depth: Int): Future[Any] = {
    Router.routers.marketViews(marketSide) ? QueryMarket(marketSide, depth)
  }
}
