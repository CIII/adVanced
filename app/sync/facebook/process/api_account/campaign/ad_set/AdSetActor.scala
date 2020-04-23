package sync.facebook.process.api_account.campaign.ad_set

import Shared.Shared._
import akka.actor.{Actor, Props}
import akka.event.Logging
import com.facebook.ads.sdk.AdSet
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import helpers.facebook.api_account.campaign.ad_set.AdSetControllerHelper._
import models.mongodb.facebook.Facebook._
import org.bson.types.ObjectId
import sync.facebook.ads.FacebookMarketingHelper
import sync.facebook.ads.user._
import sync.facebook.process.api_account.campaign.ad_set.ad.AdActor
import sync.shared.Facebook._

import scala.collection.mutable.ListBuffer

class AdSetActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case adSetDataPullRequest: FacebookAdSetDataPullRequest =>
      try {
        val adSetHelper = new AdSetHelper(adSetDataPullRequest.marketinghelper, log)
        adSetHelper.getAdSets.foreach { adSet =>
          val adSetObject = AdSetObject(
            campaignObject = adSetDataPullRequest.campaignObject,
            adSetObjId = None,
            adSet = adSet
          )
          try {
            log.info("Retrieving AdSet Data (%s)".format(
              adSetObject.adSet.getId
            ))

            val qry = DBObject(
              "apiAccountObjId" -> adSetObject.campaignObject.apiAccountObject.apiAccountObjId,
              "campaignObjId" -> adSetObject.campaignObject.apiAccountObject.apiAccountObjId,
              "apiId" -> adSetObject.adSet.getId
            )

            val newData = DBObject(
              "classPath" -> adSetObject.adSet.getClass.getCanonicalName,
              "object" -> JSON.parse(gson.toJson(adSetObject.adSet)).asInstanceOf[DBObject]
            )
            facebookAdSetCollection.findAndModify(
              qry,
              null,
              null,
              false,
              DBObject("$set" -> DBObject("adSet" -> newData)),
              true,
              true
            ) match {
              case Some(as) =>
                if (adSetDataPullRequest.recursivePull) {
                  adSetObject.adSetObjId = Some(as.get("_id").asInstanceOf[ObjectId])
                  facebookManagementActorSystem.actorOf(Props(new AdActor)) ! FacebookAdDataPullRequest(
                    marketinghelper = adSetDataPullRequest.marketinghelper,
                    adSetObject,
                    true,
                    false
                  )
                }
              case _ => throw new Exception("Error saving AdSet!")
            }
          } catch {
            case e: Exception =>
              log.info("Error Retrieving Data for Facebook AdSet (%s) - %s".format(
                adSetObject.adSet.getFieldName,
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

        val adSetHelper = new AdSetHelper(marketingHelper, log)

        var adsets = ListBuffer[AdSet]()

        if (cache.changeType != ChangeType.NEW) {
          adsets = adsets ++ adSetHelper.getAdSets
        }

        cache.changeType match {
          case ChangeType.DELETE =>
            adsets.map(adset => adSetHelper.deleteAdSet(adset.getId))
          case ChangeType.NEW =>
          case ChangeType.UPDATE =>
            adsets.foreach { adset => }
        }
      } finally {
        context.stop(self)
      }
  }
}
