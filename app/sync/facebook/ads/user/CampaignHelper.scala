package sync.facebook.ads.user

import scala.jdk.CollectionConverters._
import org.apache.pekko.event.LoggingAdapter
// TODO: Update to facebook-java-business-sdk v20
import com.facebook.ads.sdk.{AdAccount, AdsInsights, Campaign}
import sync.facebook.ads.FacebookMarketingHelper

import scala.collection.mutable

class CampaignHelper(facebookMarketingHelper: FacebookMarketingHelper, log: LoggingAdapter) {
  def getCampaigns(): List[Campaign] = {
    var result: mutable.ListBuffer[Campaign] = mutable.ListBuffer()
    var campaigns = facebookMarketingHelper.adAccount.getCampaigns.requestAllFields().execute()
    while(campaigns != null) {
      campaigns.asScala.foreach { campaign =>
        result += campaign
      }
      campaigns = campaigns.nextPage()
    }
    result.toList
  }

  def getCampaignAdsInsights(campaign: Campaign) = {
    var result: mutable.ListBuffer[AdsInsights] = mutable.ListBuffer()
    var adsInsights = campaign.getInsights.execute()
    while(adsInsights != null) {
      adsInsights.asScala.foreach { adInsight =>
        result += adInsight
      }
      adsInsights = adsInsights.nextPage()
    }
    result.toList
  }

  def getCampaignsByIds(campaignIds: List[String]): List[Campaign] = {
    var result: mutable.ListBuffer[Campaign] = mutable.ListBuffer()
    var campaigns = Campaign.fetchByIds(campaignIds.asJava, AdAccount.APIRequestGetCampaigns.FIELDS.toSeq.asJava, facebookMarketingHelper.context)
    while(campaigns != null) {
      campaigns.asScala.foreach { campaign =>
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
