package sync.google.process.management.mcc.account.campaign.criterion

import Shared.Shared._
import org.apache.pekko.actor.Actor
import org.apache.pekko.event.Logging
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import org.bson.types.ObjectId
import models.mongodb.google.Google._
import sync.shared.Google._

class CampaignCriterionActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case campaignCriterionDataPullRequest: GoogleCampaignCriterionDataPullRequest =>
      try {
        val criterionDoc = campaignCriterionDataPullRequest.campaignCriterionObject.campaignCriterionDoc
        val criterionId = criterionDoc.get("criterionId").map(_.toString).getOrElse("unknown")
        log.info(s"Processing Incoming Campaign Criterion ($criterionId)")

        // TODO: Not yet migrated to Google Ads API v18
        // The old AdWords API v201609 CampaignCriterion types are no longer available.
        // Implement using Google Ads API v18 CampaignCriterionService.
        log.info("Not yet migrated to Google Ads API v18 - campaign criterion data pull is stubbed out")

      } catch {
        case e: Exception =>
          log.info(s"Error Retrieving Data for Google Campaign Criterion - ${e.getMessage}")
          log.error(s"Error processing Google campaign criterion: ${e.getMessage}")
      } finally {
        context.stop(self)
      }

    case cache: PendingCacheStructure =>
      try {
        log.info("Processing %s -> %s -> %s -> %s".format(
          cache.changeCategory,
          cache.changeType,
          cache.trafficSource,
          cache.id
        ))

        // TODO: Not yet migrated to Google Ads API v18
        // The old AdWords API v201609 Campaign, CampaignStatus, AdvertisingChannelType,
        // SelectorBuilder, CampaignHelper types are no longer available.
        // Implement using Google Ads API v18 CampaignCriterionService.
        log.info("Not yet migrated to Google Ads API v18 - campaign criterion cache processing is stubbed out")

      } catch {
        case e: Exception => log.error(s"Error processing Google campaign criterion cache: ${e.getMessage}")
      } finally {
        context.stop(self)
      }
  }
}
