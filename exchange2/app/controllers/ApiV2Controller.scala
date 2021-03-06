/**
 * Copyright (C) 2015 Coinport Inc.
 * Author: Xiaolu Wu (xiaolu@coinport.com)
 */

package controllers

import play.api.mvc._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.coinport.coinex.data._
import scala.concurrent._
import scala.Some
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.data.Implicits._
import com.coinport.coinex.api.model._
import com.coinport.coinex.api.service._
import com.github.tototoshi.play2.json4s.native.Json4s
import controllers.ControllerHelper._
import utils.Constant
import utils.HdfsAccess
import utils.MHash
import utils.SecurityPreferenceUtil
import controllers.GoogleAuth.{ GoogleAuthenticator, GoogleAuthenticatorKey }
import models._
import services.TickerService
import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.google.common.io.BaseEncoding
import java.util.{ UUID, Properties }

object ApiV2Controller extends Controller with Json4s with AccessLogging {

  import akka.util.Timeout
  import scala.concurrent.duration._

  implicit val timeout = Timeout(2 seconds)
  val logger = Logger(this.getClass)

  def preflight(all: String) = Action { implicit request =>
    Ok("").withHeaders(
      "Access-Control-Allow-Methods" -> "POST, GET, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Origin" -> request.headers.get("Origin").getOrElse("*"),
      "Access-Control-Allow-Credentials" -> "true",
      "Access-Control-Allow-Headers" -> "Origin, Authorization, X-XSRF-TOKEN, X-Requested-With, Content-Type, Accept, Referrer, User-Agent",
      "Access-Control-Max-Age" -> "3600")
  }

  def tickers(currency: String) = if (currency.equalsIgnoreCase(Currency.Cny.toString)) tickerBasic(Constant.cnyMarketSides)
  else if (currency.equalsIgnoreCase(Currency.Btc.toString)) tickerBasic(Constant.btcMarketSides)
  else tickerBasic(Constant.allMarketSides)

  def allTickers() = tickerBasic(Constant.allMarketSides)

  def ticker(market: String) = tickerBasic(Seq(string2RichMarketSide(market)))

  private def tickerBasic(sides: Seq[MarketSide]) = Action.async {
    implicit request =>
      val r = MarketService.getJsonTickers(sides)
      r.map(result => Ok(ApiV2Result(data = result.data).toJson))
  }

  def externalTickers(currency: String) = Action {
    implicit request =>
      if ("BTC".equalsIgnoreCase(currency)) {
        val tickers = TickerService.getTickers
        val updatedTickers = tickers map { m =>
          val ts = m._2 map { t =>
            t._1 -> Seq(t._2.last, t._2.high, t._2.low, t._2.vol, t._2.buy, t._2.sell)
          }
          m._1 -> ts
        }
        Ok(ApiV2Result(data = Some(updatedTickers)).toJson)
      } else {
        Ok(defaultApiV2Result(ApiErrorCode.UnsupportCurrency.id).toJson)
      }
  }

  def reserves() = Action.async {
    implicit request =>
      val totalReserveFuture = AccountService.getAccount(-1000L)
      val totalReserveResult = Await.result(totalReserveFuture, 5 seconds).asInstanceOf[ApiResult]
      val totalReserve = totalReserveResult.data.get.asInstanceOf[ApiUserAccount]
      val reserves = Constant.supportReserveCoins map {
        case c =>
          val reserveFuture = OpenService.getCurrencyReserve(c)
          val reserveApiResult = Await.result(reserveFuture, 5 seconds).asInstanceOf[ApiResult]
          if (reserveApiResult.success) {
            val reserve = reserveApiResult.data.get.asInstanceOf[ApiCurrencyReserve]
            val total = if (totalReserve.accounts.get(c.toString.toUpperCase).isDefined) totalReserve.accounts.get(c.toString.toUpperCase).get.total.value else 0.0
            c.toString.toUpperCase -> Seq(reserve.hot.value, reserve.cold.value, reserve.user.value, total)
          } else {
            c.toString.toUpperCase -> Seq.empty
          }
      }
      val result = ApiV2Result(data = Some(reserves.toMap))
      Future(Ok(result.toJson))
  }

  def reserve(currency: String) = Action.async {
    implicit request =>
      if (Constant.supportReserveCoins.contains(string2Currency(currency))) {
        val totalReserveFuture = AccountService.getAccount(-1000L)
        val totalReserveResult = Await.result(totalReserveFuture, 5 seconds).asInstanceOf[ApiResult]
        val totalReserve = totalReserveResult.data.get.asInstanceOf[ApiUserAccount]
        val total = if (totalReserve.accounts.get(currency.toUpperCase).isDefined) totalReserve.accounts.get(currency.toUpperCase).get.total.value else 0.0
        BitwayService.getReserve(string2Currency(currency)).map { result =>
            val detail = result.data.get.asInstanceOf[ApiDetailReserve]
            val updatedStat = Seq(detail.stats(2), detail.stats(3), detail.stats(1), total)
            val updateDetail = ApiDetailReserve(detail.timestamp, detail.currency, updatedStat, detail.distribution)
          Ok(ApiV2Result(data = result.data).toJson)}
        } else {
          Future(Ok(defaultApiV2Result(ApiErrorCode.UnsupportCurrency.id).toJson))
        }
  }

