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

object MainController extends Controller with Json4s {
  def index = Action {
    implicit request =>
      Ok(views.html.index.render(session))
  }

  def trade = Action {
    implicit request =>
      session.get("uid").map {
        uid =>
          Ok(views.html.trade.render(session))
      } getOrElse {
        Redirect(routes.MainController.login())
      }
  }

  def account() = Action {
    implicit request =>
      Ok(views.html.account.render(session))
  }

  def market = Action {
    implicit request =>
      Ok(views.html.market.render(session))
  }

  def user(uid: String) = Action {
    implicit request =>
      Ok(views.html.user.render(uid, session))
  }

  def order(oid: String) = Action {
    implicit request =>
      Ok(views.html.order.render(oid, session))
  }

  def transaction(tid: String) = Action {
    implicit request =>
      Ok(views.html.transaction.render(tid, session))
  }

  def login = Action {
    implicit request =>
      Ok(views.html.login.render(session))
  }

  def register = Action {
    implicit request =>
      Ok(views.html.register.render(session))
  }

  def open = Action {
    implicit request =>
      Ok(views.html.open.render(session))
  }

  def downloadFromHdfs(path: String, filename: String) = Action {
    val stream = HdfsAccess.getFileStream(path, filename)
    val fileContent: Enumerator[Array[Byte]] = Enumerator.fromStream(stream)
      .onDoneEnumerating {
      stream.close()
    }

    SimpleResult(
      header = ResponseHeader(200),
      body = fileContent
    ).withHeaders("Content-type" -> "application/force-download", "Content-Disposition" -> "attachment")
  }

  def listFilesFromHdfs(path: String) = Action {
    val files = HdfsAccess.listFiles(path)
      .sortWith((a, b) => a.updated > b.updated)

    val result = ApiResult(data = Some(files))

    Ok(result.toJson)
  }
}
