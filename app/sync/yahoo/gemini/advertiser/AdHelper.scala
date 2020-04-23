package sync.yahoo.gemini.advertiser

import akka.event.LoggingAdapter
import models.mongodb.yahoo.Yahoo.{Ad, CallToAction, Status}
import sync.yahoo.gemini.GeminiHelper

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

class AdHelper(geminiHelper: GeminiHelper, log: LoggingAdapter) {
  def getAdvertisers(adGroupId: Option[Long]=None, startingPage: Int=1, maxPageSize: Int=geminiHelper.PAGESIZE): List[Ad] = {
    var morePages = true
    var currentPage = startingPage
    var ads = ListBuffer[Ad]()
    breakable {
      while (morePages) {
        val page = geminiHelper.apiRequest(
          geminiHelper.RequestType.FindAd,
          Map("mr" -> maxPageSize.toString, "si" -> (currentPage * maxPageSize).toString)
        ).json.\("response").as[List[Map[String, String]]]

        if(page.isEmpty) {
          morePages = false
          break()
        }

        page.foreach { ad =>
          ads += Ad(
            _id = None,
            advertiserObjId = None,
            advertiserApiId = Some(ad("advertiserId").toLong),
            campaignObjId = None,
            campaignApiId = Some(ad("campaignId").toLong),
            adGroupObjId = None,
            adGroupApiId = Some(ad("adGroupId").toLong),
            apiId = Some(ad("id").toLong),
            description = ad("description"),
            displayUrl = ad("displayUrl"),
            imageUrl = if(ad("imageUrl").isEmpty) None else Some(ad("imageUrl")),
            imageUrlHQ = if(ad("imageUrlHQ").isEmpty) None else Some(ad("imageUrlHQ")),
            landingUrl = ad("landingUrl"),
            sponsoredBy = ad("sponsoredBy"),
            status = Status.withName(ad("status")),
            title = ad("title"),
            contentUrl = if(ad("contentUrl").isEmpty) None else Some(ad("contentUrl")),
            videoPrimaryUrl = if(ad("videoPrimaryUrl").isEmpty) None else Some(ad("videoPrimaryUrl")),
            impressionTrackingUrls = if(ad("impressionTrackingUrls").isEmpty) None else Some(ad("impressionTrackingUrls")),
            callToActionText = if(ad("callToActionText").isEmpty) None else Some(CallToAction.withName(ad("callToActionText"))),
            adName = if(ad("adName").isEmpty) None else Some(ad("adName"))
          )
        }

        currentPage += 1
      }
    }

    ads.toList
  }
}
