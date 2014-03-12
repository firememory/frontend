package models

import utils.MurmurHash

case class User(username: String, password: String) {
  val uid: Long = {
    MurmurHash.hash64(username)
  }
}