  def balanceSnapshotFiles(currency: String) = Action {
    implicit request =>
      if (Constant.supportReserveCoins.contains(string2Currency(currency))) {
        val pager = ControllerHelper.parseApiV2NextPageParam()
        val path = "csv/asset/" + currency.toLowerCase
        val files = HdfsAccess.listFiles(path)
          .sortWith((a, b) => a.updated > b.updated)

        val from = if (pager.from.isDefined) pager.from.get.toLong else Long.MaxValue
        val limit = pager.limit

        val items = files.dropWhile(p => p.updated >= from)

        val jsonFormated = items.take(limit) map { f =>
          Seq(f.name, f.size, f.updated)
        }

        val downloadPreUrl = "https://exchange.coinport.com/download/" + path + "/"

        val data = ApiV2PagingWrapper(
          hasMore = limit < items.size,
          currency = currency,
          path = downloadPreUrl,
          items = jsonFormated)
        Ok(ApiV2Result(data = Some(data)).toJson)
      } else {
        Ok(defaultApiV2Result(ApiErrorCode.UnsupportCurrency.id).toJson)
      }
  }

  def transfers(currency: String) = Action.async {
    implicit request =>
      if (Constant.allCurrencySeq.contains(string2Currency(currency))) {
        val query = request.queryString
        val status = getParam(query, "status").map(s => TransferStatus.get(s.toInt).getOrElse(TransferStatus.Succeeded))
        val types = getParam(query, "type").map(s => TransferType.get(s.toInt).getOrElse(TransferType.Deposit)) match {
          case Some(t) => Seq(t)
          case None => Seq(TransferType.Deposit, TransferType.Withdrawal)
        }
        val pager = ControllerHelper.parseApiV2NextPageParam()
        val from = if (pager.from.isDefined) pager.from.get.toLong else Long.MaxValue

        val typeList = if (types.toSet.contains(TransferType.Deposit)) types :+ TransferType.DepositHot else types

        TransferService.getTransfers(None, Currency.valueOf(currency), None, None, typeList, Cursor(0, pager.limit), fromId = Some(from), needCount = false) map {
          case result =>
            if (result.success) {
              val transfers = result.data.get.asInstanceOf[ApiPagingWrapper].items.asInstanceOf[Seq[ApiTransferItem]]
              val v2Transfers = transfers.map(t => ApiV2TransfersItem(t.id, t.uid, t.amount.value, t.status, t.created, t.updated, t.operation, t.address, t.txid, t.NxtRsString))
              val hasMore = pager.limit == transfers.size
              Ok(ApiV2Result(data = Some(ApiV2TransfersPagingWrapper(hasMore, currency.toUpperCase, v2Transfers))).toJson)
            } else {
              Ok(defaultApiV2Result(result.code).toJson)
            }
        }
      } else {
        Future(Ok(defaultApiV2Result(ApiErrorCode.UnsupportCurrency.id).toJson))
      }
  }

  def trades(market: String) = Action.async {
    implicit request =>
      if (isAvailableMarket(market)) {
        val pager = ControllerHelper.parseApiV2NextPageParam()
        val from = if (pager.from.isDefined) pager.from.get.toLong else Long.MaxValue
        MarketService.getGlobalTransactions(Some(market), 0, pager.limit, fromTid = Some(from), needCount = false).map(
          result => {
            if (result.success) {
              val pageWrapper = result.data.get.asInstanceOf[ApiPagingWrapper]
              val txs = pageWrapper.items.asInstanceOf[Seq[ApiTransaction]]
              val apiV2Txs = txs.map { t =>
                ApiV2Transaction(t.id, t.timestamp, t.price.value, t.subjectAmount.value, t.maker, t.taker, t.sell, t.tOrder.oid, t.mOrder.oid, None)
              }
              val hasMore = pager.limit == txs.size
              val timestamp = System.currentTimeMillis
              val updated = result.copy(data = Some(ApiV2TradesPagingWrapper(hasMore, market, apiV2Txs)))
              Ok(updated.toJson)
            } else {
              Ok(result.toJson)
          }
        })
      } else {
        Future(Ok(defaultApiV2Result(ApiErrorCode.UnsupportMarket.id).toJson))
      }
  }

  def v2Depth(market: String) = Action.async {
    implicit request =>
      if (isAvailableMarket(market)) {
        val query = request.queryString
        val limit = getParam(query, "limit", "100").toInt min 200

        MarketService.getDepth(market, limit).map { result =>
          val depth = result.data.get.asInstanceOf[ApiMarketDepth]
          Ok(result.copy(data = Some(toV2MarketDepth(depth))).toJson)
        }
      } else {
        Future(Ok(defaultApiV2Result(ApiErrorCode.UnsupportMarket.id).toJson))
      }
  }

  private def toV2MarketDepth(d: ApiMarketDepth) = {
    val asks = d.asks.map(i => Seq(i.price.value, i.amount.value))
    val bids = d.bids.map(i => Seq(i.price.value, i.amount.value))
    Map("asks" -> asks, "bids" -> bids)
  }

  def kline(market: String) = Action.async {
    implicit request =>
      if (isAvailableMarket(market)) {
        val query = request.queryString
        val timeDimension = intervalToCharTimeDimension(getParam(query, "interval", "5m").toString)
        val defaultTo = System.currentTimeMillis()
        // return 90 items by default
        val defaultFrom = defaultTo - timeDimension * 90
        val fromParam = getParam(query, "start", defaultFrom.toString)
        val toParam = getParam(query, "end", defaultTo.toString)

        val to = toParam.toLong
        val from = fromParam.toLong max (to - timeDimension * 180)

        MarketService.getHistory(market, timeDimension, from, to).map { result =>
          val apiHistory = result.data.get.asInstanceOf[ApiHistory]
          val updated = result.copy(data = Some(ApiV2History(items = apiHistory.candles)))
          Ok(ApiV2Result(data = updated.data).toJson)
        }
      } else {
        Future(Ok(defaultApiV2Result(ApiErrorCode.UnsupportMarket.id).toJson))
      }
  }

