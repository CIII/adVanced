package sync.facebook.ads.user

import akka.event.LoggingAdapter
import com.facebook.ads.sdk._
import sync.facebook.ads.FacebookMarketingHelper

import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.mutable

class AdHelper(facebookMarketingHelper: FacebookMarketingHelper, log: LoggingAdapter) {
  def getAds: List[Ad] = {
    var result: mutable.MutableList[Ad] = mutable.MutableList()
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
    var result: mutable.MutableList[AdsInsights] = mutable.MutableList()
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
    var result: mutable.MutableList[Ad] = mutable.MutableList()
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
    var result: mutable.MutableList[Ad] = mutable.MutableList()
    var ads = Ad.fetchByIds(adIds, seqAsJavaList(AdAccount.APIRequestGetAds.FIELDS.toSeq), facebookMarketingHelper.context)
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
