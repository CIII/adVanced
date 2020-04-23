package sync.msn.bingads.account

import com.microsoft.bingads.v11.campaignmanagement._
import sync.msn.bingads.BingAdsHelper

class CampaignHelper(BingAds: BingAdsHelper) {

  def getCampaigns(accountId: Option[Long] = None, campaignIds: Option[Array[Long]]): ArrayOfCampaign = {
    accountId match {
      case Some(id) =>
        val campaignAccountIdRequest = new GetCampaignsByAccountIdRequest
        campaignAccountIdRequest.setAccountId(id)
        val campaigns = BingAds.campaignManagementService.getService.getCampaignsByAccountId (campaignAccountIdRequest)
        campaigns.getCampaigns
      case None =>
        campaignIds match {
          case Some(idArray) =>
            val campaignIdsRequest = new GetCampaignsByIdsRequest
            val idArr = new ArrayOflong
            idArray.foreach(idArr.getLongs.add(_))
            campaignIdsRequest.setCampaignIds(idArr)
            val campaigns = BingAds.campaignManagementService.getService.getCampaignsByIds(campaignIdsRequest)
            campaigns.getCampaigns
          case None =>
            throw new IllegalArgumentException("Error: Please specify an Account ID or an array of Campaign IDs to retrieve.")
        }
    }
  }

  def addCampaigns(accountId: Long, campaigns: Array[Campaign]): Boolean = {
    var offset = 0
    var campaigns_slice: Array[Campaign] = campaigns.slice(offset, offset + 100)
    while(campaigns_slice.length > 0) {
      val addCampaignsRequest = new AddCampaignsRequest()
      addCampaignsRequest.setAccountId(accountId)
      val campaignArr = new ArrayOfCampaign
      campaigns_slice.foreach(campaignArr.getCampaigns.add)
      addCampaignsRequest.setCampaigns(campaignArr)
      BingAds.campaignManagementService.getService.addCampaigns(addCampaignsRequest)
      offset += campaigns_slice.length
      campaigns_slice = campaigns.slice(offset, offset + 100)
    }
    true
  }
}
