package sync.google.process.management.mcc

import Shared.Shared._
import org.apache.pekko.actor.{Actor, Props}
import org.apache.pekko.event.Logging
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import org.bson.types.ObjectId
import com.mongodb.client.model.ReplaceOptions
import models.mongodb.google.Google._
import models.mongodb.MongoExtensions._
import sync.google.process.management.mcc.account.CustomerActor
import sync.shared.Google._

class MccActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case cache_msg: PendingCacheMessage =>
      val cache: PendingCacheStructure = cache_msg.cache.get
      try {
        val mcc_data = documentToMcc(cache.changeData)
        log.info("Processing MCC %s -> %s -> %s -> %s".format(
          cache.changeCategory,
          cache.changeType,
          cache.trafficSource,
          cache.id
        ))

        val mccObj = googleMccCollection.replaceOne(
          Document("oAuthRefreshToken" -> mcc_data.oAuthRefreshToken),
          mccToDocument(mcc_data),
          new ReplaceOptions().upsert(true)
        )

        // TODO: Not yet migrated to Google Ads API v18
        // AdWordsHelper / CustomerHelper are not available until Google Ads API v18 migration is complete.
        log.info("Not yet migrated to Google Ads API v18 - MCC customer traversal is stubbed out")

        complete_subprocess(taskKey(cache_msg.requestUsername.getOrElse("")), cache)
      } catch {
        case e: Exception => log.info("Error Retrieving Data for MCC (%s) - %s".format(cache.id, e.getMessage))
      } finally {
        context.stop(self)
      }
    case _ =>
      try {
        googleMccCollection.find().toList.foreach {
          mcc_obj =>
            val mcc = documentToMcc(mcc_obj)

            // TODO: Not yet migrated to Google Ads API v18
            // AdWordsHelper / CustomerHelper are not available until Google Ads API v18 migration is complete.
            log.info("Not yet migrated to Google Ads API v18 - incremental MCC pull is stubbed out for mcc: %s".format(mcc.name))
        }
      } catch {
        case e: Exception =>
          log.info("Error Retrieving Incremental Data for MCC - %s".format(
            e.toString
          ))
          log.error(s"Error retrieving incremental MCC data: ${e.getMessage}")
      } finally {
        context.stop(self)
      }
  }
}
