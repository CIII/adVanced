package sync.google.process.management.mcc.account.bidding_strategy

import Shared.Shared._
import org.apache.pekko.actor.Actor
import org.apache.pekko.event.Logging
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import com.mongodb.client.model.UpdateOptions
import models.mongodb.google.Google._
import sync.shared.Google.{GoogleBiddingStrategyDataPullRequest, _}

class BiddingStrategyActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case biddingStrategyDataPullRequest: GoogleBiddingStrategyDataPullRequest =>
      try {
        val strategyDoc = biddingStrategyDataPullRequest.sharedBiddingStrategyObject.sharedBiddingStrategyDoc
        log.info("Processing Incoming Data for Shared Bidding Strategy (%s)".format(
          strategyDoc.getString("id")
        ))

        googleBiddingStrategyCollection.updateOne(
          Document(
            "mccObjId" -> biddingStrategyDataPullRequest.sharedBiddingStrategyObject.customerObject.mccObject.mccObjId,
            "customerObjId" -> biddingStrategyDataPullRequest.sharedBiddingStrategyObject.customerObject.customerObjId,
            "apiId" -> strategyDoc.getString("id")
          ),
          Document("$set" -> Document("biddingStrategy" -> strategyDoc)),
          new UpdateOptions().upsert(true)
        )
      } catch {
        case e: Exception =>
          log.info("Error Retrieving Data for Google Shared Bidding Strategy - %s".format(
            e.getMessage
          ))
      } finally {
        context.stop(self)
      }
    case cache: PendingCacheStructure =>
      log.info("Processing Shared Bidding Strategy %s -> %s -> %s -> %s".format(
        cache.changeCategory,
        cache.changeType,
        cache.trafficSource,
        cache.id
      ))
      context.stop(self)
  }
}
