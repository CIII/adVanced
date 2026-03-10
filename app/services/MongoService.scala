package services

import javax.inject.{Inject, Singleton}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import play.api.{Configuration, Logging}

@Singleton
class MongoService @Inject()(configuration: Configuration) extends Logging {

  private val host = configuration.get[String]("mongodb.host")
  private val port = configuration.get[Int]("mongodb.port")
  private val dbName = configuration.get[String]("mongodb.db")

  val client: MongoClient = MongoClient(s"mongodb://$host:$port")
  val database: MongoDatabase = client.getDatabase(dbName)

  def collection(name: String): MongoCollection[Document] = database.getCollection(name)

  logger.info(s"MongoDB connected to $host:$port/$dbName")
}
