package sync.yahoo.gemini.advertiser

import akka.event.LoggingAdapter
import models.mongodb.yahoo.Yahoo.Advertiser
import play.api.libs.json.JsObject
import sync.yahoo.gemini.GeminiHelper

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

class AdvertiserHelper(geminiHelper: GeminiHelper, log: LoggingAdapter) {
  def getAdvertisers(advertiserId: Option[Long]=None, startingPage: Int=1, maxPageSize: Int=geminiHelper.PAGESIZE): List[Advertiser] = {
    var morePages = true
    var currentPage = startingPage
    var advertisers = ListBuffer[Advertiser]()
    breakable {
      while (morePages) {
        val page = geminiHelper.apiRequest(
          geminiHelper.RequestType.FindAdvertiser,
          Map("mr" -> maxPageSize.toString, "si" -> ((currentPage-1) * maxPageSize).toString)
        ).json.\("response").as[Seq[JsObject]]

        if(page.isEmpty) {
          morePages = false
          break()
        }

        page.foreach { adv =>
          advertisers += Advertiser(
            _id = None,
            apiId = adv.\("id").as[Long],
            advertiserName = adv.\("advertiserName").as[String],
            timezone = adv.\("timezone").as[String],
            currency = adv.\("currency").as[String],
            status = adv.\("status").as[String],
            billingCountry = adv.\("billingCountry").as[String],
            webSiteUrl = adv.\("webSiteUrl").as[String],
            lastUpdateDate = adv.\("lastUpdateDate").as[Long],
            bookingCountry = adv.\("bookingCountry").as[String],
            language = adv.\("language").as[String],
            createdDate = adv.\("createdDate").as[Long]
          )
        }

        currentPage += 1
      }
    }

    advertisers.toList
  }
}
