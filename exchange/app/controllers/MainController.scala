package controllers

import play.api.mvc.{Action, Controller}

object MainController extends Controller {
  def index = Action {
    implicit request =>
      Ok(views.html.index.render(session.get("username")))
  }

  def trade = Action {
    implicit request =>
      Ok(views.html.trade.render(session.get("username")))
  }

  def market = Action {
    implicit request =>
      Ok(views.html.market.render(session.get("username")))
  }

  def user = Action {
    implicit request =>
      Ok(views.html.user.render(session.get("username")))
  }

}
