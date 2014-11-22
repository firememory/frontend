package models

case class PagingWrapper(count: Int, skip: Int, limit: Int, currentPage: Int, pageSize: Int, items: Any)
