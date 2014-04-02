/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

import models.CurrencyUnit._
import com.coinport.coinex.data._
import org.specs2.mutable._
import models._

class CurrencyConversionTest extends Specification {
  "currency conversions" should {
    "String to Currency" in {
      val currency1: Currency = "RMB"
      val currency2: Currency = "BTC"
      val currency3: Currency = "Rmb"
      val currency4: Currency = "cny"
      val currency5: Currency = "USD"
      val currency6: Currency = "XXC"

      currency1 must equalTo(Currency.Rmb)
      currency2 must equalTo(Currency.Btc)
      currency1 must equalTo(currency3)
      currency1 must equalTo(currency4)
      currency5 must equalTo(Currency.Usd)
      currency6 must beNull
    }

    "currency unit conversion" in {
      val a = 1 unit BTC
      val b = 1000 unit MBTC

      a must equalTo(b)

      a / b must equalTo(PriceValue(1.0))

      12.345 unit BTC must equalTo(12345 unit MBTC)
      23.45 unit CNY must equalTo(2345 unit CNY2)
      0 unit BTC must equalTo(0 unit MBTC)

      1 unit BTC to MBTC must equalTo(1000 unit MBTC)
      val sub = 1 unit BTC
      val money = 4000 unit CNY
      money / sub must equalTo(PriceValue(4000, (CNY, BTC)))

      // price conversions
      PriceValue(4000, (CNY, BTC)) to(CNY, MBTC) must equalTo(PriceValue(4, (CNY, MBTC)))
      PriceValue(4000, (CNY, BTC)) to(CNY2, MBTC) must equalTo(PriceValue(400, (CNY2, MBTC)))
      PriceValue(4000, (CNY, BTC)) to(BTC, CNY) must equalTo(PriceValue(1.0 / 4000, (BTC, CNY)))

      // multiply
      val amount = 2 unit BTC
      var price: PriceValue = 4000.0 unit(CNY, BTC)

      amount * price must equalTo(8000.0 unit CNY)
      price * amount must equalTo(8000.0 unit CNY)

      price = 4.0 unit(CNY, MBTC)
      amount * price must equalTo(8000.0 unit CNY)
      price * amount must equalTo(8000.0 unit CNY)
    }
  }
}