  private def intervalToCharTimeDimension(interval: String): ChartTimeDimension = {
    interval match {
      case "1m" => ChartTimeDimension.OneMinute
      case "5m" => ChartTimeDimension.FiveMinutes
      case "15m" => ChartTimeDimension.FifteenMinutes
      case "30m" => ChartTimeDimension.ThirtyMinutes
      case "1h" => ChartTimeDimension.OneHour
      case "2h" => ChartTimeDimension.TwoHours
      case "4h" => ChartTimeDimension.FourHours
      case "6h" => ChartTimeDimension.SixHours
      case "1d" => ChartTimeDimension.OneDay
    }
  }

  def profile() = Authenticated.async {
    implicit request =>
      val userId = request.userId
      val apiSecretFuture = UserService.getAllApiSecrets(userId)
      val apiSecretResult = Await.result(apiSecretFuture, 5 seconds).asInstanceOf[ApiResult]
      val apiTokenPairs = if (apiSecretResult.success) {
        val apiSecrets = apiSecretResult.data.get.asInstanceOf[Seq[ApiSecret]]
        apiSecrets map { at =>
          Seq(at.identifier, Some(at.secret))
        }
      } else {
        Seq.empty
      }

      UserService.getProfileApiV2(userId) map { p =>
        if (p.success) {
          val pf = p.data.get.asInstanceOf[ApiV2Profile]
          Ok(ApiV2Result(data = Some(pf.copy(apiTokenPairs = apiTokenPairs.toSeq))).toJson)
        } else {
          Ok(defaultApiV2Result(p.code).toJson)
        }
      }
  }

  def balance() = Authenticated.async {
    implicit request =>
      val userId = request.userId
      AccountService.getAccount(userId) map {
        case result =>
          val apiUserAccount = result.data.get.asInstanceOf[ApiUserAccount]
          val balance = apiUserAccount.accounts.map(
            a => (a._1 ->
              Seq(a._2.available.value, a._2.locked.value, a._2.pendingWithdrawal.value, a._2.total.value)))
          // logger.info(s"account result: $result")
          Ok(ApiV2Result(data = Some(balance.toMap)).toJson)
      }
  }

  def userTrades() = Authenticated.async {
    implicit request =>
      val pager = ControllerHelper.parseApiV2NextPageParam()
      val from = if (pager.from.isDefined) pager.from.get.toLong else Long.MaxValue
      val query = request.queryString
      val market = getParam(query, "market")
      val marketSide = if (market.isDefined) Some(string2RichMarketSide(market.get)) else None
      val userId = request.userId
      MarketService.getTransactionsByUser(marketSide, userId, 0, pager.limit, fromTid = Some(from), needCount = false).map(
        result => {
          if (result.success) {
            val pageWrapper = result.data.get.asInstanceOf[ApiPagingWrapper]
            val txs = pageWrapper.items.asInstanceOf[Seq[ApiTransaction]]
            val apiV2Txs = txs.map { t =>
              val marketInResult = if (marketSide.isDefined) None else Some(t.subjectAmount.currency.toUpperCase + "-" + t.currencyAmount.currency.toUpperCase)
              ApiV2Transaction(t.id, t.timestamp, t.price.value, t.subjectAmount.value, t.maker, t.taker, t.sell, t.tOrder.oid, t.mOrder.oid, marketInResult)
            }
            val hasMore = pager.limit == txs.size
            val timestamp = System.currentTimeMillis
            val updated = result.copy(data = Some(ApiV2TradesPagingWrapper(hasMore, market.getOrElse(""), apiV2Txs)))
            Ok(ApiV2Result(data = updated.data).toJson)
          } else {
            Ok(defaultApiV2Result(result.code).toJson)
          }
        })
  }

  def userOrders() = Authenticated.async {
    implicit request =>
      val pager = ControllerHelper.parseApiV2NextPageParam()
      val from = if (pager.from.isDefined) pager.from.get.toLong else Long.MaxValue
      val query = request.queryString
      val market = getParam(query, "market")
      val marketSide = if (market.isDefined) Some(string2RichMarketSide(market.get)) else None
      val status = getParam(request.queryString, "order_status") match {
        case None => Nil
        case Some(s) =>
          if (s == "1") Seq(OrderStatus.PartiallyExecuted, OrderStatus.Pending)
          else if (s == "2") Seq(OrderStatus.FullyExecuted, OrderStatus.PartiallyExecutedThenCancelledByMarket)
          else if (s == "3") Seq(OrderStatus.FullyExecuted, OrderStatus.PartiallyExecutedThenCancelledByMarket,
            OrderStatus.Cancelled, OrderStatus.CancelledByMarket)
          else Nil
      }
      //TODO(xiaolu) not support ids query now.
      // val ids = getParam(query, "ids", "").split(",")
      val userId = request.userId
      AccountService.getOrders(marketSide, Some(userId), None, status, 0, pager.limit, fromOid = Some(from), needCount = false) map {
        case result: ApiResult =>
          if (result.success) {
            val apiOrders = result.data.get.asInstanceOf[ApiPagingWrapper].items.asInstanceOf[Seq[ApiOrder]]
            val apiV2Orders = apiOrders.map{o =>
	      val price = if (o.price.isDefined) o.price.get.value else 0.0
	      val amount = if (o.amount.isDefined) o.amount.get.value else 0.0
              ApiV2Order(
                o.id, o.operation.toLowerCase, o.status,
                o.subject.toUpperCase + "-" + o.currency.toUpperCase,
                price,
                amount,
                o.finishedQuantity.value,
                o.submitTime,
                None)}
            val hasMore = pager.limit == apiV2Orders.size
            Ok(ApiV2Result(data = Some(ApiV2OrderPagingWrapper(hasMore, apiV2Orders))).toJson)
          } else {
            Ok(ApiV2Result(data = result.data).toJson)
          }
      }
  }

