package sync.msn.bingads.account

import com.microsoft.bingads.v11.campaignmanagement._
import sync.msn.bingads.BingAdsHelper
import sync.shared.Msn._

class AdHelper(BingAds: BingAdsHelper) {

  def getAds(adGroupId: Option[Long] = None, adIds: Option[Array[Long]] = None): ArrayOfAd = {
    adGroupId match {
      case Some(id) =>
        val getAdsByAdGroupIdRequest = new GetAdsByAdGroupIdRequest
        getAdsByAdGroupIdRequest.setAdGroupId(id)
        val result = BingAds.campaignManagementService.getService.getAdsByAdGroupId(getAdsByAdGroupIdRequest)
        result.getAds
      case None =>
        adIds match {
          case Some(idArray) =>
            val getAdsByIdsRequest = new GetAdsByIdsRequest
            val idArr = new ArrayOflong
            idArray.foreach(idArr.getLongs.add(_))
            getAdsByIdsRequest.setAdIds(idArr)
            val result = BingAds.campaignManagementService.getService.getAdsByIds(getAdsByIdsRequest)
            result.getAds
          case None =>
            throw new IllegalArgumentException("Error: Please specify an Campaign ID or an array of AdGroup IDs to retrieve.")
        }
    }
  }

  def addAds(adGroupId: Long, adGroups: Array[Ad]): Boolean = {
    var offset = 0
    var ads_slice = adGroups.slice(offset, offset + addAdsLimit)
    while(ads_slice.length > 0) {
      val addAdsRequest = new AddAdsRequest()
      addAdsRequest.setAdGroupId(adGroupId)
      val adArr = new ArrayOfAd
      ads_slice.foreach(adArr.getAds.add)
      addAdsRequest.setAds(adArr)
      BingAds.campaignManagementService.getService.addAds(addAdsRequest)
      offset += ads_slice.length
      ads_slice = ads_slice.slice(offset, offset + addAdsLimit)
    }
    true
  }

}
