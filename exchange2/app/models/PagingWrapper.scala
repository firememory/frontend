package models

case class PagingWrapper(count: Int, skip: Int, limit: Int, currentPage: Int, pageSize: Int, items: Any)

case class ApiV2PagingWrapper(hasMore: Boolean, currency: String, path: String, items: Any)

case class ApiV2TradesPagingWrapper(timestamp: Long, hasMore: Boolean, market: String, trades: Any)