  def depositHistory() = userTransfers(TransferType.Deposit)

  def withdrawalHistory() = userTransfers(TransferType.Withdrawal)

  private def userTransfers(ttype: TransferType) = Authenticated.async {
    implicit request =>
      val query = request.queryString
      val currency = getParam(query, "currency", "ALL")
      val userId = request.userId
      val types = Seq(ttype)
      val pager = ControllerHelper.parseApiV2NextPageParam()
      val from = if (pager.from.isDefined) pager.from.get.toLong else Long.MaxValue

      val typeList = if (types.toSet.contains(TransferType.Deposit)) types :+ TransferType.DepositHot else types

      TransferService.getTransfers(Some(userId), Currency.valueOf(currency), None, None, typeList, Cursor(0, pager.limit), fromId = Some(from), needCount = false) map {
        case result =>
          if (result.success) {
            val transfers = result.data.get.asInstanceOf[ApiPagingWrapper].items.asInstanceOf[Seq[ApiTransferItem]]
            val v2Transfers = transfers.map(t => ApiV2TransferItem(t.id, t.amount.currency.toUpperCase, t.amount.value, t.status, t.created, t.updated, t.address, t.txid))
            val hasMore = pager.limit == v2Transfers.size
            if (ttype == TransferType.Deposit)
              Ok(ApiV2Result(data = Some(ApiV2DepositsPagingWrapper(hasMore, v2Transfers))).toJson)
            else
              Ok(ApiV2Result(data = Some(ApiV2WithdrawalsPagingWrapper(hasMore, v2Transfers))).toJson)
          } else {
            Ok(defaultApiV2Result(result.code).toJson)
          }
      }
  }

  def getBatchDepositAddress() = Authenticated.async {
    implicit request =>
      val userId = request.userId
      UserService.getDepositAddress(Constant.currencySeq, userId) map {
        result =>
          Ok(ApiV2Result(data = result.data).toJson)
      }
  }

  def createDepositAddr(currency: String) = Authenticated.async {
    implicit request =>
      if (Constant.supportReserveCoins.contains(string2Currency(currency))) {
        val cur: Currency = string2Currency(currency)
        val userId = request.userId
        UserService.getDepositAddress(Seq(cur), userId) map {
          result =>
            Ok(ApiV2Result(data = result.data).toJson)
        }
      } else {
        Future(Ok(defaultApiV2Result(ApiErrorCode.UnsupportCurrency.id).toJson))
      }
  }

  def submitOrders() = Authenticated.async(parse.json) {
    implicit request =>
      val userId = request.userId
      val json = Json.parse(request.body.toString)
      val orders = (json \ "orders").as[JsArray]
      val futures = orders.value map {
        case o =>
          logger.info((o \ "market").as[String])
          val market = (o \ "market").as[String]
          val price = (o \ "price").as[Double]
          val amount = (o \ "amount").as[Double]
          val subject = market.split("-")(0);
          val currency = market.split("-")(1);
          val operation = (o \ "operation").as[String] match {
            case "buy" => Operations.Buy
            case "sell" => Operations.Sell
          }

          if ((currency.toLowerCase == "cny" && price * amount < 1) ||
            (currency.toLowerCase == "btc" && price * amount < 0.001)) {
            Future(ApiResult(false, ErrorCode.InvalidAmount.value, "amount too small"))
          } else {
            val order = UserOrder(userId.toString, operation, subject, currency, Some(price), Some(amount), None, submitTime = System.currentTimeMillis)
            AccountService.submitOrder(order)
          }
      }
      val results = futures.map { fr =>
        val finished = Await.result(fr, 5 seconds).asInstanceOf[ApiResult]
        if (finished.success) {
          val apiOrder = finished.data.get.asInstanceOf[ApiOrder]
          ApiV2SubmitOrderResult(apiOrder.id, None)
        } else {
          ApiV2SubmitOrderResult(code = Some(finished.code.toString))
        }
      }
      logger.info(results.toString)

      Future(Ok(ApiV2Result(data = Some(ApiV2SubmitOrderResults(results))).toJson))
  }

  def cancelOrders() = Authenticated.async(parse.json) {
    implicit request =>
      val userId = request.userId
      val json = Json.parse(request.body.toString)
      val orderIds = (json \ "order_ids").as[JsArray]
      var cancelledList: Seq[Long] = Seq.empty
      var failedList: Seq[Long] = Seq.empty
      orderIds.value map {
        case o =>
          val orderId = o.as[Long]
          val ofr = AccountService.getOrders(None, None, Some(orderId), Nil, 0, 1, None, false)
          val or = Await.result(ofr, 5 seconds).asInstanceOf[ApiResult]
          val uo = or.data.get.asInstanceOf[ApiPagingWrapper].items.asInstanceOf[Seq[ApiOrder]].headOption
          if (uo.isDefined) {
            val fr = AccountService.cancelOrder(orderId, userId, MarketSide(string2Currency(uo.get.subject), string2Currency(uo.get.currency)))
            val finished = Await.result(fr, 5 seconds).asInstanceOf[ApiResult]
            if (finished.success) {
              val apiOrder = finished.data.get.asInstanceOf[Order]
              cancelledList = cancelledList :+ apiOrder.id
            } else {
              logger.info(finished.toString)
              failedList = failedList :+ orderId
            }
          } else {
            failedList = failedList :+ orderId
          }
      }

      val cancelResult = ApiV2CancelOrderResult(cancelledList, failedList)
      val apiCR = ApiV2Result(data = Some(cancelResult))
      Future(Ok(apiCR.toJson))
  }

