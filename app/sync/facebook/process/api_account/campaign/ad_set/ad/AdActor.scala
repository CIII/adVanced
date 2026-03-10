package sync.facebook.process.api_account.campaign.ad_set.ad

import Shared.Shared._
import org.apache.pekko.actor.Actor
import org.apache.pekko.event.Logging
// TODO: Update to facebook-java-business-sdk v20
import com.facebook.ads.sdk.Ad
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import com.mongodb.client.model.{FindOneAndUpdateOptions, ReturnDocument}
import helpers.facebook.api_account.campaign.ad_set.AdSetControllerHelper.documentToAdSetForm
import models.mongodb.facebook.Facebook._
import org.bson.types.ObjectId
import sync.facebook.ads.FacebookMarketingHelper
import sync.facebook.ads.user._
import sync.shared.Facebook._
import models.mongodb.MongoExtensions._

import scala.collection.mutable.ListBuffer

class AdActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case adDataPullRequest: FacebookAdDataPullRequest =>
      try {
        val adHelper = new AdHelper(adDataPullRequest.marketinghelper, log)
        adHelper.getAdsByAdSetId(adDataPullRequest.adSetObject.adSet.getId).foreach { ad =>
          val adObject = AdObject(
            adSetObject = adDataPullRequest.adSetObject,
            adObjId = None,
            ad = ad
          )
          try {
            log.info("Retrieving Ad Data (%s)".format(
              adObject.ad.getId
            ))

            val qry = Document(
              "apiAccountObjId" -> adObject.adSetObject.campaignObject.apiAccountObject.apiAccountObjId,
              "campaignObjId" -> adObject.adSetObject.campaignObject.apiAccountObject.apiAccountObjId,
              "adSetObjId" -> adObject.adSetObject.adSetObjId,
              "apiId" -> adObject.ad.getId
            )

            val newData = Document(
              "classPath" -> adObject.ad.getClass.getCanonicalName,
              "object" -> Document(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(adObject.ad))
            )
            facebookAdCollection.findOneAndUpdateSync(
              qry,
              Document("$set" -> Document("ad" -> newData)),
              new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
            )
          } catch {
            case e: Exception =>
              log.info("Error Retrieving Data for Facebook Ad (%s) - %s".format(
                adObject.ad.getFieldName,
                e.getMessage
              ))
              log.error(s"Error processing Facebook ad: ${e.getMessage}")
          }
        }
      } finally {
        context.stop(self)
      }
    case cache: PendingCacheStructure =>
      try {
        val adset_change = documentToAdSetForm(cache.changeData)
        log.info("Processing %s -> %s -> %s -> %s".format(
          cache.changeCategory,
          cache.changeType,
          cache.trafficSource,
          cache.id
        ))

        val api_account_data = documentToApiAccount(facebookApiAccountCollection.findOne(
          Document("_id" -> new ObjectId(adset_change.parent.apiAccountObjId.get))
        ).get)

        val marketingHelper = new FacebookMarketingHelper(
          api_account_data.accountId,
          api_account_data.applicationSecret,
          api_account_data.accessToken
        )

        val adHelper = new AdHelper(marketingHelper, log)

        var ads = ListBuffer[Ad]()

        if (cache.changeType != ChangeType.NEW) {
          ads = ads ++ adHelper.getAds
        }

        cache.changeType match {
          case ChangeType.DELETE =>
            ads.map(ad => adHelper.deleteAd(ad.getId))
          case ChangeType.NEW =>
          case ChangeType.UPDATE =>
            ads.foreach { ad => }
        }
      } finally {
        context.stop(self)
      }
  }
}
