package sync.google.process.management.mcc.account.campaign.adgroup.criterion.keyword

import Shared.Shared._
import org.apache.pekko.actor.Actor
import org.apache.pekko.event.Logging
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import org.bson.types.ObjectId
import models.mongodb.google.Google._
import sync.shared.Google._

class AdGroupCriterionActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case adGroupCriterionDataPullRequest: GoogleAdGroupCriterionDataPullRequest =>
      try {
        val criterionDoc = adGroupCriterionDataPullRequest.adGroupCriterionObject.adGroupCriterionDoc
        val criterionId = criterionDoc.get("criterionId").map(_.toString).getOrElse("unknown")
        log.info("Retrieving AdGroupCriterion Data (%s)".format(criterionId))

        // TODO: Not yet migrated to Google Ads API v18
        // The old AdWords API v201609 AdGroupCriterion types are no longer available.
        // Implement using Google Ads API v18 AdGroupCriterionService.
        log.info("Not yet migrated to Google Ads API v18 - ad group criterion data pull is stubbed out")

      } catch {
        case e: Exception =>
          log.info("Error Retrieving Data for Google AdGroup Criterion - %s".format(e.getMessage))
          log.error(s"Error processing Google AdGroup criterion: ${e.getMessage}")
      } finally {
        context.stop(self)
      }

    case cache: PendingCacheStructure =>
      try {
        log.info("Processing Pending Cache for Keyword %s -> %s -> %s -> %s".format(
          cache.changeCategory,
          cache.changeType,
          cache.trafficSource,
          cache.id
        ))

        // TODO: Not yet migrated to Google Ads API v18
        // The old AdWords API v201609 AdGroupCriterion, BiddableAdGroupCriterion,
        // NegativeAdGroupCriterion, Keyword, KeywordMatchType, CriterionUse,
        // SelectorBuilder, AdGroupCriterionHelper types are no longer available.
        // Implement using Google Ads API v18 AdGroupCriterionService.
        log.info("Not yet migrated to Google Ads API v18 - ad group criterion cache processing is stubbed out")

      } catch {
        case e: Exception => log.error(s"Error processing Google AdGroup criterion cache: ${e.getMessage}")
      } finally {
        context.stop(self)
      }
  }
}
