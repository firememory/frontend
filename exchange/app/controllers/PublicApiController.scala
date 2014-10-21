package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.coinport.coinex.data._
import scala.concurrent.Future
import scala.Some
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data.Implicits._
import com.coinport.coinex.api.model._
import com.coinport.coinex.api.service._
import com.github.tototoshi.play2.json4s.native.Json4s
import controllers.ControllerHelper._
import utils.Constant

object PublicApiController extends Controller with Json4s {

  case class SimpleApiMarketDepth(bids: Seq[List[Double]], asks: Seq[List[Double]])

  def depth(market: String) = Action.async {
    implicit request =>
      val query = request.queryString
      val depth = getParam(query, "depth", "5").toInt

      MarketService.getDepth(market, depth).map {
        case res: ApiResult =>
          val simpleRes = ApiResult(data = res.data.map(d => ApiMarketDepth2Simple(d.asInstanceOf[ApiMarketDepth])))
          Ok(simpleRes.toJson)
        case e: Any => Ok(e.toJson)
      }
  }

  private def ApiMarketDepth2Simple(amd: ApiMarketDepth) = {
    val bids = amd.bids.map(item => List(item.price.value, item.amount.value))
    val asks = amd.asks.map(item => List(item.price.value, item.amount.value))
    SimpleApiMarketDepth(bids, asks)
  }

}
