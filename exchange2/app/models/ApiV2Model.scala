/**
 * Copyright 2015 Coinport Inc. All Rights Reserved.
 * Author: xiaolu@coinport.com (Xiaolu Wu)
 */

package models

import com.coinport.coinex.api.model._

case class ApiV2History(items: Seq[ApiCandleItem])
