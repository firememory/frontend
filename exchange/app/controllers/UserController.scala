/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package controllers

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.UserService
import models.{ApiResult, User}
import models.Implicits._

object UserController extends Controller {


  def login = Action(parse.json) {
    request => {
      val json = request.body
      println("login with: " + json)
      json.validate[User] match {
        case s: JsSuccess[User] => {
          val login: User = s.get
          val user = UserService.getUser(login.username)
          if (user == null)
            Ok(Json.toJson(ApiResult(false, 1, "user not found")))
          else if (user.password.equals(login.password))
            Ok(Json.toJson(ApiResult(true))).withSession(
              "username" -> user.username,
              "uid" -> user.uid.toString
            )
          else
            Ok(Json.toJson(ApiResult(false, 2, "incorrect password")))
        }
        case e: JsError => {
          println(e)
          Ok(Json.toJson(ApiResult(false, -1, "error: " + e)))
        }
      }
    }
  }

  def register = Action(parse.json) {
    request => {
      val json = request.body
      println("try register: " + json)
      json.validate[User] match {
        case s: JsSuccess[User] => {
          val user: User = s.get
          if (UserService.getUser(user.username) != null) {
            Ok(Json.toJson(ApiResult(false, 1,  "user exists")))
        } else {
            UserService.addUser(user)
            Ok(Json.toJson(ApiResult(true))).withSession(
              "username" -> user.username,
              "uid" -> user.uid.toString
            )
          }
        }
        case e: JsError => {
          println(e)
          Ok(Json.toJson(ApiResult(false, -1, "error: " + e)))
        }
      }
    }
  }

  def logout = Action {
    implicit request =>
      Redirect(routes.MainController.index()).withSession(
        session - "username")
  }
}
