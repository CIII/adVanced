package sync.yahoo.gemini.advertiser

import akka.event.LoggingAdapter
import models.mongodb.yahoo.Yahoo._
import sync.yahoo.gemini.GeminiHelper

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

class CampaignHelper(geminiHelper: GeminiHelper, log: LoggingAdapter) {
  def getCampaigns(advertiserId: Option[Long]=None, startingPage: Int=1, maxPageSize: Int=geminiHelper.PAGESIZE): List[Campaign] = {
    var morePages = true
    var currentPage = startingPage
    var campaigns = ListBuffer[Campaign]()
    breakable {
      while (morePages) {
        val page = geminiHelper.apiRequest(
          geminiHelper.RequestType.FindCampaign,
          Map("mr" -> maxPageSize.toString, "si" -> (currentPage * maxPageSize).toString)
        ).json.\("response").as[List[Map[String, String]]]

        if(page.isEmpty) {
          morePages = false
          break()
        }

        page.foreach { cmp =>
          campaigns += Campaign(
            _id = None,
            advertiserObjId = None,
            advertiserApiId = Some(cmp("advertiserId").toLong),
            budget = Some(cmp("budget").toDouble),
            budgetType = Some(BudgetType.withName(cmp("budgetType"))),
            campaignName = cmp("campaignName"),
            channel = Some(Channel.withName(cmp("channel"))),
            apiId = Some(cmp("id").toLong),
            language = Some(cmp("language")),
            objective = Some(cmp("objective")),
            status = Status.withName(cmp("status")),
            isPartnerNetwork = Some(Bool.withName(cmp("withName"))),
            defaultLandingUrl = Some(cmp("defaultLandingUrl")),
            trackingPartner = Some(cmp("trackingPartner")),
            appLocale = Some(cmp("appLocale")),
            advancedGeoPos = Some(AdvancedGeoPos.withName(cmp("advancedGeoPos"))),
            advancedGeoNeg = Some(AdvancedGeoNeg.withName(cmp("advancedGeoNeg")))
          )
        }

        currentPage += 1
      }
    }

    campaigns.toList
  }
}