  def submitWithdrawal() = Authenticated.async(parse.json) {
    implicit request =>

      val json = Json.parse(request.body.toString)
      val userId = request.userId

      val pfFuture = UserService.getProfileApiV2(userId)
      val pfResult = Await.result(pfFuture, 5 seconds).asInstanceOf[ApiResult]

      if (pfResult.success) {
        val pf = pfResult.data.get.asInstanceOf[ApiV2Profile]
        val googleSecret = pf.googleAuthenticatorSecret.getOrElse("")
        val phoneUuid = (json \ "phoneUuid").asOpt[String].getOrElse("")
        val phoneCode = (json \ "phoneCode").asOpt[String].getOrElse("")
        val emailUuid = (json \ "emailUuid").asOpt[String].getOrElse("")
        val emailCode = (json \ "emailCode").asOpt[String].getOrElse("")
        val googleCode = (json \ "googleCode").asOpt[String].getOrElse("")
        val version = (json \ "exchangeVersion").asOpt[String]
        val lang = (json \ "lang").asOpt[String]

        val checkEmail = pf.emailAuthEnabled
        val checkPhone = pf.mobileAuthEnabled

        validateParamsAndThen(
          new CachedValueValidator(ErrorCode.InvalidEmailVerifyCode, checkEmail, emailUuid, emailCode),
          new CachedValueValidator(ErrorCode.SmsCodeNotMatch, checkPhone, phoneUuid, phoneCode),
          new GoogleAuthValidator(ErrorCode.InvalidGoogleVerifyCode, googleSecret, googleCode)) {
            popCachedValue(emailUuid, phoneUuid)
            val currency: Currency = string2Currency((json \ "currency").as[String])
            val address = (json \ "address").as[String]
            val amount = (json \ "amount").as[Double]
            val memo = (json \ "memo").asOpt[String].getOrElse("")
            val nxtPublicKey = (json \ "nxt_public_key").asOpt[String].getOrElse("")
            UserService.setWithdrawalAddress(userId, currency, address)
            AccountService.withdrawal(userId, currency, amount, address, memo, nxtPublicKey, version, lang)
          } map {
            result =>
              if (result.success) {
                val transfer = result.data.get.asInstanceOf[RequestTransferSucceeded].transfer
                Ok(ApiV2Result(data = Some(ApiV2WithdrawalResult(transfer.id, transfer.status.value))).toJson)
              } else {
                Ok(defaultApiV2Result(result.code).toJson)
              }
          }
      } else {
        Future(Ok(defaultApiV2Result(pfResult.code).toJson))
      }

  }

  def cancelWithdrawal() = Authenticated.async(parse.json) {
    implicit request =>
      val json = Json.parse(request.body.toString)
      val transferId = (json \ "transfer_id").as[Long]
      val userId = request.userId
      TransferService.apiV2CancelWithdrawal(userId, transferId) map {
        result =>
          if (result.success) {
            val errorCode = result.data.get.asInstanceOf[ErrorCode]
            Ok(ApiV2Result(code = errorCode.value, data = Some(ApiV2CancelWithdrawalResult(errorCode.toString))).toJson)
          } else {
            Ok(defaultApiV2Result(result.code).toJson)
          }
      }
  }

  def register = Action.async(parse.json) {
    implicit request =>
      logger.info(request.body.toString)
      val json = Json.parse(request.body.toString)
      val email = (json \ "email").as[String]
      val password = (json \ "pwdhash").as[String]
      val version = (json \ "exchangeVersion").asOpt[String]
      val lang = (json \ "lang").asOpt[String]
      validateParamsAndThen(
        new StringNonemptyValidator(email, password),
        new EmailFormatValidator(email),
        new PasswordFormetValidator(password)) {
          val user: User = User(id = -1, email = email, password = password)
          UserService.register(user, version, lang)
        } map {
          result =>
            if (result.success) {
              println(result)
              Ok(ApiV2Result(data = Some(ApiV2RegisterResult(result.message.toLong))).toJson)
            } else
              Ok(defaultApiV2Result(result.code).toJson)
        }
  }

  def login = Action.async {
    implicit request =>
      val apiAuthInfos = request.headers.get("Authorization").getOrElse("").split(" ")
      val authPairs = apiAuthInfos(1).split(":")
      val (email, pwd) = if (authPairs.length > 1) (authPairs(0), authPairs(1)) else (authPairs(0), "")
      val ip = request.remoteAddress
      validateParamsAndThen(
        new LoginFailedFrequencyValidator(email, ip),
        new StringNonemptyValidator(pwd),
        new EmailFormatValidator(email),
        new PasswordFormetValidator(pwd)) {
          val user: User = User(id = -1, email = email, password = pwd)
          UserService.login(user)
        } map {
          result =>
            //todo(kongliang): refactor return user profile
            if (result.success) {
              result.data.get match {
                case profile: User =>
                  val uid = profile.id.toString
                  val csrfToken = UUID.randomUUID().toString
                  UserController.cache.put("csrf-" + uid, csrfToken)
                  LoginFailedFrequencyValidator.cleanLoginFailedRecord(email, ip)

                  Ok(ApiV2Result(data = Some(ApiV2LoginResult(uid = uid.toLong, email = profile.email))).toJson).withSession(
                    "username" -> profile.email,
                    "uid" -> uid,
                    //"referralToken" -> profile.referralToken.getOrElse(0L).toString,
                    Constant.cookieNameMobileVerified -> profile.mobile.isDefined.toString,
                    Constant.cookieNameMobile -> profile.mobile.getOrElse(""),
                    Constant.cookieNameRealName -> profile.realName.getOrElse(""),
                    Constant.cookieGoogleAuthSecret -> profile.googleAuthenticatorSecret.getOrElse(""),
                    Constant.securityPreference -> profile.securityPreference.getOrElse("01"),
                    Constant.userRealName -> profile.realName2.getOrElse("")).withCookies(
                      Cookie("XSRF-TOKEN", csrfToken, None, "/", Some(".coinport.com"), false, false))
                case _ =>
                  LoginFailedFrequencyValidator.putLoginFailedRecord(email, ip)
                  Ok(defaultApiV2Result(result.code).toJson)
              }
            } else {
              val count = LoginFailedFrequencyValidator.getLoginFailedCount(email, ip)
              if (result.code != ErrorCode.LoginFailedAndLocked.value) {
                LoginFailedFrequencyValidator.putLoginFailedRecord(email, ip)
                val newRes = ApiV2Result(code = result.code, data = Some(RetryTime(4 - count)))
                Ok(newRes.toJson)
              } else {
                Ok(defaultApiV2Result(result.code).toJson)
              }
            }
        }
  }

