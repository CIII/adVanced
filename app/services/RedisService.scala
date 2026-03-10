package services

import javax.inject.{Inject, Singleton}
import io.lettuce.core.{RedisClient => LettuceRedisClient}
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.api.async.RedisAsyncCommands
import play.api.{Configuration, Logging}
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

@Singleton
class RedisService @Inject()(
  configuration: Configuration,
  lifecycle: ApplicationLifecycle
) extends Logging {

  private val host = configuration.get[String]("redis.host")
  private val port = configuration.get[Int]("redis.port")

  val client: LettuceRedisClient = LettuceRedisClient.create(s"redis://$host:$port")
  val connection: StatefulRedisConnection[String, String] = client.connect()
  val sync: RedisCommands[String, String] = connection.sync()
  val async: RedisAsyncCommands[String, String] = connection.async()

  // Verify connection
  val pong = sync.ping()
  logger.info(s"Redis connected to $host:$port (ping: $pong)")

  lifecycle.addStopHook { () =>
    Future.successful {
      connection.close()
      client.shutdown()
    }
  }
}
