package sync.msn.bingads.account

import com.microsoft.bingads.v11.campaignmanagement._
import sync.msn.bingads.BingAdsHelper
import sync.shared.Msn._

class KeywordHelper(BingAds: BingAdsHelper) {
  def getKeywords(adGroupId: Option[Long] = None, keywordIds: Option[Array[Long]] = None): ArrayOfKeyword = {
    adGroupId match {
      case Some(id) =>
        val getKeywordsByAdGroupIdRequest = new GetKeywordsByAdGroupIdRequest
        getKeywordsByAdGroupIdRequest.setAdGroupId(id)
        val result = BingAds.campaignManagementService.getService.getKeywordsByAdGroupId(getKeywordsByAdGroupIdRequest)
        result.getKeywords
      case None =>
        keywordIds match {
          case Some(idArray) =>
            val getKeywordsByIdsRequest = new GetKeywordsByIdsRequest
            val idArr = new ArrayOflong
            idArray.foreach(idArr.getLongs.add(_))
            getKeywordsByIdsRequest.setKeywordIds(idArr)
            val result = BingAds.campaignManagementService.getService.getKeywordsByIds(getKeywordsByIdsRequest)
            result.getKeywords
          case None =>
            throw new IllegalArgumentException("Error: Please specify an Campaign ID or an array of AdGroup IDs to retrieve.")
        }
    }
  }

  def addKeywords(adGroupId: Long, keywords: Array[Keyword]): Boolean = {
    var offset = 0
    var keywords_slice = keywords.slice(offset, offset + addKeywordsLimit)
    while(keywords_slice.length > 0) {
      val addAdsRequest = new AddKeywordsRequest()
      addAdsRequest.setAdGroupId(adGroupId)
      val keywordArr = new ArrayOfKeyword
      keywords_slice.foreach(keywordArr.getKeywords.add)
      addAdsRequest.setKeywords(keywordArr)
      BingAds.campaignManagementService.getService.addKeywords(addAdsRequest)
      offset += keywords_slice.length
      keywords_slice = keywords_slice.slice(offset, offset + addKeywordsLimit)
    }
    true
  }
}
