/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package controllers

import play.api.mvc._
import play.api.i18n.Lang
import play.api.data._
import play.api.Play
import play.api.Logger
import play.api.Play.current
import play.api.libs.iteratee.Enumerator
import scala.concurrent.ExecutionContext.Implicits.global
import com.coinport.coinex.api.model._
import com.github.tototoshi.play2.json4s.native.Json4s
import utils.HdfsAccess
import com.coinport.coinex.api.service._
import models.PagingWrapper
import com.coinport.coinex.data.Language
import com.coinport.coinex.data.Currency
import utils.Constant
import ControllerHelper._
import com.coinport.coinex.data
import com.coinport.coinex.data.Language.Chinese
import controllers.GoogleAuth.{CredentialRepositoryMock, GoogleAuthenticatorKey, GoogleAuthenticator}
import scala.concurrent.Future

object MainController extends Controller with Json4s {
  val logger = Logger(this.getClass)
  // def backdoor = Action {
  //   implicit request =>
  //     Redirect(routes.MainController.index())
  //       .withSession("uid" -> "1001", "username" -> "developer@coinport.com")
  // }
  val supportedLocales = List("en-US", "zh-CN")

  def changeLocale(locale: String) = Action {
    implicit request =>
      val referrer = request.headers.get(REFERER).getOrElse("/")
      if (supportedLocales.contains(locale)) {
        implicit val lang = Lang(locale)
        Redirect(referrer).withLang(lang)
      } else {
        BadRequest(referrer)
      }
  }

  def index = Action {
    implicit request =>
          Ok(views.html.index.render(request.session, langFromRequestCookie(request)))
  }

  def trade = Action {
    implicit request =>
      Ok(views.html.trade.render(request.session, langFromRequestCookie(request)))
  }

  def account() = AuthenticatedOrRedirect.async {
    implicit request =>
      val uid = request.session.get("uid").getOrElse("")
      AccountService.getAccount(uid.toLong) map {
        case result =>
          // val assetsTotalMap = result.data.map(_.asInstanceOf[ApiUserAccount].accounts.map(kv => (kv._1, kv._2.total.display))).getOrElse(Map.empty[String, String])
          val assetsCurrencies = result.data.map(_.asInstanceOf[ApiUserAccount].accounts.map(kv => kv._1)).getOrElse(Seq.empty[String]).toSeq
          Ok(views.html.account_asset.render(assetsCurrencies, request.session, langFromRequestCookie(request)))
      }
  }

  // def assets() = Action {
  //   implicit request =>
  //     Ok(views.html.account_asset.render(Seq(""), request.session))
  // }

  def transfer() = Action {
    implicit request =>
      Ok(views.html.account_transfer.render(request.session, langFromRequestCookie(request)))
  }

  def orders() = Action {
    implicit request =>
      Ok(views.html.account_orders.render(request.session, langFromRequestCookie(request)))
  }

  def transaction() = Action {
    implicit request =>
      Ok(views.html.account_transaction.render(request.session, langFromRequestCookie(request)))
  }

  def profile() = Action.async {
    implicit request =>
    UserService.getApiSecret(request.session.get("uid").get.toLong) map {
      result =>
      if (result.success) {
        val apiToken = result.data.getOrElse("").asInstanceOf[String]
        Ok(views.html.account_profiles.render(apiToken, request.session, langFromRequestCookie(request)))
      } else {
        Ok(views.html.account_profiles.render("", request.session, langFromRequestCookie(request)))
      }
    }
  }

  def settings() = Action {
    implicit request =>
      Ok(views.html.account_settings.render(request.session, langFromRequestCookie(request)))
  }

  def googleAuth() = Action {
    implicit request =>
      Ok(views.html.account_googleauth.render(request.session, langFromRequestCookie(request)))
  }


  def market = Action {
   implicit request =>
     Ok(views.html.market.render(request.session, langFromRequestCookie(request)))
  }

  def user(uid: String) = Action {
    implicit request =>
      Ok(views.html.user.render(uid, request.session, langFromRequestCookie(request)))
  }

  def order(oid: String) = Action {
    implicit request =>
      Ok(views.html.order.render(oid, request.session, langFromRequestCookie(request)))
  }

  def marketOrders(market: String) = Action {
    implicit request =>
      Ok(views.html.orders.render(market, request.session, langFromRequestCookie(request)))
  }

  def transactionView(tid: String) = Action {
    implicit request =>
      Ok(views.html.transaction.render(tid, request.session, langFromRequestCookie(request)))
  }

  def transactions(market: String) = Action {
    implicit request =>
      Ok(views.html.transactions.render(market, request.session, langFromRequestCookie(request)))
  }

  def coin(coin: String) = Action {
    implicit request =>
      Ok(views.html.coin.render(coin, request.session, langFromRequestCookie(request)))
  }

