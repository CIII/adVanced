package helpers.facebook.api_account.campaign.ad_set

import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import play.api.data.{Form, Forms}
import play.api.data.Forms._

object AdSetControllerHelper {
  lazy val optimizationGoalOptions = Seq(
    "NONE" -> "N/A",
    "APP_INSTALLS" -> "App Installs",
    "BRAND_AWARENESS" -> "Brand Awareness",
    "CLICKS" -> "Clicks",
    "ENGAGED_USERS" -> "Engaged Users",
    "EVENT_RESPONSES" -> "Event Responses",
    "IMPRESSIONS" -> "Impressions",
    "LEAD_GENERATION" -> "Lead Generation",
    "LINK_CLICKS" -> "Link Clicks",
    "OFFER_CLAIMS" -> "Offer Claims",
    "OFFSITE_CONVERSIONS" -> "Offsite Conversions",
    "PAGE_ENGAGEMENT" -> "Page Engagement",
    "PAGE_LIKES" -> "Page Likes",
    "POST_ENGAGEMENT" -> "Post Engagement",
    "REACH" -> "Reach",
    "SOCIAL_IMPRESSIONS" -> "Social Impressions",
    "VIDEO_VIEWS" -> "Video Views",
    "APP_DOWNLOADS" -> "App Downloads",
    "LANDING_PAGE_VIEWS" -> "Landing Page Views"
  )

  lazy val statusOptions = Seq(
    "ACTIVE" -> "Active",
    "PAUSED" -> "Paused",
    "DELETED" -> "Deleted",
    "ARCHIVED" -> "Archived"
  )

  lazy val billingEventOptions = Seq(
    "APP_INSTALLS" -> "App Installs",
    "CLICKS" -> "Clicks",
    "IMPRESSIONS" -> "Impressions",
    "LINK_CLICKS" -> "Link Clicks",
    "OFFER_CLAIMS" -> "Offer Claims",
    "PAGE_LIKES" -> "Page Likes",
    "POST_ENGAGEMENT" -> "Post Engagement",
    "VIDEO_VIEWS" -> "Video Views",
    "MRC_VIDEO_VIEWS" -> "MRC Video Views"
  )

  case class AdSetForm(
    var parent: AdSetParent,
    var apiId: Option[String],
    var accountId: String,
    var adLabels: Option[List[String]],
    var bidAmount: Long,
    var billingEvent: String,
    var budgetRemaining: Option[Long],
    var campaignId: String,
    var configuredStatus: String,
    var createdTime: DateTime,
    var dailyBudget: Option[Long],
    var endTime: Option[DateTime],
    var frequencyCap: Option[Int],
    var frequencyCapResetPeriod: Option[Int],
    var isAutoBid: Boolean,
    var lifetimeBudget: Option[Long],
    var effectiveStatus: Option[String],
    var name: String,
    var optimizationGoal: Option[String],
    var startTime: Option[DateTime],
    var status: Option[String],
    var updatedTime: Option[String]
  )

  def adSetFormToDbo(asf: AdSetForm): DBObject = {
    DBObject(
      "parent" -> adSetParentToDbo(asf.parent),
      "apiId" -> asf.apiId,
      "accountId" -> asf.accountId,
      "adLabels" -> asf.adLabels,
      "bidAmount" -> asf.bidAmount,
      "billingEvent" -> asf.billingEvent,
      "budgetRemaining" -> asf.budgetRemaining,
      "campaignId" -> asf.campaignId,
      "configuredStatus" -> asf.configuredStatus,
      "createdTime" -> asf.createdTime,
      "dailyBudget" -> asf.dailyBudget,
      "endTime" -> asf.endTime,
      "frequencyCap" -> asf.frequencyCap,
      "frequencyCapResetPeriod" -> asf.frequencyCapResetPeriod,
      "isAutoBid" -> asf.isAutoBid,
      "lifetimeBudget" -> asf.lifetimeBudget,
      "effectiveStatus" -> asf.effectiveStatus,
      "name" -> asf.name,
      "optimizationGoal" -> asf.optimizationGoal,
      "startTime" -> asf.startTime,
      "status" -> asf.status,
      "updatedTime" -> asf.updatedTime
    )
  }

  def dboToAdSetForm(dbo: DBObject): AdSetForm = {
    AdSetForm(
      parent=dboToAdSetParent(dbo.as[DBObject]("parent")),
      apiId = dbo.getAs[String]("apiId"),
      accountId = dbo.get("accountId").toString,
      adLabels = dbo.getAs[List[String]]("adLabels"),
      bidAmount = dbo.get("bidAmount").asInstanceOf[Long],
      billingEvent = dbo.get("billingEvent").toString,
      budgetRemaining = dbo.getAs[Long]("budgetRemaining"),
      campaignId = dbo.get("campaignId").toString,
      configuredStatus = dbo.get("configuredStatus").toString,
      createdTime = dbo.getAsOrElse[DateTime]("createdTime", DateTime.now),
      dailyBudget = dbo.getAs[Long]("dailyBudget"),
      endTime = dbo.getAs[DateTime]("endTime"),
      frequencyCap = dbo.getAs[Int]("frequencyCap"),
      frequencyCapResetPeriod = dbo.getAs[Int]("frequencyCapResetPeriod"),
      isAutoBid = dbo.get("isAutoBid").asInstanceOf[Boolean],
      lifetimeBudget = dbo.getAs[Long]("lifetimeBudget"),
      effectiveStatus = dbo.getAs[String]("effectiveStatus"),
      name = dbo.getAsOrElse[String]("name", ""),
      optimizationGoal = dbo.getAs[String]("optimizationGoal"),
      startTime = dbo.getAs[DateTime]("startTime"),
      status = dbo.getAs[String]("status"),
      updatedTime = dbo.getAs[String]("updatedTime")
    )
  }

  def adSetForm: Form[AdSetForm] = Form(
    mapping(
      "parent" -> mapping(
        "apiAccountObjId" -> optional(text),
        "campaignApiId" -> optional(text)
      )(AdSetParent.apply)(AdSetParent.unapply),
      "apiId" -> optional(text),
      "accountId" -> nonEmptyText,
      "adLabels" -> optional(list(text)),
      "bidAmount" -> longNumber,
      "billingEvent" -> nonEmptyText,
      "budgetRemaining" -> optional(longNumber),
      "campaignId" -> nonEmptyText,
      "configuredStatus" -> nonEmptyText,
      "createdTime" -> jodaDate,
      "dailyBudget" -> optional(longNumber),
      "endTime" -> optional(jodaDate),
      "freqencyCap" -> optional(number),
      "frequencyCapResetPeriod" -> optional(number),
      "isAutoBid" -> boolean,
      "lifetimeBudget" -> optional(longNumber),
      "effectiveStatus" -> optional(text),
      "name" -> nonEmptyText,
      "optimizationGoal" -> optional(text),
      "startTime" -> optional(jodaDate),
      "Status" -> optional(text),
      "updatedTime" -> optional(text)
    )(AdSetForm.apply)(AdSetForm.unapply)
  )
}