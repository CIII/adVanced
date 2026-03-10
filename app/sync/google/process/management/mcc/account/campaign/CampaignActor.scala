package sync.google.process.management.mcc.account.campaign

import Shared.Shared._
import org.apache.pekko.actor.{Actor, Props}
import org.apache.pekko.event.Logging
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import org.bson.types.ObjectId
import models.mongodb.google.Google._
import sync.shared.Google._

class CampaignActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case campaignDataPullRequest: GoogleCampaignDataPullRequest =>
      try {
        val campaignId = campaignDataPullRequest.campaignObject.campaignDoc.get("id").map(_.toString).getOrElse("unknown")
        log.info(s"Processing Incoming Data for Campaign ($campaignId)")

        // TODO: Not yet migrated to Google Ads API v18
        // The old AdWords API v201609 SelectorBuilder, CampaignHelper, AdGroupHelper are no longer available.
        // Implement using Google Ads API v18 CampaignService, AdGroupService, etc.
        log.info("Not yet migrated to Google Ads API v18 - campaign data pull is stubbed out")

      } catch {
        case e: Exception =>
          log.info("Error Retrieving Data for Google Campaign - %s".format(e.getMessage))
          log.error(s"Error processing Google campaign: ${e.getMessage}")
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
        // SelectorBuilder, CampaignHelper, BudgetHelper are no longer available.
        // Implement using Google Ads API v18 CampaignService and BudgetService.
        log.info("Not yet migrated to Google Ads API v18 - campaign cache processing is stubbed out")

      } catch {
        case e: Exception =>
          log.error(s"Error processing Google campaign cache: ${e.getMessage}")
      } finally {
        context.stop(self)
      }
  }
}
