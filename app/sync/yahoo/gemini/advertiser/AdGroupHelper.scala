package sync.yahoo.gemini.advertiser

import akka.event.LoggingAdapter
import models.mongodb.yahoo.Yahoo._
import play.api.libs.json.{JsObject, Json}
import sync.yahoo.gemini.GeminiHelper

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

class AdGroupHelper(geminiHelper: GeminiHelper, log: LoggingAdapter) {
  def getAdGroups(campaignId: Option[Long]=None, startingPage: Int=1, maxPageSize: Int=geminiHelper.PAGESIZE): List[AdGroup] = {
    var morePages = true
    var currentPage = startingPage
    var adgroups = ListBuffer[AdGroup]()
    breakable {
      while (morePages) {
        val page = geminiHelper.apiRequest(
          geminiHelper.RequestType.FindAdGroup,
          Map("mr" -> maxPageSize.toString, "si" -> (currentPage * maxPageSize).toString)
        ).json.\("response").as[List[Map[String, String]]]

        if(page.isEmpty) {
          morePages = false
          break()
        }

        page.foreach { adgroup =>
          adgroups += AdGroup(
            _id = None,
            advertiserObjId = None,
            advertiserApiId = Some(adgroup("advertiserId").toLong),
            campaignObjId = None,
            campaignApiId = Some(adgroup("campaignId").toLong),
            apiId = Some(adgroup("id").toLong),
            bidSet = BidSet(
              bids = Json.parse(adgroup("bidSet")).\("bids").asInstanceOf[List[JsObject]].map{
                x =>
                  Bid(
                    priceType = PriceType.withName(x.\("priceType").as[String]),
                    value = x.\("value").as[Long],
                    channel = Channel.withName(x.\("channel").as[String])
                  )
              }
            ),
            startDateStr = adgroup("startDateStr"),
            endDateStr = adgroup("endDateStr"),
            adGroupName = adgroup("adGroupName"),
            status = Status.withName(adgroup("status")),
            advancedGeoPos = Some(AdvancedGeoPos.withName(adgroup("advancedGeoPos"))),
            advancedGeoNeg = Some(AdvancedGeoNeg.withName(adgroup("advancedGeoNeg"))),
            biddingStrategy = Some(BiddingStrategy.withName(adgroup("biddingStrategy"))),
            epcaGoal = adgroup("epcaGoal").toLong
          )
        }

        currentPage += 1
      }
    }

    adgroups.toList
  }
}
