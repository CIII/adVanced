package sync.google.process.management.mcc.account.campaign.adgroup.ad

import Shared.Shared._
import org.apache.pekko.actor.Actor
import org.apache.pekko.event.Logging
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import org.bson.types.ObjectId
import models.mongodb.google.Google._
import sync.shared.Google._

class AdGroupAdActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case adGroupAdDataPullRequest: GoogleAdGroupAdDataPullRequest =>
      try {
        val adDoc = adGroupAdDataPullRequest.adGroupAdObject.adGroupAdDoc
        val adId = adDoc.get("adId").map(_.toString).getOrElse("unknown")
        log.info("Processing Incoming Data for AdGroupAd (%s)".format(adId))

        // TODO: Not yet migrated to Google Ads API v18
        // The old AdWords API v201609 AdGroupAd types are no longer available.
        // Implement using Google Ads API v18 AdGroupAdService.
        log.info("Not yet migrated to Google Ads API v18 - ad group ad data pull is stubbed out")

      } catch {
        case e: Exception =>
          log.info("Error Retrieving Data for Google AdGroup Ad Actor - %s".format(e.getMessage))
          log.error(s"Error processing Google AdGroup ad: ${e.getMessage}")
      } finally {
        context.stop(self)
      }

    case cache: PendingCacheStructure =>
      try {
        log.info("Processing AdGroupAd %s -> %s -> %s -> %s".format(
          cache.changeCategory,
          cache.changeType,
          cache.trafficSource,
          cache.id
        ))

        // TODO: Not yet migrated to Google Ads API v18
        // The old AdWords API v201609 AdGroupAd, Ad, AdGroupAdStatus, SelectorBuilder,
        // AdGroupAdHelper types are no longer available.
        // Implement using Google Ads API v18 AdGroupAdService.
        log.info("Not yet migrated to Google Ads API v18 - ad group ad cache processing is stubbed out")

      } catch {
        case e: Exception => log.error(s"Error processing Google AdGroup ad cache: ${e.getMessage}")
      } finally {
        context.stop(self)
      }
  }
}
