package sync.google.process.management.mcc.account.campaign

import Shared.Shared._
import akka.actor.{Actor, Props}
import akka.event.Logging
import com.google.api.ads.adwords.axis.utils.v201609.SelectorBuilder
import com.google.api.ads.adwords.axis.v201609.cm._
import com.google.api.ads.adwords.lib.selectorfields.v201609.cm.{AdGroupBidModifierField, AdGroupField, CampaignCriterionField, CampaignField}
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import helpers.google.mcc.account.campaign.CampaignControllerHelper._
import models.mongodb.google.Google._
import sync.google.adwords.AdWordsHelper
import sync.google.adwords.account.{AdGroupHelper, CampaignHelper}
import sync.shared.Google._

import scala.collection.mutable.ListBuffer
import sync.google.adwords.account.BudgetHelper
import sync.google.process.management.mcc.account.bidding_strategy.BiddingStrategyActor
import sync.google.process.management.mcc.account.campaign.adgroup.{AdGroupActor, AdGroupBidModifierActor}
import sync.google.process.management.mcc.account.campaign.criterion.CampaignCriterionActor

class CampaignActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case campaignDataPullRequest: GoogleCampaignDataPullRequest =>
          try {
            log.info(s"Processing Incoming Data for Campaign (${campaignDataPullRequest.campaignObject.campaign.getId})")

            val campaignObj = googleCampaignCollection.update(
              DBObject(
                "mccObjId" -> campaignDataPullRequest.campaignObject.customerObject.mccObject.mccObjId,
                "customerObjId" -> campaignDataPullRequest.campaignObject.customerObject.customerObjId,
                "customerApiId" -> campaignDataPullRequest.campaignObject.customerObject.managedCustomer.getCustomerId,
                "apiId" -> campaignDataPullRequest.campaignObject.campaign.getId
              ),
              $set(
                "mccObjId" -> campaignDataPullRequest.campaignObject.customerObject.mccObject.mccObjId,
                "customerObjId" -> campaignDataPullRequest.campaignObject.customerObject.customerObjId,
                "customerApiId" -> campaignDataPullRequest.campaignObject.customerObject.managedCustomer.getCustomerId,
                "apiId" -> campaignDataPullRequest.campaignObject.campaign.getId,
                "campaign" -> DBObject(
                "classPath" -> campaignDataPullRequest.campaignObject.campaign.getClass.getCanonicalName,
                "object" -> JSON.parse(gson.toJson(campaignDataPullRequest.campaignObject.campaign)).asInstanceOf[DBObject]
              )),
              true
            )

            val adGroupHelper = new AdGroupHelper(campaignDataPullRequest.adWordsHelper, log)
            if (campaignDataPullRequest.recursivePull) {
              campaignDataPullRequest.campaignObject.campaignObjId = Some(campaignObj.getUpsertedId.asInstanceOf[ObjectId])

              var offset = 0
              var campaignCriterionSelector = (new SelectorBuilder)
                .fields(campaignCriterionFields: _*)
                .offset(offset)
                .equals(CampaignCriterionField.CampaignId, campaignDataPullRequest.campaignObject.campaign.getId.toString)
                .limit(campaignDataPullRequest.adWordsHelper.PAGE_SIZE)
                .build()

              val campaignHelper = new CampaignHelper(campaignDataPullRequest.adWordsHelper, log)

              var campaignCriterion = campaignHelper.getCampaignCriterion(campaignCriterionFields, offset, None, Some(campaignCriterionSelector))
              while(offset < campaignCriterion.getTotalNumEntries) {
                campaignCriterion.getEntries.foreach { criterion =>
                  log.info(s"Processing Campaign Criterion - ${criterion.getCriterion.getId}")
                  googleManagementActorSystem.actorOf(Props(new CampaignCriterionActor)) ! GoogleCampaignCriterionDataPullRequest(
                    campaignDataPullRequest.adWordsHelper,
                    CampaignCriterionObject(
                      campaignDataPullRequest.campaignObject,
                      None,
                      criterion
                    ),
                    pushToExternal = true
                  )
                }
                offset += campaignDataPullRequest.adWordsHelper.PAGE_SIZE
                campaignCriterionSelector = (new SelectorBuilder)
                  .fields(campaignCriterionFields: _*)
                  .offset(offset)
                  .equals(CampaignCriterionField.CampaignId, campaignDataPullRequest.campaignObject.campaign.getId.toString)
                  .limit(campaignDataPullRequest.adWordsHelper.PAGE_SIZE)
                  .build()
                campaignCriterion = campaignHelper.getCampaignCriterion(campaignCriterionFields, offset, None, Some(campaignCriterionSelector))
              }

              offset = 0
              var adGroupBidModifierSelector = (new SelectorBuilder)
                .fields(adGroupBidModifierFields: _*)
                .offset(offset)
                .equals(AdGroupBidModifierField.CampaignId, campaignDataPullRequest.campaignObject.campaign.getId.toString)
                .limit(campaignDataPullRequest.adWordsHelper.PAGE_SIZE)
                .build()
              var bidModifiers = adGroupHelper.getAdGroupBidModifiers(adGroupBidModifierFields, offset, None, Some(adGroupBidModifierSelector))
              while(offset < bidModifiers.getTotalNumEntries) {
                bidModifiers.getEntries.foreach { bidModifier =>
                  log.info(s"Processing Bidmodifier - ")
                  googleManagementActorSystem.actorOf(Props(new AdGroupBidModifierActor)) ! GoogleAdGroupBidModifierDataPullRequest(
                    campaignDataPullRequest.adWordsHelper,
                    AdGroupBidModifierObject(
                      campaignDataPullRequest.campaignObject,
                      None,
                      bidModifier
                    ),
                    pushToExternal = true
                  )
                }
                offset += campaignDataPullRequest.adWordsHelper.PAGE_SIZE
                adGroupBidModifierSelector = (new SelectorBuilder)
                  .fields(adGroupBidModifierFields: _*)
                  .offset(offset)
                  .equals(AdGroupBidModifierField.CampaignId, campaignDataPullRequest.campaignObject.campaign.getId.toString)
                  .limit(campaignDataPullRequest.adWordsHelper.PAGE_SIZE)
                  .build()
                bidModifiers = adGroupHelper.getAdGroupBidModifiers(adGroupBidModifierFields, offset, None, Some(adGroupBidModifierSelector))
              }

              offset = 0
              var adGroupSelector = (new SelectorBuilder)
                .fields(adGroupFields: _*)
                .offset(offset)
                .equals(AdGroupField.CampaignId, campaignDataPullRequest.campaignObject.campaign.getId.toString)
                .limit(campaignDataPullRequest.adWordsHelper.PAGE_SIZE)
                .build()
              var adGroups = adGroupHelper.getAdGroups(adGroupFields, offset, None, Some(adGroupSelector))
              while(offset < bidModifiers.getTotalNumEntries) {
                adGroups.getEntries.foreach { adGroup =>
                  log.info(s"Processing Adgroup - ${adGroup.getId}")
                  googleManagementActorSystem.actorOf(Props(new AdGroupActor)) ! GoogleAdGroupDataPullRequest(
                    adWordsHelper = campaignDataPullRequest.adWordsHelper,
                    adGroupObject = AdGroupObject(
                      campaignDataPullRequest.campaignObject,
                      None,
                      adGroup
                    ),
                    recursivePull = true,
                    pushToExternal = true
                  )
                }
                offset += campaignDataPullRequest.adWordsHelper.PAGE_SIZE
                adGroupSelector = (new SelectorBuilder)
                  .fields(adGroupFields: _*)
                  .offset(offset)
                  .equals(AdGroupField.CampaignId, campaignDataPullRequest.campaignObject.campaign.getId.toString)
                  .limit(campaignDataPullRequest.adWordsHelper.PAGE_SIZE)
                  .build()
                adGroups = adGroupHelper.getAdGroups(adGroupFields, offset, None, Some(adGroupSelector))
              }
            }
          } catch {
            case e: Exception =>
              log.info("Error Retrieving Data for Google Campaign (%s) - %s".format(
                campaignDataPullRequest.campaignObject.campaign.getName,
                e.getMessage
              ))
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
          ).get.asDBObject
        )

        val adWordsHelper = new AdWordsHelper(
          clientId=mcc_data.oAuthClientId,
          clientSecret=mcc_data.oAuthClientSecret,
          refreshToken=mcc_data.oAuthRefreshToken,
          developerToken=mcc_data.developerToken,
          customerId=Some(account_data.getCustomerId.toString)
        )

        val campaignHelper = new CampaignHelper(adWordsHelper, log)
        val budgetHelper = new BudgetHelper(adWordsHelper, log)

        var campaigns = ListBuffer[Campaign]()

        if (cache.changeType != ChangeType.NEW) {
          campaigns = campaigns ++ campaignHelper
            .getCampaigns(
              List(CampaignField.Id, CampaignField.Name),
              0,
              None,
              Some(
                (new SelectorBuilder)
                  .offset(0)
                  .equals(CampaignField.Id, campaign_change.apiId.getOrElse("").toString)
                  .limit(adWordsHelper.PAGE_SIZE)
                  .build
              )
            ).getEntries.toList
        }

        cache.changeType match {
          case ChangeType.DELETE =>
            campaigns.map(campaign => campaignHelper.deleteCampaign(campaign))
          case ChangeType.NEW =>
            // Create a budget first, so that we can add it to the campaign.
            val budget = budgetHelper.newBudget(campaign_change.budgetAmount.getOrElse(0).toDouble, campaign_change.name)
            budgetHelper.createBudget(budget)
            
            val campaign = new Campaign
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
            campaign.setBudget(budget)
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
              
              // Update the budget for this campaign, using the amount from the campaign form
              campaign.getBudget.getAmount.setMicroAmount(dollarsToMicro(campaign_change.budgetAmount.getOrElse(0).toDouble))
              budgetHelper.updateBudget(campaign.getBudget)
            }
        }
      } catch {
        case e: Exception =>
          e.printStackTrace
      } finally {
        context.stop(self)
      }
  }
}