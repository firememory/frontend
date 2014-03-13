/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package models

case class ApiResult(success: Boolean, code: Int = 0, message: String = "")