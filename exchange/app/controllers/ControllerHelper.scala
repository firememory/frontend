package controllers

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.coinport.coinex.api.model._
import play.api.mvc.Request

case class Pager(skip: Int = 0, limit: Int = 10, page: Int)

trait Validator {
  def result: ApiResult
  def validate: Either[ApiResult, Boolean]
}

abstract class GeneralValidator[T](params: T*) extends Validator {
  def isValid(t: T): Boolean
  def validate = validate(params)

  private def validate(params: Seq[T]): Either[ApiResult, Boolean] =
    if (params.isEmpty) Right(true)
    else {
      if (!isValid(params.head))
        Left(result)
      else
        validate(params.tail)
    }
}

object ControllerHelper {
  val parmaErrorResult = ApiResult(false, -1, "参数错误", None)
  val emptyParamError = ApiResult(false, -1, "param can not empty", None)
  val emailFormatError = ApiResult(false, -1, "email format error", None)
  val passwordFormatError = ApiResult(false, -1, "password format error", None)

  class StringNonemptyValidator(stringParams: String*) extends GeneralValidator[String](stringParams: _*) {
    val result = emptyParamError
    def isValid(param: String) = param != null && param.trim.length > 0
  }

  class EmailFormatValidator(emails: String*) extends GeneralValidator[String](emails: _*) {
    val result = emailFormatError
    val emailRegex = """(\w+)@([\w\.]+)""".r
    def isValid(param: String) = param.matches(emailRegex.toString)
  }

  class PasswordFormetValidator(passwords: String*) extends GeneralValidator[String](passwords: _*) {
    val result = passwordFormatError
    def isValid(param: String) = param.trim.length > 8
  }

  def validateParamsAndThen(validators: Validator*)(f: => Future[ApiResult]): Future[ApiResult] =
    if (validators.isEmpty)
      f
    else {
      validators.head.validate match {
        case Left(r) => Future(r)
        case Right(b) => validateParamsAndThen(validators.tail: _*)(f)
      }
    }

  def getParam(queryString: Map[String, Seq[String]], param: String): Option[String] = {
    queryString.get(param).map(_(0))
  }

  def getParam(queryString: Map[String, Seq[String]], param: String, default: String): String = {
    queryString.get(param) match {
      case Some(seq) =>
        if (seq.isEmpty) default else seq(0)
      case None =>
        default
    }
  }

  def parsePagingParam()(implicit request: Request[_]): Pager = {
    val query = request.queryString
    val limit = getParam(query, "limit", "10").toInt
    val page = getParam(query, "page", "1").toInt
    val skip = (page - 1) * limit
    Pager(skip = skip, limit = limit, page = page)
  }

}
