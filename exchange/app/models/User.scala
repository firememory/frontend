/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package models

import utils.MurmurHash

case class User(username: String, password: String) {
  val uid: Long = {
    MurmurHash.hash64(username)
  }
}
