package helpers.facebook.api_account.campaign

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import org.joda.time.DateTime
import play.api.data.{Form, Forms}
import play.api.data.Forms.{longNumber, mapping, optional, text}
import scala.jdk.CollectionConverters._
import Shared.Shared.jodaDate

object CampaignControllerHelper {
  case class CampaignParent(
    apiAccountObjId: Option[String]
  )

  def campaignParentToDocument(cp: CampaignParent) = Document(
    "apiAccountObjId" -> cp.apiAccountObjId
  )

  def documentToCampaignParent(dbo: Document) = CampaignParent(
    apiAccountObjId=Option(dbo.getString("apiAccountObjId"))
  )

  case class CampaignForm(
    apiId: Option[String],
    accountId: String,
    adLabels: Option[List[String]],
    budgetRebalanceFlag: Boolean,
    buyingType: String,
    canUseSpendCap: Boolean,
    configuredStatus: String,
    createdTime: Option[String],
    effectiveStatus: String,
    name: String,
    objective: Option[String],
    spendCap: Option[Long],
    startTime: Option[DateTime],
    status: Option[String],
    stopTime: Option[DateTime],
    updatedTime: Option[DateTime]
  )

  def campaignFormToDocument(cf: CampaignForm): Document = {
    Document(
      "apiId" -> cf.apiId,
      "accountId" -> cf.accountId,
      "adLabels" -> cf.adLabels,
      "budgetRebalanceFlag" -> cf.budgetRebalanceFlag,
      "buyingType" -> cf.buyingType,
      "canUseSpendCap" -> cf.canUseSpendCap,
      "configuredStatus" -> cf.configuredStatus,
      "createdTime" -> cf.createdTime,
      "effectiveStatus" -> cf.effectiveStatus,
      "name" -> cf.name,
      "objective" -> cf.objective,
      "spendCap" -> cf.spendCap,
      "startTime" -> cf.startTime.map(_.getMillis),
      "status" -> cf.status,
      "stopTime" -> cf.stopTime.map(_.getMillis),
      "updatedTime" -> cf.updatedTime.map(_.getMillis)
    )
  }

  def documentToCampaignForm(dbo: Document): CampaignForm = {
    CampaignForm(
      apiId = Option(dbo.getString("apiId")),
      accountId = dbo.get("accountId").asInstanceOf[String],
      adLabels = Option(dbo.getList("adLabels", classOf[String])).map(_.asScala.toList),
      budgetRebalanceFlag = dbo.get("budgetRebalanceFlag").asInstanceOf[Boolean],
      buyingType = dbo.get("buyingType").asInstanceOf[String],
      canUseSpendCap = dbo.get("canUseSpendCap").asInstanceOf[Boolean],
      configuredStatus = dbo.get("configuredStatus").asInstanceOf[String],
      createdTime = Option(dbo.getString("createdTime")),
      effectiveStatus = dbo.get("effectiveStatus").asInstanceOf[String],
      name = dbo.get("name").asInstanceOf[String],
      objective = Option(dbo.getString("objective")),
      spendCap = Option(dbo.getLong("spendCap")).map(_.toLong),
      startTime = Option(dbo.get("startTime")).map(v => new DateTime(v)),
      status = Option(dbo.getString("status")),
      stopTime = Option(dbo.get("stopTime")).map(v => new DateTime(v)),
      updatedTime = Option(dbo.get("updatedTime")).map(v => new DateTime(v))
    )
  }

  def campaignForm: Form[CampaignForm] = Form(
    mapping(
      "apiId" -> optional(text),
      "accountId" -> text,
      "adLabels" -> optional(Forms.list(text)),
      "budgetRebalanceFlag" -> Forms.boolean,
      "buyingType" -> text,
      "canUseSpendCap" -> Forms.boolean,
      "configuredStatus" -> text,
      "createdTime" -> optional(text),
      "effectiveStatus" -> text,
      "name" -> text,
      "objective" -> optional(text),
      "spendCap" -> optional(longNumber),
      "startTime" -> optional(jodaDate),
      "status" -> optional(text),
      "stopTime" -> optional(jodaDate),
      "updatedTime" -> optional(jodaDate)
    )(CampaignForm.apply)(CampaignForm.unapply _)
  )
}