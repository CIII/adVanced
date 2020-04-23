package sync.msn.bingads.account

import com.microsoft.bingads.v11.campaignmanagement.{AdGroup, AddAdGroupsRequest, ArrayOfAdGroup, ArrayOflong, GetAdGroupsByCampaignIdRequest, GetAdGroupsByIdsRequest}
import sync.msn.bingads.BingAdsHelper
import sync.shared.Msn._

class AdGroupHelper(BingAds: BingAdsHelper) {

  def getAdGroups(campaignId: Option[Long] = None, adGroupIds: Option[Array[Long]] = None): ArrayOfAdGroup = {
    campaignId match {
      case Some(id) =>
        val getAdGroupsByCampaignIdRequest = new GetAdGroupsByCampaignIdRequest
        getAdGroupsByCampaignIdRequest.setCampaignId(id)
        val result = BingAds.campaignManagementService.getService.getAdGroupsByCampaignId(getAdGroupsByCampaignIdRequest)
        result.getAdGroups
      case None =>
        adGroupIds match {
          case Some(idArray) =>
            val getAdGroupsByIdsRequest = new GetAdGroupsByIdsRequest
            val idArr = new ArrayOflong()
            idArray.foreach(idArr.getLongs.add(_))
            getAdGroupsByIdsRequest.setAdGroupIds(idArr)
            val result = BingAds.campaignManagementService.getService.getAdGroupsByIds(getAdGroupsByIdsRequest)
            result.getAdGroups
          case None =>
            throw new IllegalArgumentException("Error: Please specify an Campaign ID or an array of AdGroup IDs to retrieve.")
        }
    }
  }

  def addAdGroups(campaignId: Long, adGroups: Array[AdGroup]): Boolean = {
    var offset = 0
    var adgroups_slice: Array[AdGroup] = adGroups.slice(offset, offset + addAdGroupsLimit)
    while(adgroups_slice.length > 0) {
      val addAdGroupsRequest = new AddAdGroupsRequest()
      addAdGroupsRequest.setCampaignId(campaignId)
      val adGroupArr = new ArrayOfAdGroup
      adgroups_slice.foreach(adGroupArr.getAdGroups.add)
      addAdGroupsRequest.setAdGroups(adGroupArr)
      BingAds.campaignManagementService.getService.addAdGroups(addAdGroupsRequest)
      offset += adgroups_slice.length
      adgroups_slice = adgroups_slice.slice(offset, offset + addAdGroupsLimit)
    }
    true
  }
}