  def logout = Action {
    implicit request =>
      Ok(ApiV2Result(data = None).toJson).withNewSession
  }

  def applyResetPwd() = Action.async(parse.json) {
    implicit request =>
      val json = Json.parse(request.body.toString)
      val email = (json \ "email").as[String]
      val version = (json \ "exchangeVersion").asOpt[String]
      val lang = (json \ "lang").asOpt[String]
      validateParamsAndThen(
        new EmailFormatValidator(email)) {
          UserService.requestPasswordReset(email, version, lang)
        } map { result => Ok(ApiV2Result(code = result.code, data = Some(SimpleBooleanResult(result.success))).toJson) }
  }

  //not used in api v2 now.
  def verifyPwdResetToken() = Action.async {
    implicit request =>
      val query = request.queryString
      val token = getParam(query, "token", "")
      UserService.validatePasswordResetToken(token) map {
        result => Ok(ApiV2Result(code = result.code, data = Some(SimpleBooleanResult(result.success))).toJson)
      }
  }

  def resetPwd() = Action.async(parse.json) {
    implicit request =>
      val json = Json.parse(request.body.toString)
      val newPassword = (json \ "pwdHash").as[String]
      val token = (json \ "token").as[String]

      UserService.validatePasswordResetToken(token) map {
        result => {
          if (result.success) {
            val rpFuture = UserService.resetPassword(newPassword, token)
            val rpResult = Await.result(rpFuture, 5 seconds).asInstanceOf[ApiResult]
            Ok(ApiV2Result(data = Some(SimpleBooleanResult(rpResult.success))).toJson)
          } else
            Ok(ApiV2Result(code = 1005, data = Some(SimpleBooleanResult(result.success))).toJson)
        }
      }
  }

