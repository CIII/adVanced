package sync.facebook.ads.user

import akka.event.LoggingAdapter
import com.facebook.ads.sdk.{AdAccount, AdSet, AdsInsights, Campaign}
import sync.facebook.ads.FacebookMarketingHelper

import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.mutable

class AdSetHelper(facebookMarketingHelper: FacebookMarketingHelper, log: LoggingAdapter) {
  def getAdSets: List[AdSet] = {
    var result: mutable.MutableList[AdSet] = mutable.MutableList()
    var adSets = facebookMarketingHelper.adAccount.getAdSets.requestAllFields().execute()
    while(!adSets.isEmpty) {
      val iter = adSets.iterator()
      while (iter.hasNext) {
        result += iter.next()
      }
      adSets = adSets.nextPage()
    }
    result.toList
  }

  def getAdSetAdsInsights(adSet: AdSet) = {
    var result: mutable.MutableList[AdsInsights] = mutable.MutableList()
    var adsInsights = adSet.getInsights.execute()
    while(!adsInsights.isEmpty) {
      val iter = adsInsights.iterator()
      while (iter.hasNext) {
        result += iter.next()
      }
      adsInsights = adsInsights.nextPage()
    }
    result.toList
  }

  def getAdSetsByCampaignId(campaignId: Long): List[AdSet] = {
    var result: mutable.MutableList[AdSet] = mutable.MutableList()
    var adSets = Campaign.fetchById(campaignId, facebookMarketingHelper.context).getAdSets.execute()
    while(!adSets.isEmpty) {
      val iter = adSets.iterator()
      while (iter.hasNext) {
        result += iter.next()
      }
      adSets = adSets.nextPage()
    }
    result.toList
  }

  def getAdSetsByIds(adSetIds: List[String]): List[AdSet] = {
    var result: mutable.MutableList[AdSet] = mutable.MutableList()
    var adSets = AdSet.fetchByIds(adSetIds, seqAsJavaList(AdAccount.APIRequestGetAdSets.FIELDS.toSeq), facebookMarketingHelper.context)
    while(!adSets.isEmpty) {
      val iter = adSets.iterator()
      while (iter.hasNext) {
        result += iter.next()
      }
      adSets = adSets.nextPage()
    }
    result.toList
  }

  def deleteAdSet(adSetId: String): AdSet.APIRequestDelete = {
    AdSet.fetchById(adSetId, facebookMarketingHelper.context).delete()
  }

  def updateAdSet(adSetId: String): AdSet.APIRequestUpdate = {
    AdSet.fetchById(adSetId, facebookMarketingHelper.context).update()
  }
}
