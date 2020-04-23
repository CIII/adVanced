package sync.facebook.process.api_account.campaign.ad_set.ad

import Shared.Shared._
import akka.actor.Actor
import akka.event.Logging
import com.facebook.ads.sdk.Ad
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import helpers.facebook.api_account.campaign.ad_set.AdSetControllerHelper.dboToAdSetForm
import models.mongodb.facebook.Facebook._
import org.bson.types.ObjectId
import sync.facebook.ads.FacebookMarketingHelper
import sync.facebook.ads.user._
import sync.shared.Facebook._

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

            val qry = DBObject(
              "apiAccountObjId" -> adObject.adSetObject.campaignObject.apiAccountObject.apiAccountObjId,
              "campaignObjId" -> adObject.adSetObject.campaignObject.apiAccountObject.apiAccountObjId,
              "adSetObjId" -> adObject.adSetObject.adSetObjId,
              "apiId" -> adObject.ad.getId
            )

            val newData = DBObject(
              "classPath" -> adObject.ad.getClass.getCanonicalName,
              "object" -> JSON.parse(gson.toJson(adObject.ad)).asInstanceOf[DBObject]
            )
            facebookAdCollection.findAndModify(
              qry,
              null,
              null,
              false,
              DBObject("$set" -> DBObject("ad" -> newData)),
              true,
              true
            )
          } catch {
            case e: Exception =>
              log.info("Error Retrieving Data for Facebook Ad (%s) - %s".format(
                adObject.ad.getFieldName,
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
        val adset_change = dboToAdSetForm(cache.changeData.asDBObject)
        log.info("Processing %s -> %s -> %s -> %s".format(
          cache.changeCategory,
          cache.changeType,
          cache.trafficSource,
          cache.id
        ))

        val api_account_data = dboToApiAccount(facebookApiAccountCollection.findOne(
          DBObject("_id" -> new ObjectId(adset_change.parent.apiAccountObjId.get))
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
