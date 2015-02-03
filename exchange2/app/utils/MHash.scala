/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 */

package utils

import java.security.MessageDigest

import com.google.common.io.BaseEncoding

object MHash {
  def sha256Base64(str: String) = BaseEncoding.base64.encode(MessageDigest.getInstance("SHA-256").digest(str.getBytes("UTF-8")))
  def sha256Base32(str: String) = BaseEncoding.base32.encode(MessageDigest.getInstance("SHA-256").digest(str.getBytes("UTF-8")))
  def sha1Base32(str: String) = BaseEncoding.base32.encode(MessageDigest.getInstance("SHA-1").digest(str.getBytes("UTF-8")))
}
