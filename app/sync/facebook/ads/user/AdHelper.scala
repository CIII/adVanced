package sync.facebook.ads.user

import org.apache.pekko.event.LoggingAdapter
// TODO: Update to facebook-java-business-sdk v20
import com.facebook.ads.sdk._
import sync.facebook.ads.FacebookMarketingHelper

import scala.jdk.CollectionConverters._
import scala.collection.mutable

class AdHelper(facebookMarketingHelper: FacebookMarketingHelper, log: LoggingAdapter) {
  def getAds: List[Ad] = {
    var result: mutable.ListBuffer[Ad] = mutable.ListBuffer()
    var ads = facebookMarketingHelper.adAccount.getAds.requestAllFields().execute()
    while(!ads.isEmpty) {
      val iter = ads.iterator()
      while (iter.hasNext) {
        result += iter.next()
      }
      ads = ads.nextPage()
    }
    result.toList
  }

  def getAdInsights(ad: Ad) = {
    var result: mutable.ListBuffer[AdsInsights] = mutable.ListBuffer()
    var adsInsights = ad.getInsights.execute()
    while(!adsInsights.isEmpty) {
      val iter = adsInsights.iterator()
      while (iter.hasNext) {
        result += iter.next()
      }
      adsInsights = adsInsights.nextPage()
    }
    result.toList
  }

  def getAdsByAdSetId(adSetId: String): List[Ad] = {
    var result: mutable.ListBuffer[Ad] = mutable.ListBuffer()
    var ads = AdSet.fetchById(adSetId, facebookMarketingHelper.context).getAds.execute()
    while(!ads.isEmpty) {
      val iter = ads.iterator()
      while (iter.hasNext) {
        result += iter.next()
      }
      ads = ads.nextPage()
    }
    result.toList
  }

  def getAdsByIds(adIds: List[String]): List[Ad] = {
    var result: mutable.ListBuffer[Ad] = mutable.ListBuffer()
    var ads = Ad.fetchByIds(adIds.asJava, AdAccount.APIRequestGetAds.FIELDS.toSeq.asJava, facebookMarketingHelper.context)
    while(!ads.isEmpty) {
      val iter = ads.iterator()
      while (iter.hasNext) {
        result += iter.next()
      }
      ads = ads.nextPage()
    }
    result.toList
  }

  def deleteAd(adId: String): Ad.APIRequestDelete = {
    Ad.fetchById(adId, facebookMarketingHelper.context).delete()
  }

  def updateAd(adId: String): Ad.APIRequestUpdate = {
    Ad.fetchById(adId, facebookMarketingHelper.context).update()
  }
}
