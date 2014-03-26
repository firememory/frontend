/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package services

import models.{ApiResult, User}
import com.coinport.coinex.data._
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global

object UserService extends AkkaService {
  def register(user: User) = {
    val email = user.username
    val password = user.password

    val profile = UserProfile(0L, email, None, None, None, false, None, false, None, None, None, UserStatus.Normal)
    val command = DoRegisterUser(profile, password)

    Router.backend ? command map {
      case succeeded: RegisterUserSucceeded =>
        val returnProfile = succeeded.userProfile
        ApiResult(true, 0, returnProfile.id.toString, Some(returnProfile))
      case failed: RegisterUserFailed =>
        failed.reason match {
          case RegisterationFailureReason.EmailAlreadyRegistered =>
            ApiResult(false, 1, "用户 " + email + " 已存在")
          case RegisterationFailureReason.MissingInformation =>
            ApiResult(false, 2, "缺少必填字段")
          case _ =>
            ApiResult(false, -1, failed.toString)
        }

      case x =>
        ApiResult(false, -1, x.toString)
    }
  }

  def login(user: User) = {
    val email = user.username
    val password = user.password

    val command = Login(email, password)

    Router.backend ? command map {
      case succeeded: LoginSucceeded =>
        ApiResult(true, 0, "登录成功", Some(succeeded))
      case failed: LoginFailed =>
        failed.reason match {
          case LoginFailureReason.PasswordNotMatch =>
            ApiResult(false, 1, "密码错误")
          case LoginFailureReason.UserNotExist =>
            ApiResult(false, 2, "用户 " + email + " 不存在")
          case _ =>
            ApiResult(false, -1, failed.toString)
        }
      case x =>
        ApiResult(false, -1, x.toString)
    }
  }
}