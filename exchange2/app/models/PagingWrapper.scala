package models

case class PagingWrapper(count: Int, skip: Int, limit: Int, currentPage: Int, pageSize: Int, items: Any)

case class ApiV2PagingWrapper(hasMore: Boolean, currency: String, path: String, items: Any)

case class ApiV2TradesPagingWrapper(hasMore: Boolean, market: String, trades: Any)

case class ApiV2OrderPagingWrapper(hasMore: Boolean, orders: Seq[ApiV2Order])

case class ApiV2DepositsPagingWrapper(hasMore: Boolean, deposits: Seq[ApiV2TransferItem])

case class ApiV2WithdrawalsPagingWrapper(hasMore: Boolean, withdrawals: Seq[ApiV2TransferItem])

