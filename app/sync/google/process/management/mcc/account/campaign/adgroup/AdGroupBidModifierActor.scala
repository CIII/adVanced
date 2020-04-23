package sync.google.process.management.mcc.account.campaign.adgroup

import Shared.Shared._
import akka.actor.Actor
import akka.event.Logging
import com.google.api.ads.adwords.axis.utils.v201609.SelectorBuilder
import com.google.api.ads.adwords.lib.selectorfields.v201609.cm.AdGroupBidModifierField
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import models.mongodb.google.Google._
import sync.google.adwords.account.AdGroupHelper
import sync.shared.Google.{AdGroupBidModifierObject, GoogleAdGroupBidModifierDataPullRequest, adGroupBidModifierFields, _}

class AdGroupBidModifierActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case adGroupBidModifierDataPullRequest: GoogleAdGroupBidModifierDataPullRequest =>
      try {
        log.info(s"Processing Incoming Ad Group Bid Modifier (${adGroupBidModifierDataPullRequest.adGroupBidModifierObject.adGroupBidModifier.getCriterion.getId})")

        googleBidModifierCollection.update(
          DBObject(
            "mccObjId" -> adGroupBidModifierDataPullRequest.adGroupBidModifierObject.campaignObject.customerObject.mccObject.mccObjId,
            "customerObjId" -> adGroupBidModifierDataPullRequest.adGroupBidModifierObject.campaignObject.customerObject.customerObjId,
            "customerApiId" -> adGroupBidModifierDataPullRequest.adGroupBidModifierObject.campaignObject.customerObject.managedCustomer.getCustomerId,
            "campaignObjId" -> adGroupBidModifierDataPullRequest.adGroupBidModifierObject.campaignObject.campaignObjId,
            "campaignApiId" -> adGroupBidModifierDataPullRequest.adGroupBidModifierObject.campaignObject.campaign.getId,
            "ApiId" -> adGroupBidModifierDataPullRequest.adGroupBidModifierObject.adGroupBidModifier.getCriterion.getId,
            "bidModifierType" -> "adGroup"
          ),
          $set(
            "mccObjId" -> adGroupBidModifierDataPullRequest.adGroupBidModifierObject.campaignObject.customerObject.mccObject.mccObjId,
            "customerObjId" -> adGroupBidModifierDataPullRequest.adGroupBidModifierObject.campaignObject.customerObject.customerObjId,
            "customerApiId" -> adGroupBidModifierDataPullRequest.adGroupBidModifierObject.campaignObject.customerObject.managedCustomer.getCustomerId,
            "campaignObjId" -> adGroupBidModifierDataPullRequest.adGroupBidModifierObject.campaignObject.campaignObjId,
            "campaignApiId" -> adGroupBidModifierDataPullRequest.adGroupBidModifierObject.campaignObject.campaign.getId,
            "ApiId" -> adGroupBidModifierDataPullRequest.adGroupBidModifierObject.adGroupBidModifier.getCriterion.getId,
            "bidModifierType" -> "adGroup",
            "bidModifier" -> DBObject(
              "classPath" -> adGroupBidModifierDataPullRequest.adGroupBidModifierObject.adGroupBidModifier.getClass.getCanonicalName,
              "object" -> JSON.parse(gson.toJson(adGroupBidModifierDataPullRequest.adGroupBidModifierObject.adGroupBidModifier)).asInstanceOf[DBObject]
            )
          ),
          true
        )
      } catch {
        case e: Exception =>
          log.info("Error Retrieving Data for AdGroup Bid Modifier (%s) - %s".format(
            adGroupBidModifierDataPullRequest.adGroupBidModifierObject.adGroupBidModifier.getCriterion.getId,
            e.getMessage
          ))
          e.printStackTrace()
      } finally {
        context.stop(self)
      }
  }
}