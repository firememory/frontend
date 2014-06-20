/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package controllers

import play.api.mvc._
import play.api.libs.iteratee.Enumerator
import scala.concurrent.ExecutionContext.Implicits.global
import com.coinport.coinex.api.model._
import com.github.tototoshi.play2.json4s.native.Json4s
import utils.HdfsAccess
import com.coinport.coinex.api.service.NotificationService
import models.PagingWrapper
import com.coinport.coinex.data.Language

object MainController extends Controller with Json4s {
  def backdoor = Action {
    implicit request =>
      Redirect(routes.MainController.index())
        .withSession("uid" -> "1001", "username" -> "developer@coinport.com")
  }

  def index = Action {
    implicit request =>
      Ok(views.html.index.render(request.session, request.acceptLanguages(0)))
  }

  def trade = Action {
    implicit request =>
      Ok(views.html.trade.render(request.session, request.acceptLanguages(0)))
  }

  def account() = Authenticated {
    implicit request =>
      Ok(views.html.account.render(request.session, request.acceptLanguages(0)))
  }

  def market = Action {
    implicit request =>
      Ok(views.html.market.render(request.session, request.acceptLanguages(0)))
  }

  def user(uid: String) = Action {
    implicit request =>
      Ok(views.html.user.render(uid, request.session, request.acceptLanguages(0)))
  }

  def order(oid: String) = Action {
    implicit request =>
      Ok(views.html.order.render(oid, request.session, request.acceptLanguages(0)))
  }

  def transaction(tid: String) = Action {
    implicit request =>
      Ok(views.html.transaction.render(tid, request.session, request.acceptLanguages(0)))
  }

  def transactions(market: String) = Action {
    implicit request =>
      Ok(views.html.transactions.render(market, request.session, request.acceptLanguages(0)))
  }

  def coin(coin: String) = Action {
    implicit request =>
      Ok(views.html.coin.render(coin, request.session, request.acceptLanguages(0)))
  }

  def depth(market: String) = Action {
    implicit request =>
      Ok(views.html.depth.render(market, request.session, request.acceptLanguages(0)))
  }

  def login(msg: String = "") = Action {
    implicit request =>
      Ok(views.html.login.render(msg, request.session, request.acceptLanguages(0))).withNewSession
  }

  def register(email: String = "") = Action {
    implicit request =>
      Ok(views.html.register.render(email, request.session, request.acceptLanguages(0)))
  }

  def inviteCode(msg: String = "") = Action {
    implicit request =>
      Ok(views.html.inviteCode.render(msg, request.session, request.acceptLanguages(0)))
  }

  def open = Action {
    implicit request =>
      Ok(views.html.open.render(request.session, request.acceptLanguages(0)))
  }

  def prompt(msgKey: String) = Action {
    implicit request =>
    Ok(views.html.prompt.render(msgKey, request.session, request.acceptLanguages(0)))
  }

  def company = Action {
    implicit request =>
      Ok(views.html.company.render(request.session, request.acceptLanguages(0)))
  }
  def downloadFromHdfs(path: String, filename: String) = Action {
    val stream = HdfsAccess.getFileStream(path, filename)
    val fileContent: Enumerator[Array[Byte]] = Enumerator.fromStream(stream)
      .onDoneEnumerating {
      stream.close()
    }

    Result(
      header = ResponseHeader(200),
      body = fileContent
    ).withHeaders("Content-type" -> "application/force-download", "Content-Disposition" -> "attachment")
  }

  def listFilesFromHdfs(path: String) = Action {
    implicit request =>
      val pager = ControllerHelper.parsePagingParam()
      val files = HdfsAccess.listFiles(path)
        .sortWith((a, b) => a.updated > b.updated)

      val from = Math.min(pager.skip, files.length - 1)
      val until = pager.skip + pager.limit

      val items = files.slice(from, until)

      val data = PagingWrapper(
        count = files.length,
        skip = pager.skip,
        limit = pager.limit,
        currentPage = pager.page,
        pageSize = pager.limit,
        items = items)

      val result = ApiResult(data = Some(data))

      Ok(result.toJson)
  }

  def getNotifications() = Action.async {
    implicit  request =>
      val lang: Language =
        if (request.acceptLanguages(0).language.contains("en")) Language.English
        else Language.Chinese

      NotificationService.getNotifications(lang) map {
        case result =>
        Ok(result.toJson)
      }
  }

  def bidaskView() = Action {
    implicit request =>
      Ok(views.html.viewBidask.render(request.session, request.acceptLanguages(0)))
  }

  def registerView() = Action {
    implicit request =>
      Ok(views.html.viewRegister.render(request.acceptLanguages(0)))
  }

  def assetView() = Action {
    implicit request =>
      Ok(views.html.viewAsset.render(request.acceptLanguages(0)))
  }

  def transferView() = Action {
    implicit request =>
      Ok(views.html.viewTransfer.render(request.acceptLanguages(0)))
  }

  def depositView() = Action {
    implicit request =>
      Ok(views.html.viewDeposit.render(request.acceptLanguages(0)))
  }

  def depositDebugView() = Action {
    implicit request =>
      Ok(views.html.viewDepositDebug.render(request.acceptLanguages(0)))
  }

  def withdrawalView() = Action {
    implicit request =>
      Ok(views.html.viewWithdrawal.render(request.acceptLanguages(0)))
  }

  def ordersView() = Action {
    implicit request =>
      Ok(views.html.viewOrders.render(request.acceptLanguages(0)))
  }

  def transactionsView() = Action {
    implicit request =>
      Ok(views.html.viewTransactions.render(request.acceptLanguages(0)))
  }

  def opendataView() = Action {
    implicit request =>
      Ok(views.html.viewOpendata.render(request.acceptLanguages(0)))
  }

  def reserveView() = Action {
    implicit request =>
      Ok(views.html.viewReserve.render(request.acceptLanguages(0)))
  }

  def opensourceView() = Action {
    implicit request =>
      Ok(views.html.viewOpensource.render(request.acceptLanguages(0)))
  }

  def connectivityView() = Action {
    implicit request =>
      Ok(views.html.viewConnectivity.render(request.acceptLanguages(0)))
  }

  def userAgreement() = Action {
    implicit request =>
      Ok(views.html.userAgreement.render(request.session, request.acceptLanguages(0)))
  }
}
