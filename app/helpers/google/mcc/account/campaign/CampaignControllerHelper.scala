package helpers.google.mcc.account.campaign

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import play.api.data.Form
import play.api.data.Forms._
import models.mongodb.google.Google._

object CampaignControllerHelper {
  case class CampaignParent(
    mccObjId: Option[String],
    customerApiId: Option[Long]
  )

  def campaignParentToDocument(cp: CampaignParent) = Document(
    "mccObjId" -> cp.mccObjId,
    "customerApiId" -> cp.customerApiId
  )

  def documentToCampaignParent(dbo: Document) = CampaignParent(
    mccObjId=Option(dbo.getString("mccObjId")),
    customerApiId=Option(dbo.getLong("customerApiId")).map(_.toLong)
  )

  case class CampaignForm(
    parent: CampaignParent,
    apiId: Option[Long],
    name: String,
    status: Option[String],
    servingStatus: Option[String],
    startDate: Option[String],
    endDate: Option[String],
    budgetAmount: Option[Int],
    isSharedBudget: Option[Boolean],
    advertisingChannelType: Option[String],
    adServingOptimizationStatus: Option[String],
    frequencyCapImpressions: Option[Long],
    frequencyCapTimeUnit: Option[String],
    frequencyCapLevel: Option[String],
    targetGoogleSearch: Option[Boolean],
    targetSearchNetwork: Option[Boolean],
    targetContentNetwork: Option[Boolean],
    targetPartnerSearchNetwork: Option[Boolean]
  )

  def campaignFormToDocument(cf: CampaignForm): Document = {
    Document(
      "parent" -> campaignParentToDocument(cf.parent),
      "apiId" -> cf.apiId,
      "name" -> cf.name,
      "status" -> cf.status,
      "servingStatus" -> cf.servingStatus,
      "startDate" -> cf.startDate,
      "endDate" -> cf.endDate,
      "budgetAmount" -> cf.budgetAmount,
      "isSharedBudget" -> cf.isSharedBudget,
      "advertisingChannelType" -> cf.advertisingChannelType,
      "adServingOptimizationStatus" -> cf.adServingOptimizationStatus,
      "frequencyCapImpressions" -> cf.frequencyCapImpressions,
      "frequencyCapTimeUnit" -> cf.frequencyCapTimeUnit,
      "frequencyCapLevel" -> cf.frequencyCapLevel,
      "targetGoogleSearch" -> cf.targetGoogleSearch,
      "targetSearchNetwork" -> cf.targetSearchNetwork,
      "targetContentNetwork" -> cf.targetContentNetwork,
      "targetPartnerSearchNetwork" -> cf.targetPartnerSearchNetwork
    )
  }

  def documentToCampaignForm(dbo: Document): CampaignForm = {
    CampaignForm(
      parent=documentToCampaignParent(Option(dbo.toBsonDocument.get("parent")).filter(_.isDocument).map(v => Document(v.asDocument())).get),
      apiId=Option(dbo.getLong("apiId")).map(_.toLong),
      name=dbo.getString("name"),
      status=Option(dbo.getString("status")),
      servingStatus=Option(dbo.getString("servingStatus")),
      startDate=Option(dbo.getString("startDate")),
      endDate=Option(dbo.getString("endDate")),
      budgetAmount=Option(dbo.getInteger("budgetAmount")).map(_.intValue()),
      isSharedBudget=Option(dbo.getBoolean("isSharedBudget")).map(_.booleanValue()),
      advertisingChannelType=Option(dbo.getString("advertisingChannelType")),
      adServingOptimizationStatus=Option(dbo.getString("adServingOptimizationStatus")),
      frequencyCapImpressions=Option(dbo.getLong("frequencyCapImpressions")).map(_.toLong),
      frequencyCapTimeUnit=Option(dbo.getString("frequencyCapTimeUnit")),
      frequencyCapLevel=Option(dbo.getString("frequencyCapLevel")),
      targetGoogleSearch=Option(dbo.getBoolean("targetGoogleSearch")).map(_.booleanValue()),
      targetSearchNetwork=Option(dbo.getBoolean("targetSearchNetwork")).map(_.booleanValue()),
      targetContentNetwork=Option(dbo.getBoolean("targetContentNetwork")).map(_.booleanValue()),
      targetPartnerSearchNetwork=Option(dbo.getBoolean("targetPartnerSearchNetwork")).map(_.booleanValue())
    )
  }

  def campaignForm: Form[CampaignForm] = Form(
    mapping(
      "parent" -> mapping(
        "mccObjId" -> optional(text),
        "customerApiId" -> optional(longNumber)
      )(CampaignParent.apply)(CampaignParent.unapply),
      "apiId" -> optional(longNumber),
      "name" -> nonEmptyText,
      "status" -> optional(text),
      "servingStatus" -> optional(text),
      "startDate" -> optional(text),
      "endDate" -> optional(text),
      "budgetAmount" -> optional(number),
      "isSharedBudget" -> optional(boolean),
      "advertisingChannelType" -> optional(text),
      "adServingOptimizationStatus" -> optional(text),
      "frequencyCapImpressions" -> optional(longNumber),
      "frequencyCapTimeUnit" -> optional(text),
      "frequencyCapLevel" -> optional(text),
      "targetGoogleSearch" -> optional(boolean),
      "targetSearchNetwork" -> optional(boolean),
      "targetContentNetwork" -> optional(boolean),
      "targetPartnerSearchNetwork" -> optional(boolean)
    )(CampaignForm.apply)(CampaignForm.unapply)
  )
}
