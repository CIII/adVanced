package sync.facebook.process.api_account.campaign

import Shared.Shared._
import org.apache.pekko.actor.{Actor, Props}
import org.apache.pekko.event.Logging
// TODO: Update to facebook-java-business-sdk v20
import com.facebook.ads.sdk.Campaign
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import com.mongodb.client.model.{FindOneAndUpdateOptions, ReturnDocument}
import helpers.facebook.api_account.campaign.CampaignControllerHelper._
import models.mongodb.MongoExtensions._
import models.mongodb.facebook.Facebook._
import org.bson.types.ObjectId
import sync.facebook.ads.FacebookMarketingHelper
import sync.facebook.ads.user._
import sync.shared.Facebook._
import models.mongodb.MongoExtensions._

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

            val qry = Document(
              "apiAccountObjId" -> campaignObject.apiAccountObject.apiAccountObjId,
              "apiId" -> campaignObject.campaign.getId
            )
            val newData = Document(
              "classPath" -> campaignObject.campaign.getClass.getCanonicalName,
              "object" -> Document(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(campaignObject.campaign))
            )

            facebookCampaignCollection.findOneAndUpdateSync(
              qry,
              Document("$set" -> Document("campaign" -> newData)),
              new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
            ) match {
              case Some(campaign) =>
                if (campaignDataPullRequest.recursivePull) {
                  val updatedCampaignObject = campaignObject.copy(campaignObjId = Some(campaign.get("_id").asInstanceOf[ObjectId]))
                  context.system.actorOf(Props(new sync.facebook.process.api_account.campaign.ad_set.AdSetActor)) ! FacebookAdSetDataPullRequest(
                    campaignDataPullRequest.marketinghelper,
                    updatedCampaignObject,
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
              log.error(s"Error processing Facebook campaign: ${e.getMessage}")
          }
        }
      } finally {
        context.stop(self)
      }
    case cache: PendingCacheStructure =>
      try {
        val campaign_change = documentToCampaignForm(cache.changeData)
        log.info("Processing %s -> %s -> %s -> %s".format(
          cache.changeCategory,
          cache.changeType,
          cache.trafficSource,
          cache.id
        ))

        val api_account_data = documentToApiAccount(facebookApiAccountCollection.findOne(
          Document("accountId" -> campaign_change.accountId)
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