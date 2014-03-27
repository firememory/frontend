/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package controllers

import play.api.mvc.{Action, Controller}

object MainController extends Controller {
  def index = Action {
    implicit request =>
      Ok(views.html.index.render(session.get("username")))
  }

  def trade = Action {
    implicit request =>
      Ok(views.html.trade.render(session.get("username"), session.get("uid")))
  }

  def market = Action {
    implicit request =>
      Ok(views.html.market.render(session.get("username")))
  }

  def user = Action {
    implicit request =>
      Ok(views.html.user.render(session.get("username")))
  }

  def register = Action {
    implicit request =>
      Ok(views.html.register.render())
  }

}
