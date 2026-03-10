package sync.google.process.management.mcc.account.campaign.adgroup

import Shared.Shared._
import org.apache.pekko.actor.Actor
import org.apache.pekko.event.Logging
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.google.Google._
import sync.shared.Google._

class AdGroupBidModifierActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case adGroupBidModifierDataPullRequest: GoogleAdGroupBidModifierDataPullRequest =>
      try {
        val bidModifierDoc = adGroupBidModifierDataPullRequest.adGroupBidModifierObject.adGroupBidModifierDoc
        val criterionId = bidModifierDoc.get("criterionId").map(_.toString).getOrElse("unknown")
        log.info(s"Processing Incoming Ad Group Bid Modifier ($criterionId)")

        // TODO: Not yet migrated to Google Ads API v18
        // The old AdWords API v201609 AdGroupBidModifier types are no longer available.
        // Implement using Google Ads API v18 AdGroupBidModifierService.
        log.info("Not yet migrated to Google Ads API v18 - bid modifier data pull is stubbed out")

      } catch {
        case e: Exception =>
          log.info("Error Retrieving Data for AdGroup Bid Modifier - %s".format(e.getMessage))
          log.error(s"Error processing AdGroup bid modifier: ${e.getMessage}")
      } finally {
        context.stop(self)
      }
  }
}
