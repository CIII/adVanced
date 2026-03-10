package sync.google.adwords

import java.util.Calendar
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import models.mongodb.google._
import models.mongodb.lynx.TQReporting
import org.joda.time.DateTime
import play.api.Logging
import play.api.libs.json.{JsNumber, JsObject, Json}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Helper for parsing Google Ads report data.
 *
 * TODO: The report download mechanism needs to be re-implemented with
 * Google Ads API v18 searchStream() instead of the old ReportDownloader.
 * The report type enum (ReportDefinitionReportType) has been removed.
 */
object ReportHelper extends Logging {
  def getDateString: String = {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DATE, -1)
    val date = cal.get(Calendar.DATE)
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    s"$year-$month-$date"
  }

  def parseGoogleGeoPerformance(js: JsObject): GoogleGeoPerformance = {
    var geo = new GoogleGeoPerformance
    geo.id((js \ "ad group id").get.as[String])
    geo.adGroupId((js \ "ad group id").get.as[String].toLong)
    geo.adGroupName((js \ "ad group").get.as[String])
    geo.adGroupState((js \ "ad group state").get.as[String])
    geo.campaignName((js \ "campaign").get.as[String])
    geo.campaignId((js \ "campaign id").get.as[String].toLong)
    geo.campaignState((js \ "campaign state").get.as[String])
    geo.region((js \ "region").get.as[String])
    geo.city((js \ "city").get.as[String])
    geo.mostSpecificLocation((js \ "most specific location").get.as[String])
    geo.isTargetable((js \ "is targetable").get.as[String])
    geo.clientName((js \ "client name").get.as[String])
    geo.currency((js \ "currency").get.as[String])
    geo.countryTerritory((js \ "country/territory").get.as[String])
    geo.metroArea((js \ "metro area").get.as[String])
    geo.customerId((js \ "customer id").get.as[String])
    geo = parseGooglePerformance[GoogleGeoPerformance](js, geo)
    geo
  }

  def parseGoogleCampaignPerformance(js: JsObject): GoogleCampaignPerformance = {
    var campaign = new GoogleCampaignPerformance
    campaign.id((js \ "campaign id").get.as[String])
    campaign.campaignName((js \ "campaign").get.as[String])
    campaign.campaignId((js \ "campaign id").get.as[String].toLong)
    campaign.campaignState((js \ "campaign state").get.as[String])
    campaign = parseGooglePerformance[GoogleCampaignPerformance](js, campaign)
    campaign
  }

  def parseGoogleAdGroupPerformance(js: JsObject): GoogleAdGroupPerformance = {
    var adGroup = new GoogleAdGroupPerformance
    adGroup.id((js \ "ad group id").get.as[String])
    adGroup.adGroupId((js \ "ad group id").get.as[String].toLong)
    adGroup.adGroupName((js \ "ad group").get.as[String])
    adGroup.adGroupState((js \ "ad group state").get.as[String])
    adGroup.campaignName((js \ "campaign").get.as[String])
    adGroup.campaignId((js \ "campaign id").get.as[String].toLong)
    adGroup.campaignState((js \ "campaign state").get.as[String])
    adGroup = parseGooglePerformance[GoogleAdGroupPerformance](js, adGroup)
    adGroup
  }

  def parseGoogleAdPerformance(js: JsObject): GoogleAdPerformance = {
    var ad = new GoogleAdPerformance
    ad.id((js \ "ad id").get.as[String])
    ad.adGroupId((js \ "ad group id").get.as[String].toLong)
    ad.adGroupName((js \ "ad group").get.as[String])
    ad.adGroupState((js \ "ad group state").get.as[String])
    ad.campaignName((js \ "campaign").get.as[String])
    ad.campaignId((js \ "campaign id").get.as[String].toLong)
    ad.campaignState((js \ "campaign state").get.as[String])
    ad.adName((js \ "ad").get.as[String])
    ad.adId((js \ "ad id").get.as[String].toLong)
    ad.adState((js \ "ad state").get.as[String])
    ad.adType((js \ "ad type").get.as[String])
    ad.adDescription((js \ "description").get.as[String])
    ad.adDescriptionLine1((js \ "description line 1").get.as[String])
    ad.headline1((js \ "headline 1").get.as[String])
    ad.headline2((js \ "headline 2").get.as[String])
    ad.longHeadline((js \ "long headline").get.as[String])
    ad.imageAdName((js \ "image ad name").get.as[String])
    ad.imageWidth((js \ "image width").get.as[String])
    ad.imageHeight((js \ "image height").get.as[String])
    ad.path1((js \ "path 1").get.as[String])
    ad.path2((js \ "path 2").get.as[String])
    ad = parseGooglePerformance[GoogleAdPerformance](js, ad)
    ad
  }

  def parseGooglePerformance[T <: GooglePerformance](js: JsObject, entity: T): T = {
    entity.date(DateTime.parse((js \ "day").get.as[String]))
    entity.cost((js \ "cost").asOpt[String].getOrElse("0").toDouble / 1000000.0)
    entity.impressions((js \ "impressions").asOpt[String].getOrElse("0").toInt)
    entity.avgPosition((js \ "avg position").asOpt[String].getOrElse("0").toDouble)
    entity.dayOfWeek((js \ "day of week").get.as[String])
    entity.accountName((js \ "accountName").get.as[String])
    entity.interactions((js \ "interactions").asOpt[String].getOrElse("0").toInt)
    entity.crossDeviceConversions((js \ "cross-device conv").asOpt[String].getOrElse("0").toDouble)
    entity.network((js \ "network (with search partners)").get.as[String])
    entity.gmailClicksToWebsite((js \ "gmail clicks to website").asOpt[String].getOrElse("0").toInt)
    entity.conversions((js \ "conversions").asOpt[String].getOrElse("0").toDouble)
    entity.clicks((js \ "clicks").asOpt[String].getOrElse("0").toInt)
    entity.gmailSaves((js \ "gmail saves").asOpt[String].getOrElse("0").toInt)
    entity.gmailForwards((js \ "gmail forwards").asOpt[String].getOrElse("0").toInt)
    entity.device((js \ "device").get.as[String])
    entity.viewThroughConversions((js \ "view-through conv").asOpt[String].getOrElse("0").toDouble)
    entity.revenue((js \ "revenue").asOpt[Double].getOrElse(0.0))
    entity.conF((js \ "conf").asOpt[Int].getOrElse(0))
    entity.arrivals((js \ "arrivals").asOpt[Int].getOrElse(0))
    entity.conU((js \ "conu").asOpt[Int].getOrElse(0))
    entity.duration((js \ "duration").asOpt[Double].getOrElse(0.0))
    entity.lpConvU((js \ "lp_conv_u").asOpt[Int].getOrElse(0))
    entity.uFormComplete((js \ "u_form_complete").asOpt[Int].getOrElse(0))
    entity.bounce((js \ "bounce").asOpt[Int].getOrElse(0))
    entity
  }

  /**
   * Given a Google report, find any Lynx sessions which match based on the attribution criteria,
   * and pull the revenue and conf (summed) from the lynx session and add it to the google record.
   *
   * TODO: Re-implement report type parameter with Google Ads API v18 report types.
   */
  def attributeLynxSessionData(log: org.apache.pekko.event.LoggingAdapter, sanitizedItem: JsObject, reportType: String): JsObject = {
    val attributionCriteriaList: List[(String, String)] = getAttributionCriteriaForReportType(reportType)
    var matchDoc = Document(
      "utm_source" -> "google",
      "created_at" -> (sanitizedItem \ "day").get.as[String],
      "g_device" -> normalizeDevice((sanitizedItem \ "device").get.as[String]),
      "day_of_week" -> (sanitizedItem \ "day of week").get.as[String],
      "g_network" -> normalizeNetwork((sanitizedItem \ "network (with search partners)").get.as[String])
    )
    for (attributionCriteria <- attributionCriteriaList) {
      matchDoc = matchDoc ++ Document(attributionCriteria._2 -> (sanitizedItem \ attributionCriteria._1).get.as[String])
    }

    var groupDoc = Document("_id" -> null)
    getAttributionFields.foreach { field =>
      groupDoc = groupDoc ++ Document(field -> Document("$sum" -> ("$" + field)))
    }

    val pipeline = List(
      Document("$match" -> matchDoc),
      Document("$group" -> groupDoc)
    )

    // TODO: Make async - remove Await.result
    val sessionAttributeList = Await.result(TQReporting.arrivalFactCollection.aggregate(pipeline).toFuture(), 30.seconds)
    if (sessionAttributeList.nonEmpty) {
      if (sessionAttributeList.size > 1) {
        logger.warn("Multiple attributable sessions for Google report record")
      }

      var attributedItem: JsObject = sanitizedItem
      val sessionAttributes = sessionAttributeList.head
      getAttributionFields.foreach { field =>
        if (field.equals("revenue")) {
          attributedItem = attributedItem + ("revenue" -> JsNumber(sessionAttributes.getDouble("revenue").doubleValue()))
        } else {
          attributedItem = attributedItem + (field -> JsNumber(sessionAttributes.getInteger(field).intValue()))
        }
      }

      attributedItem
    } else {
      sanitizedItem
    }
  }

  def normalizeDevice(googleVal: String): String = googleVal match {
    case "Computers" => "c"
    case "Tablets with full browsers" => "t"
    case "Mobile devices with full browsers" => "m"
    case "Other" => "o"
    case _ => throw new Exception(s"Unmatched google device value: $googleVal")
  }

  def normalizeNetwork(googleVal: String): String = googleVal match {
    case "Google search" => "g"
    case "Search partners" => "s"
    case "Display Network" => "d"
    case _ => throw new Exception(s"Unmatched google network value: $googleVal")
  }

  /**
   * TODO: Replace string-based report types with Google Ads API v18 enum values.
   */
  def getAttributionCriteriaForReportType(reportType: String): List[(String, String)] = reportType match {
    case "AD_PERFORMANCE_REPORT" => List(
      ("ad id", "utm_content"),
      ("ad group id", "adgroupid"),
      ("campaign id", "utm_campaign")
    )
    case "ADGROUP_PERFORMANCE_REPORT" => List(
      ("ad group id", "adgroupid"),
      ("campaign id", "utm_campaign")
    )
    case "CAMPAIGN_PERFORMANCE_REPORT" => List(
      ("campaign id", "utm_campaign")
    )
    case "GEO_PERFORMANCE_REPORT" => List(
      ("most specific location", "g_location"),
      ("campaign id", "utm_campaign"),
      ("ad group id", "adgroupid")
    )
    case _ => List()
  }

  def getAttributionFields: List[String] = {
    List("conf", "revenue", "arrivals", "conu", "duration", "lp_conv_u", "u_form_complete", "bounce")
  }
}