  def sendVerifyEmail() = Action.async(parse.json) {
    implicit request =>
      val json = Json.parse(request.body.toString)
      val email = (json \ "email").as[String]
      val version = (json \ "exchangeVersion").asOpt[String]
      val lang = (json \ "lang").asOpt[String]
      validateParamsAndThen(
        new EmailFormatValidator(email)) {
          UserService.resendVerifyEmail(email, version, lang)
        } map { result => Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson) }
  }

  def verifyActivationCode() = Action.async {
    implicit request =>
      val query = request.queryString
      val token = getParam(query, "token", "")
      UserService.verifyEmail(token) map {
        result => Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson)
      }
  }

  def verifyRealName() = Authenticated.async(parse.json) {
    implicit request =>
      val json = Json.parse(request.body.toString)
      val userId = request.userId
      val realName = (json \ "realName").asOpt[String].getOrElse("")
      val location = (json \ "location").asOpt[String].getOrElse("")
      val identiType = (json \ "identiType").asOpt[String].getOrElse("")
      val idNumber = (json \ "idNumber").asOpt[String].getOrElse("")
      UserService.verifyRealName(userId, realName, location, identiType, idNumber).map {
        result =>
          if (result.success) {
            val newSession = request.session + (Constant.userRealName -> realName)
            Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson).withSession(newSession)
          } else {
            Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson)
          }
      }
  }

  def doChangePassword() = Action.async(parse.json) {
    implicit request =>
      val data = request.body
      val json = Json.parse(request.body.toString)
      val email = (json \ "email").asOpt[String].getOrElse("")
      val oldPassword = (json \ "oldPassword").asOpt[String].getOrElse("")
      val newPassword = (json \ "newPassword").asOpt[String].getOrElse("")
      UserService.changePassword(email, oldPassword, newPassword) map {
        result =>
          Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson)
      }
  }

  def updateNickName() = Authenticated.async(parse.json) {
    implicit request =>
      val json = Json.parse(request.body.toString)
      val userId = request.userId
      val nickname = (json \ "nickname").asOpt[String].getOrElse("")
      val cleanNickName = nickname.replaceAll("[^a-zA-Z0-9.]", "")
      UserService.updateNickName(userId, cleanNickName).map {
        result =>
          if (result.success) {
            val newSession = request.session + (Constant.cookieNameRealName -> cleanNickName)
            Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson).withSession(newSession)
          } else {
            Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson)
          }
    }
  }

  def setPreferenceEmail = Authenticated.async(parse.json) {
    implicit request =>
      val json = Json.parse(request.body.toString)
      val uuid = (json \ "uuid").asOpt[String].getOrElse("")
      val userId = request.userId
      val emailCode = (json \ "emailCode").asOpt[String].getOrElse("")
      val emailPrefer = (json \ "emailPrefer").asOpt[String].getOrElse("1")

      val pfFuture = UserService.getProfileApiV2(userId)
      val pfResult = Await.result(pfFuture, 5 seconds).asInstanceOf[ApiResult]

      if (pfResult.success) {
        val pf = pfResult.data.get.asInstanceOf[ApiV2Profile]
        val securityPreference = (pf.emailAuthEnabled, pf.mobileAuthEnabled) match {
          case (true, true) => Some("11")
          case (false, true) => Some("10")
          case (true, false) => Some("01")
          case _ => Some("01")
        }
        val prefer = SecurityPreferenceUtil.updateEmailVerification(securityPreference, emailPrefer)
        logger.info(s"emailCode: $emailCode, emailPrefer: $emailPrefer, prefer: $prefer, uuid= $uuid")

        validateParamsAndThen(
          new CachedValueValidator(ErrorCode.InvalidEmailVerifyCode, true, uuid, emailCode)) {
            popCachedValue(uuid)
            UserService.setUserSecurityPreference(userId.toLong, prefer)
          } map {
            result =>
              if (result.success) {
                val newSession = request.session + (Constant.securityPreference -> prefer)
                Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson).withSession(newSession)
              } else {
                Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson)
              }
          }
      } else {
        Future(Ok(defaultApiV2Result(pfResult.code).toJson))
      }
  }

  def setPreferencePhone = Authenticated.async(parse.json) {
    implicit request =>
      val json = Json.parse(request.body.toString)
      val uuid = (json \ "uuid").asOpt[String].getOrElse("")
      val userId = request.userId
      val phoneCode = (json \ "phoneCode").asOpt[String].getOrElse("")
      val phonePrefer = (json \ "phonePrefer").asOpt[String].getOrElse("1")

      val pfFuture = UserService.getProfileApiV2(userId)
      val pfResult = Await.result(pfFuture, 5 seconds).asInstanceOf[ApiResult]

      if (pfResult.success) {
        val pf = pfResult.data.get.asInstanceOf[ApiV2Profile]
        val securityPreference = (pf.emailAuthEnabled, pf.mobileAuthEnabled) match {
          case (true, true) => Some("11")
          case (false, true) => Some("10")
          case (true, false) => Some("01")
          case _ => Some("01")
        }
        val prefer = SecurityPreferenceUtil.updateMobileVerification(securityPreference, phonePrefer)
        logger.info(s"phoneCode: $phoneCode, phonePrefer: $phonePrefer, prefer: $prefer, uuid= $uuid")

        validateParamsAndThen(
          new CachedValueValidator(ErrorCode.SmsCodeNotMatch, true, uuid, phoneCode)) {
            popCachedValue(uuid)
            UserService.setUserSecurityPreference(userId.toLong, prefer)
          } map {
            result =>
              if (result.success) {
                val newSession = request.session + (Constant.securityPreference -> prefer)
                Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson).withSession(newSession)
              } else {
                Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson)
              }
          }
      } else {
        Future(Ok(defaultApiV2Result(pfResult.code).toJson))
      }
  }

  def doBindOrUpdateMobile = Authenticated.async(parse.json) {
    implicit request =>
      val json = Json.parse(request.body.toString)
      val userId = request.userId
      val pfFuture = UserService.getProfileApiV2(userId)
      val pfResult = Await.result(pfFuture, 5 seconds).asInstanceOf[ApiResult]

      if (pfResult.success) {
        val pf = pfResult.data.get.asInstanceOf[ApiV2Profile]
        val email = pf.email
        val oldMobile = pf.mobile.getOrElse("")

        val newMobile = (json \ "mobile").asOpt[String].getOrElse("")
        val uuidOld = (json \ "verifyCodeUuidOld").asOpt[String].getOrElse("")
        val verifyCodeOld = (json \ "verifyCodeOld").asOpt[String].getOrElse("")
        val uuid = (json \ "verifyCodeUuid").asOpt[String].getOrElse("")
        val verifyCode = (json \ "verifyCode").asOpt[String].getOrElse("")

        logger.info(s"doBindOrUpdateMobile: mobileOld: $oldMobile, newMobile: $newMobile, uuid: $uuid, verifycode: $verifyCode")

        val needCheckOld = oldMobile.trim.nonEmpty
        validateParamsAndThen(
          new CachedValueValidator(ErrorCode.SmsCodeNotMatch, needCheckOld, uuidOld, verifyCodeOld),
          new CachedValueValidator(ErrorCode.SmsCodeNotMatch, true, uuid, verifyCode),
          new StringNonemptyValidator(userId.toString, email, newMobile)) {
            popCachedValue(uuidOld, uuid)
            UserService.bindOrUpdateMobile(email, newMobile)
          } map {
            result =>
              if (result.success) {
                val newSession = request.session + (Constant.cookieNameMobile -> newMobile) + (Constant.cookieNameMobileVerified -> "true")
                Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson).withSession(newSession)
              } else {
                Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson)
              }
          }
      } else {
        Future(Ok(defaultApiV2Result(pfResult.code).toJson))
      }
  }

  def bindGoogleAuth = Authenticated.async(parse.json) {
    implicit request =>
      val json = Json.parse(request.body.toString)
      val userId = request.userId
      val googleCode = (json \ "googlecode").asOpt[String].getOrElse("")
      val googleSecret = (json \ "googlesecret").asOpt[String].getOrElse("")

      validateParamsAndThen(
        new GoogleAuthValidator(ErrorCode.InvalidGoogleVerifyCode, googleSecret, googleCode)) {
          UserService.bindGoogleAuth(userId, googleSecret)
        } map {
          result =>
            if (result.success) {
              val newSession = request.session + (Constant.cookieGoogleAuthSecret -> googleSecret)
              Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson).withSession(newSession)
            } else {
              Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson)
            }
        }
  }

  def unbindGoogleAuth = Authenticated.async(parse.json) {
    implicit request =>
      val json = Json.parse(request.body.toString)
      val userId = request.userId
      val pfFuture = UserService.getProfileApiV2(userId)
      val pfResult = Await.result(pfFuture, 5 seconds).asInstanceOf[ApiResult]

      if (pfResult.success) {
        val pf = pfResult.data.get.asInstanceOf[ApiV2Profile]
        val googleSecret = pf.googleAuthenticatorSecret.getOrElse("")
        val googleCode = (json \ "googlecode").asOpt[String].getOrElse("")

        validateParamsAndThen(
          new GoogleAuthValidator(ErrorCode.InvalidGoogleVerifyCode, googleSecret, googleCode)) {
            UserService.unbindGoogleAuth(userId)
          } map {
            result =>
              if (result.success) {
                val newSession = request.session - Constant.cookieGoogleAuthSecret
                Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson).withSession(newSession)
              } else {
                Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson)
              }
          }
      } else {
        Future(Ok(defaultApiV2Result(pfResult.code).toJson))
      }
  }

  def getGoogleAuth = Authenticated {
    implicit request =>
      val uid = request.userId
      val pfFuture = UserService.getProfileApiV2(uid)
      val pfResult = Await.result(pfFuture, 5 seconds).asInstanceOf[ApiResult]

      if (pfResult.success) {
        val pf = pfResult.data.get.asInstanceOf[ApiV2Profile]
        var secret = pf.googleAuthenticatorSecret.getOrElse("")
        if (secret.isEmpty) {
          val email = pf.email
          val googleAuthenticator = new GoogleAuthenticator()
          val key = googleAuthenticator.createCredentials(uid + "//" + email)
          secret = key.getKey()
        }
        val url = GoogleAuthenticatorKey.getQRBarcodeURL("COINPORT", uid.toString, secret)
        Ok(ApiV2Result(data = Some(Map("authUrl" -> url, "secret" -> secret))).toJson())
      } else {
        Ok(defaultApiV2Result(pfResult.code).toJson)
      }
  }

  def addBankCard = Authenticated.async(parse.json) {
    implicit request =>
      val userId = request.userId
      val json = Json.parse(request.body.toString)

      val bankName = (json \ "bankName").asOpt[String].getOrElse("")
      val ownerName = (json \ "U_RN").asOpt[String].getOrElse("")
      val cardNumber = (json \ "cardNumber").asOpt[String].getOrElse("")
      val branchBankName = (json \ "branchBankName").asOpt[String].getOrElse("")
      val emailUuid = (json \ "emailUuid").asOpt[String].getOrElse("")
      val emailCode = (json \ "emailCode").asOpt[String].getOrElse("")

      validateParamsAndThen(
        new StringNonemptyValidator(bankName, ownerName, cardNumber, emailCode, emailUuid),
        new CachedValueValidator(ErrorCode.InvalidEmailVerifyCode, true, emailUuid, emailCode)) {
          popCachedValue(emailUuid)
          UserService.addBankCard(userId, bankName, ownerName, cardNumber, branchBankName)
        } map {
          result =>
            Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson)
        }
  }

  def deleteBankCard = Authenticated.async(parse.json) {
    implicit request =>
      val uid = request.userId
      val json = Json.parse(request.body.toString)
      val cardNumber = (json \ "cardNumber").asOpt[String].getOrElse("")
      UserService.deleteBankCard(uid, cardNumber) map {
        result =>
          Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson)
      }
  }

  def queryBankCards = Authenticated.async {
    implicit request =>
      val uid = request.userId
      UserService.queryBankCards(uid) map {
        result =>
          if (result.success) {
            val cards = result.data.get.asInstanceOf[Seq[BankCard]]
            val apiV2Cards = cards map {c => ApiV2BankCard(c.bankName, c.ownerName, c.cardNumber, c.branchBankName)}
            Ok(ApiV2Result(data = Some(apiV2Cards)).toJson)
          } else
            Ok(defaultApiV2Result(result.code).toJson)
      }
  }

  def addApiToken = Authenticated.async(parse.json) {
    implicit request =>
      val userId = request.userId
      val json = Json.parse(request.body.toString)

      UserService.generateApiSecret(userId) map {
        result =>
          if (result.success) {
            val apiSecret = result.data.get.asInstanceOf[ApiSecret]
            val newSecret = ApiV2ApiTokenResult(apiSecret.identifier.get, Some(apiSecret.secret))
            Ok(ApiV2Result(code = result.code, data = Some(newSecret)).toJson)
          } else {
            Ok(defaultApiV2Result(result.code).toJson)
          }
      }
  }

  def deleteApiToken = Authenticated.async(parse.json) {
    implicit request =>
      val userId = request.userId
      val json = Json.parse(request.body.toString)
      val token = (json \ "token").asOpt[String].getOrElse("")

      validateParamsAndThen(
        new StringNonemptyValidator(token)) {
          UserService.deleteApiSecret(userId, token)
        } map {
          result =>
            if (result.success) {
              val apiSecret = result.data.get.asInstanceOf[ApiSecret]
              val newSecret = ApiV2ApiTokenResult(apiSecret.identifier.get, None)
              Ok(ApiV2Result(code = result.code, data = Some(newSecret)).toJson)
            } else {
              Ok(defaultApiV2Result(result.code).toJson)
            }
        }
  }

  private def defaultApiV2Result(code: Int) = ApiV2Result(code, System.currentTimeMillis, None)

  def string2Currency(currency: String) : Currency = {
    if (currency == null || currency.isEmpty)
      Currency.Unknown
    else
      Currency.valueOf(currency.toLowerCase.capitalize).getOrElse(Currency.Unknown)
  }

  def isAvailableMarket(market: String): Boolean = {
    val m = string2RichMarketSide(market)
    m.outCurrency != Currency.Unknown && m.inCurrency != Currency.Unknown
  }
}
