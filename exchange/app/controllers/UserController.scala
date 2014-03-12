package controllers

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.UserService
import models.User

case class RegisterResult(success: Boolean, message: String = "")

object UserController extends Controller {
    implicit val registerWrites: Writes[RegisterResult] = (
      (JsPath \ "success").write[Boolean] and
        (JsPath \ "message").write[String]
      )(unlift(RegisterResult.unapply))

  implicit val userReads: Reads[User] = (
    (JsPath \ "username").read[String] and
      (JsPath \ "password").read[String]
    )(User.apply _)

  def login = Action(parse.json) {
    request => {
      val json = request.body
      println("login with: " + json)
      json.validate[User] match {
        case s: JsSuccess[User] => {
          val login: User = s.get
          val user = UserService.getUser(login.username)
          if (user == null)
            Ok(Json.toJson(RegisterResult(false,"user not found")))
          else if (user.password.equals(login.password))
            Ok(Json.toJson(RegisterResult(true))).withSession(
              "username" -> user.username,
              "uid" -> user.uid.toString
            )
          else
            Ok(Json.toJson(RegisterResult(false,"incorrect password")))
        }
        case e: JsError => {
          println(e)
          Ok(Json.toJson(RegisterResult(false, "error: " + e)))
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
            Ok(Json.toJson(RegisterResult(false, "user exists")))
        } else {
            UserService.addUser(user)
            Ok(Json.toJson(RegisterResult(true))).withSession(
              "username" -> user.username,
              "uid" -> user.uid.toString
            )
          }
        }
        case e: JsError => {
          println(e)
          Ok(Json.toJson(RegisterResult(false, "error: " + e)))
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
