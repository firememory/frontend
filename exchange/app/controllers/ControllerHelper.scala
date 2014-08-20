package controllers

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.Properties
import java.io.FileInputStream
import com.coinport.coinex.api.model._
import com.coinport.coinex.data.ErrorCode
import play.api.mvc._
import play.api.Logger
import play.api.i18n.Lang
import play.api.Play
import play.api.Play.current
import services.CacheService
import controllers.GoogleAuth.GoogleAuthenticator

case class Pager(skip: Int = 0, limit: Int = 10, page: Int)

trait AccessLogging {
  val accessLogger = Logger("access")

  object AccessLoggingAction extends ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
      accessLogger.info(s"method=${request.method} uri=${request.uri} remote-address=${request.remoteAddress}")
      block(request)
    }
  }
}

trait Validator {
  def result: ApiResult
  def validate: Either[ApiResult, Boolean]
  def logger: Logger = Logger("validator")
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

class CachedValueValidator(error: ErrorCode, check: Boolean, uuid: String, value: String) extends Validator {
  val cacheService = CacheService.getDefaultServiceImpl
  val result = ApiResult(false, error.value, error.toString)

  def validate = {
    if (!check) Right(true)
    else {
      if (uuid == null || uuid.trim.isEmpty || value == null || value.trim.isEmpty) Left(result) else {
        val cachedValue = cacheService.get(uuid)
        logger.info(s" validate cached value. uuid: $uuid, cachedValue: $cachedValue")
        if (cachedValue != null && cachedValue.equals(value)) {
          //cacheService.pop(uuid)
          Right(true)
        } else Left(result)
      }
    }
  }
}

class GoogleAuthValidator(error: ErrorCode, secret: String, code: String) extends Validator {
  val result = ApiResult(false, error.value, error.toString)

  val codeInt = try{code.toInt} catch {case e: Exception => 0}
  val googleAuthenticator = new GoogleAuthenticator()

  def validate = {
    logger.info(s"google auth validator: secret: $secret, code: $code")
    if (secret == null || secret.trim.isEmpty) Right(true)
    else if(codeInt == 0) Left(result)
    else {
      if (googleAuthenticator.authorize(secret.toString, codeInt)) {
        logger.info(s"google authentication success")
        Right(true)
      } else {
        logger.info(s"google authentication failed")
        Left(result)
      }
    }
  }
}

object ControllerHelper {
  val emptyParamError = ApiResult(false, ErrorCode.ParamEmpty.value, "param can not emppty", None)
  val emailFormatError = ApiResult(false, ErrorCode.InvalidEmailFormat.value, "email format error", None)
  val passwordFormatError = ApiResult(false, ErrorCode.InvalidPasswordFormat.value, "password format error", None)
  val inviteCodeError = ApiResult(false, ErrorCode.EmailNotBindWithInviteCode.value, "invalid invite code", None)
  val phoneNumberFormatError = ApiResult(false, ErrorCode.InvalidPhoneNumberFormat.value, "invalid mobile phone number", None)

  class StringNonemptyValidator(stringParams: String*) extends GeneralValidator[String](stringParams: _*) {
    val result = emptyParamError
    def isValid(param: String) = param != null && param.trim.length > 0
  }

  class EmailFormatValidator(emails: String*) extends GeneralValidator[String](emails: _*) {
    val result = emailFormatError
    val emailRegex = """^[-0-9a-zA-Z.+_]+@[-0-9a-zA-Z.+_]+\.[a-zA-Z]{2,4}$"""
    def isValid(param: String) = param.matches(emailRegex)
  }

  class PasswordFormetValidator(passwords: String*) extends GeneralValidator[String](passwords: _*) {
    val result = passwordFormatError
    def isValid(param: String) = param.trim.length > 8
  }

  class PhoneNumberValidator(phoneNum: String*) extends GeneralValidator[String](phoneNum: _*) {
    val result = phoneNumberFormatError
    def isValid(param: String) = {
      val res1 = param != null && param.trim.length > 9
      val res2 = if (param.startsWith("+86")) param.substring(3).trim.length == 11 else true
      res1 && res2
    }
  }

  def popCachedValue(uuids: String*): Unit = {
    val cacheService = CacheService.getDefaultServiceImpl
    uuids.foreach { uuid => cacheService.pop(uuid) }
  }

  // class EmailWithInviteCodeValidator(emails: String*) extends GeneralValidator[String](emails: _*) {
  //   val result = inviteCodeError
  //   def isValid(param: String): Boolean = {
  //     val props = new Properties()
  //     var input: FileInputStream = null
  //     try {
  //       input = new FileInputStream(UserController.usedInviteCodeFile)
  //       props.load(input)
  //       logger.info(s"email: $param, all emails: ${props.values}")
  //       props.values.contains(param)
  //     } catch {
  //       case e: Exception =>
  //         logger.error(e.getMessage, e)
  //         false
  //     } finally {
  //       input.close()
  //     }
  //   }
  // }

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

  def langFromRequestCookie(request: Request[_]): Lang = {
    request.cookies.get(Play.langCookieName) match {
      case Some(langCookie) => Lang(langCookie.value)
      case None =>
        if(request.acceptLanguages.size > 0) request.acceptLanguages(0)
        else Lang("zh-CN")
    }
  }
}
