/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package controllers

import play.api.mvc._
import play.api.libs.iteratee.Enumerator
import scala.concurrent.ExecutionContext.Implicits.global

object MainController extends Controller {
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

  def market = Action {
    implicit request =>
      Ok(views.html.market.render(session.get("username")))
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

  def data(fileName: String) = Action {
    val path = "/data/export/" + fileName
    val file = new java.io.File(path)
    val fileContent: Enumerator[Array[Byte]] = Enumerator.fromFile(file)
    SimpleResult(
      header = ResponseHeader(200),
      body = fileContent
    ).withHeaders("Content-Disposition" -> "attachment")
  }
}
