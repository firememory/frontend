package services

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

object CloopenRestSmsService {}
// object CloopenRestSmsService extends SmsService {
//   val cloopen = smsConfig.getConfig("sms.cloopen")
//   val host = cloopen.getString("host")
//   val port = cloopen.getString("port")
//   val accountSid = cloopen.getString("accountSid")
//   val accountToken = cloopen.getString("accountToken")
//   val appId = cloopen.getString("appId")
//   val templateId = cloopen.getString("templateId")
//   val statusCode_Ok = "000000"

//   val restAPI: CCPRestSDK = new CCPRestSDK()
//   restAPI.init(host, port)
//   restAPI.setAccount(accountSid, accountToken)
//   restAPI.setAppId(appId)

//   override def sendSmsSingle(phoneNum: String, text: String): Future[ApiResult] = future {
//     future { ApiResult(false, -1, "not implemented 3.", None) }
//   }

//   override def sendSmsGroup(phoneNums: Set[String], text: String): Future[ApiResult] = {
//     future { ApiResult(false, -1, "not implemented 2.", None) }
//   }
//   override def sendVerifySms(phoneNum: String, randCode: String): Future[ApiResult] = {
//     val result = restAPI.sendTemplateSMS(phoneNum, templateId, Array[String](text))
//     val statusCode = result.get("statusCode")
//     if (statusCode_Ok.equals(statusCode)) {
//       result.foreach(kv => println(s"key: ${kv.key}, value: ${kv.value}"))
//       Future{ApiResult(true, 0, "")}
//     } else {
//       Future{ApiResult(false, statusCode.toInt, result.get(statusMsg))}
//     }
//   }

// }
