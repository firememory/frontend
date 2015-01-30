/**
 * Copyright 2015 Coinport Inc. All Rights Reserved.
 * Author: xiaolu@coinport.com (Xiaolu Wu)
 */

package models

import com.coinport.coinex.api.model._

case class ApiV2History(items: Seq[ApiCandleItem])

case class ApiV2Order(
  id: String,
  operation: String,
  status: Int,
  market: String,
  price: Double,
  amount: Double,
  dealed_amount: Double,
  created: Long,
  last_updated: Option[Long]
)

case class ApiV2TransferItem(id: String, currency: String, quantity: Double, status: Int, created: Long, updated: Long, address: String)

