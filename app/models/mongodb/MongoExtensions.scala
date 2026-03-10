package models.mongodb

import org.mongodb.scala._
import org.mongodb.scala.bson.Document

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Implicit extensions providing blocking helpers for the MongoDB Scala driver.
 * The Casbah driver had synchronous methods (.findOne, .toList, etc.) that don't
 * exist in the official driver. This bridges the gap until a full async migration.
 *
 * Usage: import models.mongodb.MongoExtensions._
 */
object MongoExtensions {

  implicit class RichMongoCollection(val coll: MongoCollection[Document]) extends AnyVal {
    def findOne(filter: Document): Option[Document] = {
      val result = Await.result(coll.find(filter).first().toFuture(), 10.seconds)
      Option(result)
    }

    def count(filter: Document): Long = {
      Await.result(coll.countDocuments(filter).toFuture(), 10.seconds)
    }

    def countSync(): Long = {
      Await.result(coll.countDocuments().toFuture(), 10.seconds)
    }

    def insertOne(doc: Document): Unit = {
      Await.result(coll.insertOne(doc).toFuture(), 10.seconds)
    }

    def replaceOne(filter: Document, replacement: Document): Unit = {
      Await.result(coll.replaceOne(filter, replacement).toFuture(), 10.seconds)
    }

    def replaceOne(filter: Document, replacement: Document, options: com.mongodb.client.model.ReplaceOptions): Unit = {
      Await.result(coll.replaceOne(filter, replacement, options).toFuture(), 10.seconds)
    }

    def findOneAndUpdateSync(filter: Document, update: Document, options: com.mongodb.client.model.FindOneAndUpdateOptions): Option[Document] = {
      Option(Await.result(coll.findOneAndUpdate(filter, update, options).toFuture(), 10.seconds))
    }

    def findOneAndReplaceSync(filter: Document, replacement: Document, options: com.mongodb.client.model.FindOneAndReplaceOptions): Option[Document] = {
      Option(Await.result(coll.findOneAndReplace(filter, replacement, options).toFuture(), 10.seconds))
    }

    def distinctSync(fieldName: String): List[String] = {
      Await.result(coll.distinct[String](fieldName).toFuture(), 10.seconds).toList
    }
  }

  implicit class RichFindObservable(val obs: FindObservable[Document]) extends AnyVal {
    def toList: List[Document] = {
      Await.result(obs.toFuture(), 30.seconds).toList
    }

    def toArray: Array[Document] = {
      Await.result(obs.toFuture(), 30.seconds).toArray
    }
  }

  implicit class RichAggregateObservable(val obs: AggregateObservable[Document]) extends AnyVal {
    def toList: List[Document] = {
      Await.result(obs.toFuture(), 30.seconds).toList
    }
  }

  // Note: getString, getLong, getBoolean, getObjectId, getList etc. are available
  // on Document via the built-in implicit conversion `documentToUntypedDocument`
  // (org.mongodb.scala.Document → org.bson.Document) provided by the MongoDB Scala driver.
}
