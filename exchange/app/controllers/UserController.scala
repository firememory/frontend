/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package controllers

import play.api.mvc._
import play.api.libs.json._
import services.UserService
import models.{ApiResult, User}
import models.Implicits._
import scala.concurrent.Future
import com.coinport.coinex.data.{LoginSucceeded, UserProfile}
import scala.concurrent.ExecutionContext.Implicits.global

object UserController extends Controller {
  def login = Action.async(parse.json) {
    implicit request =>
      val json = request.body
      println("login with: " + json)
      json.validate[User] match {
        case s: JsSuccess[User] =>
          val user: User = s.get
          UserService.login(user) map {
            result =>
              if (result.success) {
                val returnUser = result.data.asInstanceOf[LoginSucceeded]
                Ok(Json.toJson(result)).withSession(
                  "username" -> returnUser.email,
                  "uid" -> returnUser.id.toString
                )
              } else {
                Ok(Json.toJson(result))
              }
          }
        case e: JsError =>
          Future {
            Ok(Json.toJson(ApiResult(false, -1, "error: " + e)))
          }
      }
  }

  def register = Action.async(parse.json) {
    implicit request =>
      val json = request.body
      println("try register: " + json)
      json.validate[User] match {
        case s: JsSuccess[User] => {
          val user: User = s.get
          UserService.register(user) map {
            result =>
              if (result.success) {
                val profile = result.data.get.asInstanceOf[UserProfile]
                Ok(Json.toJson(result)).withSession(
                  "username" -> profile.email,
                  "uid" -> profile.id.toString
                )
              } else {
                Ok(Json.toJson(result))
              }
          }
        }
        case e: JsError => {
          Future {
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
