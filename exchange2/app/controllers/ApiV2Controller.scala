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
import controllers.GoogleAuth.GoogleAuthenticator
import models._
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

  def ticker(market: String) = {
    val out = market.split("-")(0)
    val in = market.split("-")(1)
    tickerBasic(Seq(string2RichMarketSide(market)))
  }

  private def tickerBasic(sides: Seq[MarketSide]) = Action.async {
    implicit request =>
      val r = MarketService.getJsonTickers(sides)
      r.map(result => Ok(ApiV2Result(data = result.data).toJson))
  }

  def reserves() = Action.async {
    implicit request =>
      val reserves = Constant.supportReserveCoins map {
        case c =>
          val reserveFuture = OpenService.getCurrencyReserve(c)
          val reserveApiResult = Await.result(reserveFuture, 5 seconds).asInstanceOf[ApiResult]
          if (reserveApiResult.success) {
            val reserve = reserveApiResult.data.get.asInstanceOf[ApiCurrencyReserve]
            c.toString.toUpperCase -> Seq(reserve.hot.value, reserve.cold.value, reserve.user.value, reserve.total.value)
          } else {
            c.toString.toUpperCase -> Seq.empty
          }
      }
      val result = ApiV2Result(data = Some(reserves.toMap))
      Future(Ok(result.toJson))
  }

  def reserve(currency: String) = Action.async {
    implicit request =>
      BitwayService.getReserve(currency).map(result =>
        Ok(ApiV2Result(data = result.data).toJson))
  }

  def balanceSnapshotFiles(currency: String) = Action {
    implicit request =>
      val pager = ControllerHelper.parseApiV2NextPageParam()
      val path = "csv/asset/" + currency.toLowerCase
      val files = HdfsAccess.listFiles(path)
        .sortWith((a, b) => a.updated > b.updated)

      val from = if (pager.from.isDefined) pager.from.get.toLong else 1400000000000L * 1000 // max timestamp
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
  }

  def transfers(currency: String) = Action.async {
    implicit request =>
      val query = request.queryString
      val status = getParam(query, "status").map(s => TransferStatus.get(s.toInt).getOrElse(TransferStatus.Succeeded))
      val types = getParam(query, "type").map(s => TransferType.get(s.toInt).getOrElse(TransferType.Deposit)) match {
        case Some(t) => Seq(t)
        case None => Seq(TransferType.Deposit, TransferType.Withdrawal)
      }
      val pager = ControllerHelper.parseApiV2PagingParam()

      val typeList = if (types.toSet.contains(TransferType.Deposit)) types :+ TransferType.DepositHot else types

      TransferService.getTransfers(None, Currency.valueOf(currency), None, None, typeList, Cursor(pager.skip, pager.limit)) map {
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
  }

  def trades(market: String) = Action.async {
    implicit request =>
      val pager = ControllerHelper.parseApiV2PagingParam()
      MarketService.getGlobalTransactions(Some(market), pager.skip, pager.limit).map(
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
  }

  def v2Depth(market: String) = Action.async {
    implicit request =>
      val query = request.queryString
      val limit = getParam(query, "limit", "100").toInt min 200

      MarketService.getDepth(market, limit).map { result =>
        val depth = result.data.get.asInstanceOf[ApiMarketDepth]
        Ok(result.copy(data = Some(toV2MarketDepth(depth))).toJson)
      }
  }

  private def toV2MarketDepth(d: ApiMarketDepth) = {
    val asks = d.asks.map(i => Seq(i.price.value, i.amount.value))
    val bids = d.bids.map(i => Seq(i.price.value, i.amount.value))
    Map("asks" -> asks, "bids" -> bids)
  }

  def kline(market: String) = Action.async {
    implicit request =>
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
      val pager = ControllerHelper.parseApiV2PagingParam()
      val query = request.queryString
      val market = getParam(query, "market")
      val marketSide = if (market.isDefined) Some(string2RichMarketSide(market.get)) else None
      val userId = request.userId
      MarketService.getTransactionsByUser(marketSide, userId, pager.skip, pager.limit).map(
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
      val pager = ControllerHelper.parseApiV2PagingParam()
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
      AccountService.getOrders(marketSide, Some(userId), None, status, pager.skip, pager.limit) map {
        case result: ApiResult =>
          if (result.success) {
            val apiOrders = result.data.get.asInstanceOf[ApiPagingWrapper].items.asInstanceOf[Seq[ApiOrder]]
            val apiV2Orders = apiOrders.map(o => ApiV2Order(
              o.id, o.operation.toLowerCase, o.status,
              o.subject.toUpperCase + "-" + o.currency.toUpperCase,
              o.price.get.value,
              o.amount.get.value,
              o.finishedQuantity.value,
              o.submitTime,
              None))
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
      val pager = ControllerHelper.parseApiV2PagingParam()

      val typeList = if (types.toSet.contains(TransferType.Deposit)) types :+ TransferType.DepositHot else types

      TransferService.getTransfers(Some(userId), Currency.valueOf(currency), None, None, typeList, Cursor(pager.skip, pager.limit)) map {
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
      val cur: Currency = currency
      val userId = request.userId
      UserService.getDepositAddress(Seq(cur), userId) map {
        result =>
          Ok(ApiV2Result(data = result.data).toJson)
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
          val ofr = AccountService.getOrders(None, None, Some(orderId), Nil, 0, 1)
          val or = Await.result(ofr, 5 seconds).asInstanceOf[ApiResult]
          val uo = or.data.get.asInstanceOf[ApiPagingWrapper].items.asInstanceOf[Seq[ApiOrder]].headOption
          if (uo.isDefined) {
            val fr = AccountService.cancelOrder(orderId, userId, MarketSide(uo.get.subject, uo.get.currency))
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

        val checkEmail = pf.emailAuthEnabled
        val checkPhone = pf.mobileAuthEnabled

        validateParamsAndThen(
          new CachedValueValidator(ErrorCode.InvalidEmailVerifyCode, checkEmail, emailUuid, emailCode),
          new CachedValueValidator(ErrorCode.SmsCodeNotMatch, checkPhone, phoneUuid, phoneCode),
          new GoogleAuthValidator(ErrorCode.InvalidGoogleVerifyCode, googleSecret, googleCode)) {
            popCachedValue(emailUuid, phoneUuid)
            val currency: Currency = (json \ "currency").as[String]
            val address = (json \ "address").as[String]
            val amount = (json \ "amount").as[Double]
            val memo = (json \ "memo").asOpt[String].getOrElse("")
            val nxtPublicKey = (json \ "nxt_public_key").asOpt[String].getOrElse("")
            UserService.setWithdrawalAddress(userId, currency, address)
            AccountService.withdrawal(userId, currency, amount, address, memo, nxtPublicKey)
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
            Ok(ApiV2Result(data = Some(ApiV2CancelWithdrawalResult(errorCode.toString))).toJson)
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
      validateParamsAndThen(
        new StringNonemptyValidator(email, password),
        new EmailFormatValidator(email),
        new PasswordFormetValidator(password)) {
          val user: User = User(id = -1, email = email, password = password)
          UserService.register(user)
        } map {
          result =>
            if (result.success) {
              println(result)
              Ok(ApiV2Result(data = Some(ApiV2RegisterResult(result.message.toLong))).toJson)
            } else
              Ok(defaultApiV2Result(result.code).toJson)
        }
  }

  def login = Authenticated.async {
    implicit request =>
      val apiAuthInfos = request.headers.get("Authorization").getOrElse("").split(" ")
      val authPairs = apiAuthInfos(1)
      val tokenArr = new java.lang.String(BaseEncoding.base64.decode(authPairs)).split(":")
      val (email, pwd) = (tokenArr(0), tokenArr(1))
      val password = MHash.sha256Base64(pwd)
      val ip = request.remoteAddress
      validateParamsAndThen(
        new StringNonemptyValidator(password),
        new EmailFormatValidator(email),
        new PasswordFormetValidator(password),
        new LoginFailedFrequencyValidator(email, ip)) {
          val user: User = User(id = -1, email = email, password = password)
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
                val newRes = ApiV2Result(data = Some(4 - count))
                Ok(newRes.toJson)
              } else {
                Ok(result.toJson)
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
      validateParamsAndThen(
        new EmailFormatValidator(email)) {
          UserService.requestPasswordReset(email)
        } map { result => Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson) }
  }

  def verifyPwdResetToken() = Action.async {
    implicit request =>
      val query = request.queryString
      val token = getParam(query, "token", "")
      UserService.validatePasswordResetToken(token) map {
        result => Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson)
      }
  }

  def resetPwd() = Action.async(parse.json) {
    implicit request =>
      val json = Json.parse(request.body.toString)
      val newPassword = (json \ "pwdHash").as[String]
      val token = (json \ "token").as[String]
      UserService.resetPassword(newPassword, token) map {
        result => Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson)
      }
  }

  def sendVerifyEmail() = Action.async(parse.json) {
    implicit request =>
      val json = Json.parse(request.body.toString)
      val email = (json \ "email").as[String]
      validateParamsAndThen(
        new EmailFormatValidator(email)) {
          UserService.resendVerifyEmail(email)
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
        result => Ok(ApiV2Result(data = Some(SimpleBooleanResult(result.success))).toJson)
      }
  }

  private def defaultApiV2Result(code: Int) = ApiV2Result(code, System.currentTimeMillis, None)
}