  def depth(market: String) = Action {
    implicit request =>
      Ok(views.html.depth.render(market, request.session, langFromRequestCookie(request)))
  }

  def login(msg: String = "") = Action {
    implicit request =>
    val regex = """^[-0-9a-zA-Z._]+$"""
    val safeMsg = if (msg.matches(regex)) msg else ""
    Ok(views.html.login.render(safeMsg, request.flash, request.session, langFromRequestCookie(request))).withNewSession
  }

  def register() = Action {
    implicit request =>
    Ok(views.html.register.render(request.session, langFromRequestCookie(request))).withNewSession
  }

  // def inviteCode(msg: String = "")(implicit lang: Lang) = Action {
  //   implicit request =>
  //     Ok(views.html.inviteCode.render(msg, request.session, langFromRequestCookie(request)))
  // }

   def open = Action {
     implicit request =>
       Ok(views.html.open.render(request.session, langFromRequestCookie(request)))
   }

  def prompt(msgKey: String) = Action {
    implicit request =>
      val regex = """^[-0-9a-zA-Z._]+$"""
      val safeMsg = if (msgKey.matches(regex)) msgKey else ""
      Ok(views.html.prompt.render(safeMsg, request.session, langFromRequestCookie(request)))
  }

  // def company = Action {
  //   implicit request =>
  //     Ok(views.html.company.render(request.session, langFromRequestCookie(request)))
  // }

  def downloadFromHdfs(path: String, filename: String) = Action {
    try {
      val stream = HdfsAccess.getFileStream(path, filename)
      val fileContent: Enumerator[Array[Byte]] = Enumerator.fromStream(stream)
        .onDoneEnumerating {
        stream.close()
      }

      Result(
        header = ResponseHeader(200),
        body = fileContent
      ).withHeaders("Content-type" -> "application/force-download", "Content-Disposition" -> "attachment")
    } catch {
      case e: Exception =>
        logger.error(s"downloadFromHdfs error: file not found. path=$path, filename=$filename")
        NotFound
    }
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
    implicit request =>
      val language = langFromRequestCookie(request)

      val lang: Language = if (language.language.startsWith("zh")) Language.Chinese
      else Language.English

      NotificationService.getNotifications(lang) map {
        case result =>
          Ok(result.toJson)
      }
  }

  // def registerView() = Action {
  //   implicit request =>
  //     Ok(views.html.viewRegister.render(langFromRequestCookie(request)))
  // }

  // def assetView() = Action {
  //   implicit request =>
  //     Ok(views.html.viewAsset.render(langFromRequestCookie(request)))
  // }

  // def transferView() = Action {
  //   implicit request =>
  //     Ok(views.html.viewTransfer.render(langFromRequestCookie(request)))
  // }

  // def depositView() = Action {
  //   implicit request =>
  //     Ok(views.html.viewDeposit.render(langFromRequestCookie(request)))
  // }

  // def depositDebugView() = Action {
  //   implicit request =>
  //     if (request.session.get("uid").getOrElse("0").toLong != 1000000027L)
  //       Forbidden
  //     else
  //       Ok(views.html.viewDepositDebug.render(langFromRequestCookie(request)))
  // }

  // def withdrawalView() = Action {
  //   implicit request =>
  //     Ok(views.html.viewWithdrawal.render(request.session, langFromRequestCookie(request)))
  // }

  // def ordersView() = Action {
  //   implicit request =>
  //     Ok(views.html.viewOrders.render(langFromRequestCookie(request)))
  // }

  // def transactionsView() = Action {
  //   implicit request =>
  //     Ok(views.html.viewTransactions.render(langFromRequestCookie(request)))
  // }

   def opendataView() = Action {
     implicit request =>
       Ok(views.html.viewOpendata.render(langFromRequestCookie(request)))
   }

   def reserveView() = Action {
     implicit request =>
       Ok(views.html.viewReserve.render(langFromRequestCookie(request)))
   }

   def opensourceView() = Action {
     implicit request =>
       Ok(views.html.viewOpensource.render(langFromRequestCookie(request)))
   }

  // def connectivityView() = Action {
  //   implicit request =>
  //     Ok(views.html.viewConnectivity.render(langFromRequestCookie(request)))
  // }

   def openmarketView() = Action {
     implicit request =>
       Ok(views.html._open_market.render(langFromRequestCookie(request)))
   }

  def terms() = Action {
    implicit request =>
      Ok(views.html.terms.render(request.session, langFromRequestCookie(request)))
  }

  def privacy() = Action {
    implicit request =>
    Ok(views.html.privacy.render(request.session, langFromRequestCookie(request)))
  }

  def onServerError() = Action {
    implicit request =>
      Ok(views.html.errorPage.render("", langFromRequestCookie(request)))
  }

}
