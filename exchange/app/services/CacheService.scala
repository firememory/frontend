package services

trait Cache[K, V] {
  def put(key: K, value: V)
  def putWithTimeout(key: K, value: V, timeoutSecs: Int)
  def get(key: K): V
}

trait CacheService {

}

object CacheService {

}
