package sync.google.process.management.mcc.account.campaign.adgroup

import Shared.Shared._
import akka.actor.{Actor, Props}
import akka.event.Logging
import com.google.api.ads.adwords.axis.utils.v201609.SelectorBuilder
import com.google.api.ads.adwords.axis.v201609.cm._
import com.google.api.ads.adwords.lib.selectorfields.v201609.cm.{AdGroupAdField, AdGroupCriterionField, AdGroupField}
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import helpers.google.mcc.account.campaign.adgroup.AdGroupControllerHelper._
import models.mongodb.google.Google._
import org.bson.types.ObjectId
import sync.google.adwords.AdWordsHelper
import sync.google.adwords.account.{AdGroupAdHelper, AdGroupCriterionHelper, AdGroupHelper}
import sync.google.process.management.mcc.account.campaign.adgroup.ad.AdGroupAdActor
import sync.google.process.management.mcc.account.campaign.adgroup.criterion.keyword.AdGroupCriterionActor
import sync.shared.Google.{GoogleAdGroupCriterionDataPullRequest, _}

import scala.collection.mutable.ListBuffer


class AdGroupActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case adGroupDataPullRequest: GoogleAdGroupDataPullRequest =>
          try {
            log.info("Retrieving AdGroup Data (%s)".format(
              adGroupDataPullRequest.adGroupObject.adGroup.getId
            ))

            val adGroupObj = googleAdGroupCollection.update(
              DBObject(
                "mccObjId" -> adGroupDataPullRequest.adGroupObject.campaignObject.customerObject.mccObject.mccObjId,
                "customerObjId" -> adGroupDataPullRequest.adGroupObject.campaignObject.customerObject.customerObjId,
                "campaignObjId" -> adGroupDataPullRequest.adGroupObject.campaignObject.campaignObjId.get,
                "apiId" -> adGroupDataPullRequest.adGroupObject.adGroup.getId
              ),
              $set(
                "mccObjId" -> adGroupDataPullRequest.adGroupObject.campaignObject.customerObject.mccObject.mccObjId,
                "customerObjId" -> adGroupDataPullRequest.adGroupObject.campaignObject.customerObject.customerObjId,
                "campaignObjId" -> adGroupDataPullRequest.adGroupObject.campaignObject.campaignObjId.get,
                "apiId" -> adGroupDataPullRequest.adGroupObject.adGroup.getId,
                "adGroup" -> DBObject(
                  "classPath" -> adGroupDataPullRequest.adGroupObject.adGroup.getClass.getCanonicalName,
                  "object" -> JSON.parse(gson.toJson(adGroupDataPullRequest.adGroupObject.adGroup)).asInstanceOf[DBObject]
                )
              ),
              true
            )
            if (adGroupDataPullRequest.recursivePull) {
              adGroupDataPullRequest.adGroupObject.adGroupObjId = Some(adGroupObj.getUpsertedId.asInstanceOf[ObjectId])
              val adHelper = new AdGroupAdHelper(adGroupDataPullRequest.adWordsHelper, log)
              val keywordHelper = new AdGroupCriterionHelper(adGroupDataPullRequest.adWordsHelper, log)
              var offset = 0

              var adSelector = (new SelectorBuilder)
                .fields(adGroupAdFields: _*)
                .offset(offset)
                .equals(AdGroupAdField.AdGroupId, adGroupDataPullRequest.adGroupObject.adGroup.getId.toString)
                .limit(adGroupDataPullRequest.adWordsHelper.PAGE_SIZE)
                .build()
              var adGroupAds = adHelper.getAdGroupAds(adGroupAdFields, offset, None, Some(adSelector))
              while(offset < adGroupAds.getTotalNumEntries) {
                adGroupAds.getEntries.foreach { ad =>
                  log.info(s"Processing Adgroup Ad - ${ad.getAd.getId}")
                  googleManagementActorSystem.actorOf(Props(new AdGroupAdActor)) ! GoogleAdGroupAdDataPullRequest(
                    adWordsHelper = adGroupDataPullRequest.adWordsHelper,
                    adGroupAdObject = AdGroupAdObject(
                      adGroupObject = adGroupDataPullRequest.adGroupObject,
                      adGroupAdObjId = None,
                      adGroupAd = ad
                    ),
                    pushToExternal = true
                  )
                }
                offset += adGroupDataPullRequest.adWordsHelper.PAGE_SIZE
                adSelector = (new SelectorBuilder)
                  .fields(adGroupAdFields: _*)
                  .offset(offset)
                  .equals(AdGroupAdField.AdGroupId, adGroupDataPullRequest.adGroupObject.adGroup.getId.toString)
                  .limit(adGroupDataPullRequest.adWordsHelper.PAGE_SIZE)
                  .build()
                adGroupAds = adHelper.getAdGroupAds(adGroupAdFields, offset, None, Some(adSelector))
              }


              offset = 0
              var keywordSelector = (new SelectorBuilder)
                .fields(adGroupCriterionFields: _*)
                .offset(offset)
                .equals(AdGroupCriterionField.AdGroupId, adGroupDataPullRequest.adGroupObject.adGroup.getId.toString)
                .limit(adGroupDataPullRequest.adWordsHelper.PAGE_SIZE)
                .build()
              var adGroupCriterion = keywordHelper.getAdGroupCriterion(adGroupCriterionFields, offset, None, Some(keywordSelector))
              while(offset < adGroupCriterion.getTotalNumEntries) {
                adGroupCriterion.getEntries.foreach { criterion =>
                  log.info(s"Processing Adgroup Criterion - ${criterion.getCriterion.getId}")
                  googleManagementActorSystem.actorOf(Props(new AdGroupCriterionActor)) ! GoogleAdGroupCriterionDataPullRequest(
                    adGroupDataPullRequest.adWordsHelper,
                    adGroupCriterionObject = AdGroupCriterionObject(
                      adGroupObject = adGroupDataPullRequest.adGroupObject,
                      adGroupCriterionObjId = None,
                      adGroupCriterion = criterion
                    ),
                    pushToExternal = true
                  )
                }
                offset += adGroupDataPullRequest.adWordsHelper.PAGE_SIZE
                keywordSelector = (new SelectorBuilder)
                  .fields(adGroupCriterionFields: _*)
                  .offset(offset)
                  .equals(AdGroupCriterionField.AdGroupId, adGroupDataPullRequest.adGroupObject.adGroup.getId.toString)
                  .limit(adGroupDataPullRequest.adWordsHelper.PAGE_SIZE)
                  .build()
                adGroupCriterion = keywordHelper.getAdGroupCriterion(adGroupCriterionFields, 0, None, Some(keywordSelector))
              }
            }
          } catch {
            case e: Exception =>
              log.info("Error Retrieving Data for Google AdGroup (%s) - %s".format(
                adGroupDataPullRequest.adGroupObject.adGroup.getName,
                e.getMessage
              ))
              e.printStackTrace()
          } finally {
            context.stop(self)
          }
    case cache: PendingCacheStructure =>
      try {
        val adgroup_change = dboToAdGroupForm(cache.changeData.asDBObject)
        log.info("Processing Pending Cache %s -> %s -> %s -> %s".format(
          cache.changeCategory,
          cache.changeType,
          cache.trafficSource,
          cache.id
        ))

        val account_data = gson.fromJson(
          googleCustomerCollection.findOne(
            DBObject(
              "mccObjId" -> new ObjectId(adgroup_change.parent.mccObjId.get),
              "apiId" -> adgroup_change.parent.customerApiId
            )
          ).get.getAs[String]("customer").get,
          classOf[com.google.api.ads.adwords.axis.v201609.mcm.Customer]
        )

        val mcc_data = dboToMcc(
          googleMccCollection.findOne(
            DBObject("_id" -> new ObjectId(adgroup_change.parent.mccObjId.get))
          ).get
        )

        val adWordsHelper = new AdWordsHelper(
          clientId=mcc_data.oAuthClientId,
          clientSecret=mcc_data.oAuthClientSecret,
          refreshToken=mcc_data.oAuthRefreshToken,
          developerToken=mcc_data.developerToken,
          customerId=Some(account_data.getCustomerId.toString)
        )

        val adGroupHelper = new AdGroupHelper(adWordsHelper, log)

        var adgroups = ListBuffer[AdGroup]()

        if (cache.changeType != ChangeType.NEW) {
          adgroups = adgroups ++ adGroupHelper
            .getAdGroups(
              List(AdGroupField.Id, AdGroupField.Name),
              0,
              None,
              Some(
                (new SelectorBuilder)
                  .offset(0)
                  .equals(AdGroupField.Id, adgroup_change.apiId.getOrElse("").toString)
                  .limit(adWordsHelper.PAGE_SIZE)
                  .build
              )
            ).getEntries.toList
        }

        cache.changeType match {
          case ChangeType.DELETE =>
            adgroups.map(adgroup => adGroupHelper.deleteAdGroup(adgroup))
          case ChangeType.NEW =>
            val adgroup = new AdGroup()
            adgroup.setName(adgroup_change.name)
            adgroup.setCampaignId(adgroup_change.parent.campaignApiId.get)
            if(adgroup_change.contentBidCriterionTypeGroup.nonEmpty) {
              adgroup.setContentBidCriterionTypeGroup(CriterionTypeGroup.fromString(adgroup_change.contentBidCriterionTypeGroup.getOrElse("UNKNOWN")))
            }
            adgroup.setStatus(AdGroupStatus.fromString(adgroup_change.status))
            adGroupHelper.createAdGroup(adgroup)
          case ChangeType.UPDATE =>
            adgroups.foreach{adgroup =>
              adgroup.setName(adgroup_change.name)
              adgroup.setStatus(AdGroupStatus.fromString(adgroup_change.status))
              if(adgroup_change.maxCpc.nonEmpty){
                val bsc: BiddingStrategyConfiguration  = new BiddingStrategyConfiguration
                val bid: CpcBid = new CpcBid
                val money: Money = new Money
                money.setMicroAmount(dollarsToMicro(adgroup_change.maxCpc.get))
                bid.setBid(money)
                bsc.setBids(Array(bid))
                adgroup.setBiddingStrategyConfiguration(bsc)
              }
              if(adgroup_change.contentBidCriterionTypeGroup.nonEmpty) {
                adgroup.setContentBidCriterionTypeGroup(CriterionTypeGroup.fromString(adgroup_change.contentBidCriterionTypeGroup.getOrElse("UNKNOWN")))
              }
              adGroupHelper.updateAdGroup(adgroup)
            }
        }
      } catch {
        case e: Exception => e.printStackTrace
      } finally {
        context.stop(self)
      }
  }
}