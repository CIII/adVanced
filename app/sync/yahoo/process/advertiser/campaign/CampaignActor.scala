package sync.yahoo.process.advertiser.campaign

import Shared.Shared._
import akka.actor.{Actor, Props}
import akka.event.Logging
import com.mongodb.casbah.Imports._
import models.mongodb.yahoo.Yahoo._
import sync.shared.Yahoo._
import sync.yahoo.gemini.advertiser.AdGroupHelper
import sync.yahoo.process.advertiser.AdvertiserActor
import sync.yahoo.process.advertiser.campaign.adgroup.AdGroupActor

class CampaignActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case campaignDataPullRequest: YahooCampaignDataPullRequest =>
      try {
        log.info("Processing Incoming Data for Campaign (%d)".format(
          campaignDataPullRequest.campaignObject.campaign.apiId.getOrElse(0L)
        ))

        val qry = DBObject(
          "apiAccountObjId" -> campaignDataPullRequest.campaignObject.advertiserObject.apiAccountObject.apiAccountObjId,
          "advertiserObjId" -> campaignDataPullRequest.campaignObject.advertiserObject.advertiserObjId,
          "apiId" -> campaignDataPullRequest.campaignObject.campaign.apiId
        )
        val newData = DBObject(
          "object" -> campaignToDBObject(campaignDataPullRequest.campaignObject.campaign)
        )

        var matchFound = true

        yahooCampaignCollection.findOne(
          qry ++ ("campaign.endTsecs" -> -1)
        ) match {
          case Some(campaignRs) =>
            if (
              !gson.toJson(campaignRs.as[String]("campaign"))
                .equals(gson.toJson(campaignDataPullRequest.campaignObject.campaign))
            ) {
              matchFound = false
              log.debug("Yahoo Campaign match found. Changes detected. Updating...")
            }
          case _ =>
            matchFound = false
            log.debug("No Yahoo Campaign record Found. Inserting...")
        }
        if(!matchFound) {
          yahooCampaignCollection.update(qry, DBObject("$push" -> DBObject("campaign" -> newData)), upsert = true)

          if (campaignDataPullRequest.pushToExternal) {
            //yahooCampaignSyncActor ! (campaignDataPullRequest, endTsecs)
          }
        }

        if(campaignDataPullRequest.recursivePull) {
          campaignDataPullRequest.campaignObject.campaignObjId = yahooCampaignCollection.findOne(qry).get._id

          val adGroupHelper = new AdGroupHelper(campaignDataPullRequest.geminiHelper, log)

          adGroupHelper.getAdGroups().foreach(adGroup =>
            yahooManagementActorSystem.actorOf(Props(new AdGroupActor)) ! YahooAdGroupDataPullRequest(
              geminiHelper = campaignDataPullRequest.geminiHelper,
              adGroupObject = AdGroupObject(
                campaignDataPullRequest.campaignObject,
                None,
                adGroup
              ),
              recursivePull = true,
              pushToExternal = true
            )
          )
        }
      } catch {
        case e: Exception =>
          log.info("Error Retrieving Data for Yahoo Campaign (%s) - %s".format(
            campaignDataPullRequest.campaignObject.campaign.campaignName,
            e.getMessage
          ))
          e.printStackTrace()
      } finally {
        context.stop(self)
      }
    case cache: PendingCacheStructure =>
      /*val campaign_change = dboToCampaignForm(cache.changeData.asDBObject)
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
          ),
          DBObject("$slice" -> -1)
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

      var campaigns = ListBuffer[Campaign]()

      if (cache.changeType != ChangeType.NEW) {
        campaigns = campaigns ++ campaignHelper
          .getCampaigns(
            List(CampaignField.Id, CampaignField.Name),
            None,
            Some(
              (new SelectorBuilder)
                .equals(CampaignField.Id, campaign_change.apiId.getOrElse("").toString)
                .build
            )
          )
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
      }*/
  }
}