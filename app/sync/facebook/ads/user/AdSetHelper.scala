package sync.facebook.ads.user

import org.apache.pekko.event.LoggingAdapter
// TODO: Update to facebook-java-business-sdk v20
import com.facebook.ads.sdk.{AdAccount, AdSet, AdsInsights, Campaign}
import sync.facebook.ads.FacebookMarketingHelper

import scala.jdk.CollectionConverters._
import scala.collection.mutable

class AdSetHelper(facebookMarketingHelper: FacebookMarketingHelper, log: LoggingAdapter) {
  def getAdSets: List[AdSet] = {
    var result: mutable.ListBuffer[AdSet] = mutable.ListBuffer()
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
    var result: mutable.ListBuffer[AdsInsights] = mutable.ListBuffer()
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
    var result: mutable.ListBuffer[AdSet] = mutable.ListBuffer()
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
    var result: mutable.ListBuffer[AdSet] = mutable.ListBuffer()
    var adSets = AdSet.fetchByIds(adSetIds.asJava, AdAccount.APIRequestGetAdSets.FIELDS.toSeq.asJava, facebookMarketingHelper.context)
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
