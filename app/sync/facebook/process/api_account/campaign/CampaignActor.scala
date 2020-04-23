package sync.facebook.process.api_account.campaign

import Shared.Shared._
import akka.actor.{Actor, Props}
import akka.event.Logging
import com.facebook.ads.sdk.Campaign
import com.mongodb.casbah.Imports.{ObjectId, _}
import com.mongodb.util.JSON
import helpers.facebook.api_account.campaign.CampaignControllerHelper._
import models.mongodb.facebook.Facebook._
import org.bson.types.ObjectId
import sync.facebook.ads.FacebookMarketingHelper
import sync.facebook.ads.user._
import sync.shared.Facebook._

import scala.collection.mutable.ListBuffer

class CampaignActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case campaignDataPullRequest: FacebookCampaignDataPullRequest =>
      try {
        val campaignHelper = new CampaignHelper(campaignDataPullRequest.marketinghelper, log)
        campaignHelper.getCampaigns().foreach { c =>
          val campaignObject = CampaignObject(
            ApiAccountObject(
              campaignDataPullRequest.apiAccountObject.apiAccount._id.get,
              campaignDataPullRequest.apiAccountObject.apiAccount
            ),
            None,
            c
          )
          try {
            log.info("Retrieving Campaign Data (%s)".format(
              campaignObject.campaign.getId
            ))

            val qry = DBObject(
              "apiAccountObjId" -> campaignObject.apiAccountObject.apiAccountObjId,
              "apiId" -> campaignObject.campaign.getId
            )
            val newData = DBObject(
              "classPath" -> campaignObject.campaign.getClass.getCanonicalName,
              "object" -> JSON.parse(gson.toJson(campaignObject.campaign)).asInstanceOf[DBObject]
            )

            facebookCampaignCollection.findAndModify(
              qry,
              null,
              null,
              false,
              DBObject("$set" -> DBObject("campaign" -> newData)),
              true,
              true
            ) match {
              case Some(campaign) =>
                if (campaignDataPullRequest.recursivePull) {
                  campaignObject.campaignObjId = Some(campaign.get("_id").asInstanceOf[ObjectId])
                  facebookManagementActorSystem.actorOf(Props(new sync.facebook.process.api_account.campaign.ad_set.AdSetActor)) ! FacebookAdSetDataPullRequest(
                    campaignDataPullRequest.marketinghelper,
                    campaignObject,
                    true,
                    false
                  )
                }
              case _ => throw new Exception("Error saving Facebook Campaign!")
            }
          } catch {
            case e: Exception =>
              log.info("Error Retrieving Data for Facebook Campaign (%s) - %s".format(
                campaignObject.campaign.getFieldName,
                e.getMessage
              ))
              e.printStackTrace()
          }
        }
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

        val api_account_data = dboToApiAccount(facebookApiAccountCollection.findOne(
          DBObject("_id" -> new ObjectId(campaign_change.parent.apiAccountObjId.get))
        ).get)

        val marketingHelper = new FacebookMarketingHelper(
          api_account_data.accountId,
          api_account_data.applicationSecret,
          api_account_data.accessToken
        )

        val campaignHelper = new CampaignHelper(marketingHelper, log)

        var campaigns = ListBuffer[Campaign]()

        if (cache.changeType != ChangeType.NEW) {
          campaigns = campaigns ++ campaignHelper.getCampaigns
        }

        cache.changeType match {
          case ChangeType.DELETE =>
            campaigns.map(campaign => campaignHelper.deleteCampaign(campaign.getId))
          case ChangeType.NEW =>
          case ChangeType.UPDATE =>
            campaigns.foreach { campaign => }
        }
      } finally {
        context.stop(self)
      }
  }
}