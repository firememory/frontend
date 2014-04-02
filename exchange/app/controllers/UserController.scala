/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package controllers

import play.api.mvc._
import play.api.libs.json._
import services.UserService
import models._
import scala.concurrent.Future
import com.coinport.coinex.data.{LoginSucceeded, UserProfile}
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.tototoshi.play2.json4s.native.Json4s

object UserController extends Controller with Json4s {
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
                Ok(result.toJson).withSession(
                  "username" -> returnUser.email,
                  "uid" -> returnUser.id.toString
                )
              } else {
                Ok(result.toJson)
              }
          }
        case e: JsError =>
          Future {
            Ok(ApiResult(false, -1, "error: " + e).toJson)
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
                Ok(result.toJson).withSession(
                  "username" -> profile.email,
                  "uid" -> profile.id.toString
                )
              } else {
                Ok(result.toJson)
              }
          }
        }
        case e: JsError => {
          Future {
            Ok(ApiResult(false, -1, "error: " + e).toJson)
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
