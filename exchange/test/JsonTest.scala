/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

import org.specs2.mutable._
import models._
import models.Implicits._
import play.api.libs.json.Json

class JsonTest extends Specification {
  "models to JSON" should {
    "ApiResult to JSON" in {
      val result = ApiResult(true, 0, "some message")
      Json.toJson(result).toString must equalTo( """{"success":true,"code":0,"message":"some message"}""")
    }
  }
}