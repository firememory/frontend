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
      Ok(views.html.index.render(session.get("username"), session.get("uid")))
  }

  def trade = Action {
    implicit request =>
      session.get("uid").map {
        uid =>
          Ok(views.html.trade.render(session.get("username"), Some(uid)))
      } getOrElse {
        Redirect(routes.MainController.login())
      }
  }

  def market = Action {
    implicit request =>
      Ok(views.html.market.render(session.get("username")))
  }

  def user = Action {
    implicit request =>
      Ok(views.html.user.render(session.get("username")))
  }

  def login = Action {
    Ok(views.html.login.render())
  }

  def register = Action {
    implicit request =>
      Ok(views.html.register.render())
  }

  def open = Action {
    implicit request =>
      Ok(views.html.open.render(session.get("username"), session.get("uid")))
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
