package sync.google.process.management.mcc.account.bidding_strategy

import Shared.Shared._
import akka.actor.Actor
import akka.event.Logging
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import models.mongodb.google.Google._
import sync.google.adwords.account.CustomerHelper
import sync.shared.Google.{GoogleBiddingStrategyDataPullRequest, _}

class BiddingStrategyActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case biddingStrategyDataPullRequest: GoogleBiddingStrategyDataPullRequest =>
      try {
        log.info("Processing Incoming Data for Shared Bidding Strategy (%s)".format(
          biddingStrategyDataPullRequest.sharedBiddingStrategyObject.sharedBiddingStrategy.getId
        ))

        googleBiddingStrategyCollection.update(
          DBObject(
            "mccObjId" -> biddingStrategyDataPullRequest.sharedBiddingStrategyObject.customerObject.mccObject.mccObjId,
            "customerObjId" -> biddingStrategyDataPullRequest.sharedBiddingStrategyObject.customerObject.customerObjId,
            "apiId" -> biddingStrategyDataPullRequest.sharedBiddingStrategyObject.sharedBiddingStrategy.getId
          ),
          DBObject("$set" -> DBObject("biddingStrategy" -> DBObject(
            "classPath" -> biddingStrategyDataPullRequest.sharedBiddingStrategyObject.sharedBiddingStrategy.getClass.getCanonicalName,
            "object" -> JSON.parse(gson.toJson(biddingStrategyDataPullRequest.sharedBiddingStrategyObject.sharedBiddingStrategy)).asInstanceOf[DBObject]
          ))),
          true
        )
      } catch {
        case e: Exception =>
          log.info("Error Retrieving Data for Google Shared Bidding Strategy (%s) - %s".format(
            biddingStrategyDataPullRequest.sharedBiddingStrategyObject.sharedBiddingStrategy.getName,
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