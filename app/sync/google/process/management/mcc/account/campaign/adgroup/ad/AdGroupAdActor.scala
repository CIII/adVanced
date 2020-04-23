package sync.google.process.management.mcc.account.campaign.adgroup.ad

import Shared.Shared._
import akka.actor.Actor
import akka.event.Logging
import com.google.api.ads.adwords.axis.utils.v201609.SelectorBuilder
import com.google.api.ads.adwords.axis.v201609.cm._
import com.google.api.ads.adwords.lib.selectorfields.v201609.cm.AdGroupAdField
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import controllers.Google.AdGroupAdForm
import helpers.google.mcc.account.campaign.adgroup.ad.ImageAdControllerHelper._
import helpers.google.mcc.account.campaign.adgroup.ad.TextAdControllerHelper
import helpers.google.mcc.account.campaign.adgroup.ad.TextAdControllerHelper._
import models.mongodb.google.Google._
import org.bson.types.ObjectId
import sync.google.adwords.AdWordsHelper
import sync.google.adwords.account.AdGroupAdHelper
import sync.shared.Google.{GoogleAdGroupAdDataPullRequest, _}

import scala.collection.mutable.ListBuffer

class AdGroupAdActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case adGroupAdDataPullRequest: GoogleAdGroupAdDataPullRequest =>
      try {
        log.info("Processing Incoming Data for AdGroupAd (%s)".format(
          adGroupAdDataPullRequest.adGroupAdObject.adGroupAd.getAd.getId
        ))

        googleAdCollection.update(
          DBObject(
            "mccObjId" -> adGroupAdDataPullRequest.adGroupAdObject.adGroupObject.campaignObject.customerObject.mccObject.mccObjId,
            "customerObjId" -> adGroupAdDataPullRequest.adGroupAdObject.adGroupObject.campaignObject.customerObject.customerObjId,
            "campaignObjId" -> adGroupAdDataPullRequest.adGroupAdObject.adGroupObject.campaignObject.campaignObjId,
            "adGroupObjId" -> adGroupAdDataPullRequest.adGroupAdObject.adGroupObject.adGroupObjId.get,
            "apiId" -> adGroupAdDataPullRequest.adGroupAdObject.adGroupAd.getAd.getId,
            "adType" -> adGroupAdDataPullRequest.adGroupAdObject.adGroupAd.getAd.getAdType
          ),
          $set(
            "mccObjId" -> adGroupAdDataPullRequest.adGroupAdObject.adGroupObject.campaignObject.customerObject.mccObject.mccObjId,
            "customerObjId" -> adGroupAdDataPullRequest.adGroupAdObject.adGroupObject.campaignObject.customerObject.customerObjId,
            "campaignObjId" -> adGroupAdDataPullRequest.adGroupAdObject.adGroupObject.campaignObject.campaignObjId,
            "adGroupObjId" -> adGroupAdDataPullRequest.adGroupAdObject.adGroupObject.adGroupObjId.get,
            "apiId" -> adGroupAdDataPullRequest.adGroupAdObject.adGroupAd.getAd.getId,
            "adType" -> adGroupAdDataPullRequest.adGroupAdObject.adGroupAd.getAd.getAdType,
            "ad" -> DBObject(
            "classPath" -> adGroupAdDataPullRequest.adGroupAdObject.adGroupAd.getClass.getCanonicalName,
            "object" -> JSON.parse(gson.toJson(adGroupAdDataPullRequest.adGroupAdObject.adGroupAd)).asInstanceOf[DBObject]
          )),
          true,
          true
        )
      } catch {
        case e: Exception =>
          log.info("Error Retrieving Data for Google AdGroup Ad Actor (%s) - %s".format(
            adGroupAdDataPullRequest.adGroupAdObject.adGroupAd.getAd.getId,
            e.getMessage
          ))
          e.printStackTrace()
      } finally {
        context.stop(self)
      }
    case cache: PendingCacheStructure =>
      try {
        val adgroupad_change = cache.changeCategory match {
          case ChangeCategory.IMAGE_AD =>
            dboToImageAdForm(cache.changeData.asDBObject)
          case ChangeCategory.TEXT_AD =>
            dboToTextAdForm(cache.changeData.asDBObject)
          case ChangeCategory.MOBILE_AD =>
        }

        log.info("Processing AdGroupAd %s -> %s -> %s -> %s".format(
          cache.changeCategory,
          cache.changeType,
          cache.trafficSource,
          cache.id
        ))

        val account_data = gson.fromJson(
          googleCustomerCollection.findOne(
            DBObject(
              "mccObjId" -> new ObjectId(adgroupad_change.asInstanceOf[AdGroupAdForm].parent.mccObjId.get),
              "apiId" -> adgroupad_change.asInstanceOf[AdGroupAdForm].parent.customerApiId
            )
          ).get.getAs[String]("customer").get,
          classOf[com.google.api.ads.adwords.axis.v201609.mcm.Customer]
        )

        val mcc_data = dboToMcc(
          googleMccCollection.findOne(
            DBObject("_id" -> new ObjectId(adgroupad_change.asInstanceOf[AdGroupAdForm].parent.mccObjId.get))
          ).get.asDBObject
        )

        val adWordsHelper = new AdWordsHelper(
          clientId = mcc_data.oAuthClientId,
          clientSecret = mcc_data.oAuthClientSecret,
          refreshToken = mcc_data.oAuthRefreshToken,
          developerToken = mcc_data.developerToken,
          customerId = Some(account_data.getCustomerId.toString)
        )

        val adGroupAdHelper = new AdGroupAdHelper(adWordsHelper, log)

        var adGroupAds = ListBuffer[AdGroupAd]()

        if (cache.changeType != ChangeType.NEW) {
          adGroupAds = adGroupAds ++ adGroupAdHelper
            .getAdGroupAds(
              List(AdGroupAdField.Id, AdGroupAdField.Name),
              0,
              None,
              Some(
                (new SelectorBuilder)
                  .offset(0)
                  .equals(AdGroupAdField.Id, adgroupad_change.asInstanceOf[AdGroupAdForm].apiId.getOrElse("").toString)
                  .limit(adWordsHelper.PAGE_SIZE)
                  .build
              )
            ).getEntries.toList
        }

        cache.changeType match {
          case ChangeType.DELETE =>
            adGroupAds.map(adGroupAd => adGroupAdHelper.deleteAdGroupAd(adGroupAd))
          case ChangeType.NEW =>
            val adGroupAd = new AdGroupAd()
            val ad = new Ad()
            adgroupad_change match {
              case text_ad: TextAdControllerHelper.TextAdForm =>
                ad.setAdType("TextAd")
                ad.setDevicePreference(text_ad.devicePreference.getOrElse(0).asInstanceOf[Long])
                ad.setDisplayUrl(text_ad.displayUrl.getOrElse(""))
                ad.setUrl(text_ad.url.getOrElse(""))
            }
            adGroupAd.setAd(ad)
            adGroupAd.setAdGroupId(adgroupad_change.asInstanceOf[AdGroupAdForm].parent.adGroupApiId.get)
            adGroupAd.setStatus(AdGroupAdStatus.fromString(adgroupad_change.asInstanceOf[AdGroupAdForm].status.getOrElse(AdGroupAdStatus.ENABLED.toString)))
            adGroupAdHelper.createAdGroupAd(adGroupAd)
          case ChangeType.UPDATE =>
            adGroupAds.foreach { adGroupAd =>
              val ad = new Ad()
              adgroupad_change match {
                case text_ad: TextAdControllerHelper.TextAdForm =>
                  ad.setAdType("TextAd")
                  ad.setDevicePreference(text_ad.devicePreference.getOrElse(0).asInstanceOf[Long])
                  ad.setDisplayUrl(text_ad.displayUrl.getOrElse(""))
                  ad.setUrl(text_ad.url.getOrElse(""))
              }
              adGroupAd.setAd(ad)
              adGroupAd.setAdGroupId(adgroupad_change.asInstanceOf[AdGroupAdForm].parent.adGroupApiId.get)
              adGroupAd.setStatus(AdGroupAdStatus.fromString(adgroupad_change.asInstanceOf[AdGroupAdForm].status.getOrElse(AdGroupAdStatus.ENABLED.toString)))
              adGroupAdHelper.updateAdGroupAd(adGroupAd)
            }
        }
        } catch {
          case e: Exception => e.printStackTrace
        } finally {
          context.stop(self)
        }
  }
}