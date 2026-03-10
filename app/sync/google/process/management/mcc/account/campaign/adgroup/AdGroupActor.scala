package sync.google.process.management.mcc.account.campaign.adgroup

import Shared.Shared._
import org.apache.pekko.actor.{Actor, Props}
import org.apache.pekko.event.Logging
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import org.bson.types.ObjectId
import models.mongodb.google.Google._
import sync.shared.Google._

class AdGroupActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case adGroupDataPullRequest: GoogleAdGroupDataPullRequest =>
      try {
        val adGroupId = adGroupDataPullRequest.adGroupObject.adGroupDoc.get("id").map(_.toString).getOrElse("unknown")
        log.info("Retrieving AdGroup Data (%s)".format(adGroupId))

        // TODO: Not yet migrated to Google Ads API v18
        // The old AdWords API v201609 SelectorBuilder, AdGroupAdHelper, AdGroupCriterionHelper are no longer available.
        // Implement using Google Ads API v18 AdGroupService, AdGroupAdService, AdGroupCriterionService.
        log.info("Not yet migrated to Google Ads API v18 - ad group data pull is stubbed out")

      } catch {
        case e: Exception =>
          log.info("Error Retrieving Data for Google AdGroup - %s".format(e.getMessage))
          log.error(s"Error processing Google AdGroup: ${e.getMessage}")
      } finally {
        context.stop(self)
      }

    case cache: PendingCacheStructure =>
      try {
        log.info("Processing Pending Cache %s -> %s -> %s -> %s".format(
          cache.changeCategory,
          cache.changeType,
          cache.trafficSource,
          cache.id
        ))

        // TODO: Not yet migrated to Google Ads API v18
        // The old AdWords API v201609 AdGroup, AdGroupStatus, CriterionTypeGroup, BiddingStrategyConfiguration,
        // CpcBid, Money, SelectorBuilder, AdGroupHelper are no longer available.
        // Implement using Google Ads API v18 AdGroupService.
        log.info("Not yet migrated to Google Ads API v18 - ad group cache processing is stubbed out")

      } catch {
        case e: Exception => log.error(s"Error processing Google AdGroup cache: ${e.getMessage}")
      } finally {
        context.stop(self)
      }
  }
}
