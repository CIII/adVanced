package sync.facebook.ads.user

import scala.collection.JavaConversions._
import akka.event.LoggingAdapter
import com.facebook.ads.sdk.{AdAccount, AdsInsights, Campaign}
import sync.facebook.ads.FacebookMarketingHelper

import scala.collection.mutable

class CampaignHelper(facebookMarketingHelper: FacebookMarketingHelper, log: LoggingAdapter) {
  def getCampaigns(): List[Campaign] = {
    var result: mutable.MutableList[Campaign] = mutable.MutableList()
    var campaigns = facebookMarketingHelper.adAccount.getCampaigns.requestAllFields().execute()
    while(campaigns != null) {
      for (campaign: Campaign <- campaigns) {
        result += campaign
      }
      campaigns = campaigns.nextPage()
    }
    result.toList
  }

  def getCampaignAdsInsights(campaign: Campaign) = {
    var result: mutable.MutableList[AdsInsights] = mutable.MutableList()
    var adsInsights = campaign.getInsights.execute()
    while(adsInsights != null) {
      for (adInsight: AdsInsights <- adsInsights) {
        result += adInsight
      }
      adsInsights = adsInsights.nextPage()
    }
    result.toList
  }

  def getCampaignsByIds(campaignIds: List[String]): List[Campaign] = {
    var result: mutable.MutableList[Campaign] = mutable.MutableList()
    var campaigns = Campaign.fetchByIds(campaignIds, seqAsJavaList(AdAccount.APIRequestGetCampaigns.FIELDS.toSeq), facebookMarketingHelper.context)
    while(campaigns != null) {
      for (campaign: Campaign <- campaigns) {
        result += campaign
      }
      campaigns = campaigns.nextPage()
    }
    result.toList
  }

  def deleteCampaign(campaignId: String): Campaign.APIRequestDelete = {
    Campaign.fetchById(campaignId, facebookMarketingHelper.context).delete()
  }

  def updateCampaign(campaignId: String): Campaign.APIRequestUpdate = {
    Campaign.fetchById(campaignId, facebookMarketingHelper.context).update()
  }
}
