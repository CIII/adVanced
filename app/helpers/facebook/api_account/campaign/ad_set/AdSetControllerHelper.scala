package helpers.facebook.api_account.campaign.ad_set

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import org.joda.time.DateTime
import play.api.data.{Form, Forms}
import play.api.data.Forms._
import scala.jdk.CollectionConverters._
import Shared.Shared.jodaDate

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

  case class AdSetParent(
    apiAccountObjId: Option[String],
    campaignApiId: Option[String]
  )

  def adSetParentToDocument(asp: AdSetParent): Document = Document(
    "apiAccountObjId" -> asp.apiAccountObjId,
    "campaignApiId" -> asp.campaignApiId
  )

  def documentToAdSetParent(dbo: Document): AdSetParent = AdSetParent(
    apiAccountObjId = Option(dbo.getString("apiAccountObjId")),
    campaignApiId = Option(dbo.getString("campaignApiId"))
  )

  case class AdSetForm(
    parent: AdSetParent,
    apiId: Option[String],
    accountId: String,
    adLabels: Option[List[String]],
    bidAmount: Long,
    billingEvent: String,
    budgetRemaining: Option[Long],
    campaignId: String,
    configuredStatus: String,
    createdTime: DateTime,
    dailyBudget: Option[Long],
    endTime: Option[DateTime],
    frequencyCap: Option[Int],
    frequencyCapResetPeriod: Option[Int],
    isAutoBid: Boolean,
    lifetimeBudget: Option[Long],
    effectiveStatus: Option[String],
    name: String,
    optimizationGoal: Option[String],
    startTime: Option[DateTime],
    status: Option[String],
    updatedTime: Option[String]
  )

  def adSetFormToDocument(asf: AdSetForm): Document = {
    Document(
      "parent" -> adSetParentToDocument(asf.parent),
      "apiId" -> asf.apiId,
      "accountId" -> asf.accountId,
      "adLabels" -> asf.adLabels,
      "bidAmount" -> asf.bidAmount,
      "billingEvent" -> asf.billingEvent,
      "budgetRemaining" -> asf.budgetRemaining,
      "campaignId" -> asf.campaignId,
      "configuredStatus" -> asf.configuredStatus,
      "createdTime" -> asf.createdTime.getMillis,
      "dailyBudget" -> asf.dailyBudget,
      "endTime" -> asf.endTime.map(_.getMillis),
      "frequencyCap" -> asf.frequencyCap,
      "frequencyCapResetPeriod" -> asf.frequencyCapResetPeriod,
      "isAutoBid" -> asf.isAutoBid,
      "lifetimeBudget" -> asf.lifetimeBudget,
      "effectiveStatus" -> asf.effectiveStatus,
      "name" -> asf.name,
      "optimizationGoal" -> asf.optimizationGoal,
      "startTime" -> asf.startTime.map(_.getMillis),
      "status" -> asf.status,
      "updatedTime" -> asf.updatedTime
    )
  }

  def documentToAdSetForm(dbo: Document): AdSetForm = {
    AdSetForm(
      parent=documentToAdSetParent(Option(dbo.toBsonDocument.get("parent")).map(v => Document(v.asDocument())).get),
      apiId = Option(dbo.getString("apiId")),
      accountId = dbo.get("accountId").toString,
      adLabels = Option(dbo.getList("adLabels", classOf[String])).map(_.asScala.toList),
      bidAmount = dbo.get("bidAmount").asInstanceOf[Long],
      billingEvent = dbo.get("billingEvent").toString,
      budgetRemaining = Option(dbo.getLong("budgetRemaining")).map(_.toLong),
      campaignId = dbo.get("campaignId").toString,
      configuredStatus = dbo.get("configuredStatus").toString,
      createdTime = Option(dbo.get("createdTime")).map(v => new DateTime(v)).getOrElse(DateTime.now),
      dailyBudget = Option(dbo.getLong("dailyBudget")).map(_.toLong),
      endTime = Option(dbo.get("endTime")).map(v => new DateTime(v)),
      frequencyCap = Option(dbo.getInteger("frequencyCap")).map(_.intValue()),
      frequencyCapResetPeriod = Option(dbo.getInteger("frequencyCapResetPeriod")).map(_.intValue()),
      isAutoBid = dbo.get("isAutoBid").asInstanceOf[Boolean],
      lifetimeBudget = Option(dbo.getLong("lifetimeBudget")).map(_.toLong),
      effectiveStatus = Option(dbo.getString("effectiveStatus")),
      name = Option(dbo.getString("name")).getOrElse(""),
      optimizationGoal = Option(dbo.getString("optimizationGoal")),
      startTime = Option(dbo.get("startTime")).map(v => new DateTime(v)),
      status = Option(dbo.getString("status")),
      updatedTime = Option(dbo.getString("updatedTime"))
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
    )(AdSetForm.apply)(AdSetForm.unapply _)
  )
}