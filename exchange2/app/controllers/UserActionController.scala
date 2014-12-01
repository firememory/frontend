/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package controllers

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.mvc._
import play.api.Logger
import com.github.tototoshi.play2.json4s.native.Json4s
import com.coinport.coinex.api.model._
import com.coinport.coinex.data._
import com.coinport.coinex.api.service._

object UserActionController extends Controller with Json4s with AccessLogging {
  val logger = Logger(this.getClass)

  def getUserLoginHistory = Action.async {
    implicit request =>
      val userId = request.session.get("uid").getOrElse("")
      UserActionService.getLoginHistory(userId.toLong) map {
        result =>
          Ok(result.toJson)
      }
  }

}
