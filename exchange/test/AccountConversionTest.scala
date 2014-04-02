/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

import com.coinport.coinex.data._
import com.coinport.coinex.data.Currency._
import org.specs2.mutable._
import models._

class AccountConversionTest extends Specification {
  "account conversions" should {
    "user account conversion" in {
      val accounts: scala.collection.Map[Currency, CashAccount] = scala.collection.Map(
        Btc -> CashAccount(Currency.Btc, 8000, 2000, 0),
        Rmb -> CashAccount(Currency.Rmb, 100000, 0, 0)
      )
      val backendAccount = com.coinport.coinex.data.UserAccount(123L, cashAccounts = accounts)
      val userAccount: models.UserAccount = backendAccount

      userAccount must equalTo(models.UserAccount(123L, Map("RMB" -> 1000.0, "BTC" -> 8.0)))
    }
  }
}