/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package services

import models.User
import scala.collection.mutable.Map

object UserService {
  val users: Map[String, User] = Map.empty

  def addUser(user: User) = {
    users += (user.username -> user)
    println("user " + user + " added.")
    println(users.mkString("\n"))
  }

  def getUser(username: String): User = {
    users.get(username).getOrElse(null)
  }
}