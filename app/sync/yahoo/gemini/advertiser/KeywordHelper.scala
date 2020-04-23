package sync.yahoo.gemini.advertiser

import akka.event.LoggingAdapter
import models.mongodb.yahoo.Yahoo._
import play.api.libs.json.{JsObject, Json}
import sync.yahoo.gemini.GeminiHelper

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

class KeywordHelper(geminiHelper: GeminiHelper, log: LoggingAdapter) {
  def getKeywords(advertiserId: Option[Long]=None, startingPage: Int=1, maxPageSize: Int=geminiHelper.PAGESIZE): List[Keyword] = {
    var morePages = true
    var currentPage = startingPage
    var keywords = ListBuffer[Keyword]()
    breakable {
      while (morePages) {
        val page = geminiHelper.apiRequest(
          geminiHelper.RequestType.FindKeyword,
          Map("mr" -> maxPageSize.toString, "si" -> (currentPage * maxPageSize).toString)
        ).json.\("response").as[List[Map[String, String]]]

        if(page.isEmpty) {
          morePages = false
          break()
        }

        page.foreach { kwd =>
          keywords += Keyword(
            _id = None,
            advertiserObjId = None,
            advertiserApiId = Some(kwd("advertiserId").toLong),
            campaignObjId = None,
            campaignApiId = if(ParentType.withName(kwd("parentType")).equals(ParentType.CAMPAIGN)) Some(kwd("parentId").toLong) else None,
            adGroupObjId = None,
            adGroupApiId = if(ParentType.withName(kwd("parentType")).equals(ParentType.ADGROUP)) Some(kwd("parentId").toLong) else None,
            apiId = kwd("id").toLong,
            bidSet = if(kwd("bidSet").isEmpty) None else Some(
              BidSet(
                bids = Json.parse(kwd("bidSet")).\("bids").asInstanceOf[List[JsObject]].map{
                  x =>
                    Bid(
                      priceType = PriceType.withName(x.\("priceType").as[String]),
                      value = x.\("value").as[Long],
                      channel = Channel.withName(x.\("channel").as[String])
                    )
                }
              )
            ),
            exclude = kwd("exclude").asInstanceOf[Boolean],
            matchType = MatchType.withName(kwd("matchType")),
            parentType = ParentType.withName(kwd("parentType")),
            status = Status.withName(kwd("status")),
            value = kwd("value"),
            adParamValues = if(kwd("adParamValues").isEmpty) None else Some(Json.parse("adParamValues").asInstanceOf[List[JsObject]].map{
              x =>
                AdParamValue(
                  paramIndex = x.\("paramIndex").as[Int],
                  insertionText = x.\("insertionText").as[String]
                )
            }),
            landingUrl = if(kwd("landingUrl").isEmpty) None else Some(kwd("landingUrl"))
          )
        }

        currentPage += 1
      }
    }

    keywords.toList
  }
}