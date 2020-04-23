package sync.google.process.management.mcc.account.campaign.criterion

import Shared.Shared._
import akka.actor.Actor
import sync.shared.Google._
import akka.event.Logging
import com.google.api.ads.adwords.axis.utils.v201609.SelectorBuilder
import com.google.api.ads.adwords.axis.v201609.cm._
import com.google.api.ads.adwords.lib.selectorfields.v201609.cm.{CampaignCriterionField, CampaignField}
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import helpers.google.mcc.account.campaign.CampaignControllerHelper._
import models.mongodb.google.Google._
import sync.google.adwords.AdWordsHelper
import sync.google.adwords.account.CampaignHelper
import sync.shared.Google.{CampaignCriterionObject, GoogleCampaignCriterionDataPullRequest, campaignCriterionFields}

import scala.collection.mutable.ListBuffer

class CampaignCriterionActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case campaignCriterionDataPullRequest: GoogleCampaignCriterionDataPullRequest =>
          try {
            log.info(s"Processing Incoming Campaign Criterion (${campaignCriterionDataPullRequest.campaignCriterionObject.campaignCriterion.getCriterion.getId})")

            googleCampaignCollection.update(
              DBObject(
                "mccObjId" -> campaignCriterionDataPullRequest.campaignCriterionObject.campaignObject.customerObject.mccObject.mccObjId,
                "customerObjId" -> campaignCriterionDataPullRequest.campaignCriterionObject.campaignObject.customerObject.customerObjId,
                "customerApiId" -> campaignCriterionDataPullRequest.campaignCriterionObject.campaignObject.customerObject.managedCustomer.getCustomerId,
                "campaignObjId" -> campaignCriterionDataPullRequest.campaignCriterionObject.campaignObject.campaignObjId,
                "campaignApiId" -> campaignCriterionDataPullRequest.campaignCriterionObject.campaignObject.campaign.getId,
                "apiId" -> campaignCriterionDataPullRequest.campaignCriterionObject.campaignCriterion.getCriterion.getId,
                "criterionType" -> "CampaignCriterion"
              ),
              $set(
                "mccObjId" -> campaignCriterionDataPullRequest.campaignCriterionObject.campaignObject.customerObject.mccObject.mccObjId,
                "customerObjId" -> campaignCriterionDataPullRequest.campaignCriterionObject.campaignObject.customerObject.customerObjId,
                "customerApiId" -> campaignCriterionDataPullRequest.campaignCriterionObject.campaignObject.customerObject.managedCustomer.getCustomerId,
                "campaignObjId" -> campaignCriterionDataPullRequest.campaignCriterionObject.campaignObject.campaignObjId,
                "campaignApiId" -> campaignCriterionDataPullRequest.campaignCriterionObject.campaignObject.campaign.getId,
                "apiId" -> campaignCriterionDataPullRequest.campaignCriterionObject.campaignCriterion.getCriterion.getId,
                "criterionType" -> "CampaignCriterion",
                "criterion" -> DBObject(
                "classPath" -> campaignCriterionDataPullRequest.campaignCriterionObject.campaignCriterion.getClass.getCanonicalName,
                "object" -> JSON.parse(gson.toJson(campaignCriterionDataPullRequest.campaignCriterionObject.campaignCriterion)).asInstanceOf[DBObject]
              )),
              true
            )
          } catch {
            case e: Exception =>
              log.info(s"Error Retrieving Data for Google Campaign Criterion (${campaignCriterionDataPullRequest.campaignCriterionObject.campaignCriterion.getCriterion.getId}) - ${e.getMessage}")
              e.printStackTrace()
          } finally {
            context.stop(self)
          }
    case cache: PendingCacheStructure =>
      try {
      val campaign_change = dboToCampaignForm(cache.changeData.asDBObject)
      log.info("Processing %s -> %s -> %s -> %s".format(
        cache.changeCategory,
        cache.changeType,
        cache.trafficSource,
        cache.id
      ))

      val account_data = gson.fromJson(
        googleCustomerCollection.findOne(
          DBObject(
            "mccObjId" -> new ObjectId(campaign_change.parent.mccObjId.get),
            "apiId" -> campaign_change.parent.customerApiId
          )
        ).get.getAs[String]("customer").get,
        classOf[com.google.api.ads.adwords.axis.v201609.mcm.Customer]
      )

      val mcc_data = dboToMcc(
        googleMccCollection.findOne(
          DBObject("_id" -> new ObjectId(campaign_change.parent.mccObjId.get))
        ).get
      )

      val adWordsHelper = new AdWordsHelper(
        clientId=mcc_data.oAuthClientId,
        clientSecret=mcc_data.oAuthClientSecret,
        refreshToken=mcc_data.oAuthRefreshToken,
        developerToken=mcc_data.developerToken,
        customerId=Some(account_data.getCustomerId.toString)
      )

      val campaignHelper = new CampaignHelper(adWordsHelper, log)

      var campaigns = ListBuffer[Campaign]()

      if (cache.changeType != ChangeType.NEW) {
        campaigns = campaigns ++ campaignHelper
          .getCampaigns(
            List(CampaignField.Id, CampaignField.Name),
            0,
            None,
            Some(
              (new SelectorBuilder)
                .equals(CampaignField.Id, campaign_change.apiId.getOrElse("").toString)
                .build
            )
          ).getEntries.toList
      }

      cache.changeType match {
        case ChangeType.DELETE =>
          campaigns.map(campaign => campaignHelper.deleteCampaign(campaign))
        case ChangeType.NEW =>
          val campaign = new Campaign()
          campaign.setName(campaign_change.name)
          campaign.setStatus(CampaignStatus.fromString(campaign_change.status.getOrElse(campaign.getStatus.toString)))
          campaign.setStartDate(campaign_change.startDate.getOrElse(campaign.getStartDate))
          campaign.setEndDate(campaign_change.endDate.getOrElse(campaign.getEndDate))
          campaign.setAdvertisingChannelType(
            AdvertisingChannelType
              .fromString(
                campaign_change.advertisingChannelType.getOrElse(campaign.getAdvertisingChannelType.toString)
              )
          )
        case ChangeType.UPDATE =>
          campaigns.foreach{campaign =>
            campaign.setName(campaign_change.name)
            campaign.setStatus(CampaignStatus.fromString(campaign_change.status.getOrElse(campaign.getStatus.toString)))
            campaign.setStartDate(campaign_change.startDate.getOrElse(campaign.getStartDate))
            campaign.setEndDate(campaign_change.endDate.getOrElse(campaign.getEndDate))
            campaign.setAdvertisingChannelType(
              AdvertisingChannelType
                .fromString(
                  campaign_change.advertisingChannelType.getOrElse(campaign.getAdvertisingChannelType.toString)
                )
            )
            campaignHelper.updateCampaign(campaign)
          }
      }
    } catch {
      case e: Exception => e.printStackTrace
    } finally {
      context.stop(self)
    }
  }
}
