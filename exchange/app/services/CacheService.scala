package services

import java.util.concurrent.TimeUnit
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.typesafe.config.ConfigFactory
import com.redis._

trait CacheService {
  val defaultTimeoutSecs: Int = 24 * 3600  // 24 hours.

  def maximumSize: Int
  def put(key: String, value: String) = putWithTimeout(key, value, defaultTimeoutSecs)
  def putWithTimeout(key: String, value: String, timeoutSecs: Int): Unit
  def get(key: String): String // return null if not present
}

object CacheService {
  val GOOGLE_GUAVA_IMPL = "google-guava-impl"
  val REDIS_IMPL = "redis-impl"

  def getDefaultServiceImpl = getNamedServiceImpl(GOOGLE_GUAVA_IMPL)
  def getNamedServiceImpl(serviceName: String): CacheService = {
    if (GOOGLE_GUAVA_IMPL.equals(serviceName))
      GoogleGuavaCacheService
    else if (REDIS_IMPL.equals(serviceName))
      RedisCacheService
    else
      throw new IllegalArgumentException(s"cache service: $serviceName not found.")
  }
}

object GoogleGuavaCacheService extends CacheService {
  val maximumSize = 100000

  val cache: Cache[String, String] = CacheBuilder.newBuilder()
    .maximumSize(maximumSize)
    .expireAfterWrite(30, TimeUnit.MINUTES)
    .build()

  def putWithTimeout(key: String, value: String, timeoutSecs: Int): Unit = cache.put(key, value)

  def get(key: String): String = cache.getIfPresent(key)
}

object RedisCacheService extends CacheService {
  val redisConfig = ConfigFactory.load("application.conf")
  val redisHost = redisConfig.getString("redis.host")
  val redisPort = redisConfig.getInt("redis.port")
  val redisClient = new RedisClient(redisHost, redisPort)

  def maximumSize: Int = 100000

  def putWithTimeout(key: String, value: String, timeoutSecs: Int): Unit = {
    redisClient.set(key, value)
    redisClient.expire(key, timeoutSecs)
  }

  def get(key: String): String = redisClient.get(key) match {
    case Some(value) => value
    case _ => null
  }
}
