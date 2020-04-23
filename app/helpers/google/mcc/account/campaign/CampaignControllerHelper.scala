package helpers.google.mcc.account.campaign

import com.mongodb.casbah.Imports._
import play.api.data.Form
import play.api.data.Forms._
import models.mongodb.google.Google._

object CampaignControllerHelper {
  case class CampaignParent(
    var mccObjId: Option[String],
    var customerApiId: Option[Long]
  )

  def campaignParentToDbo(cp: CampaignParent) = DBObject(
    "mccObjId" -> cp.mccObjId,
    "customerApiId" -> cp.customerApiId
  )

  def dboToCampaignParent(dbo: DBObject) = CampaignParent(
    mccObjId=dbo.getAsOrElse[Option[String]]("mccObjId", None),
    customerApiId=dbo.getAsOrElse[Option[Long]]("customerApiId", None)
  )

  case class CampaignForm(
    var parent: CampaignParent,
    var apiId: Option[Long],
    var name: String,
    var status: Option[String],
    var servingStatus: Option[String],
    var startDate: Option[String],
    var endDate: Option[String],
    var budgetAmount: Option[Int],
    var isSharedBudget: Option[Boolean],
    var advertisingChannelType: Option[String],
    var adServingOptimizationStatus: Option[String],
    var frequencyCapImpressions: Option[Long],
    var frequencyCapTimeUnit: Option[String],
    var frequencyCapLevel: Option[String],
    var targetGoogleSearch: Option[Boolean],
    var targetSearchNetwork: Option[Boolean],
    var targetContentNetwork: Option[Boolean],
    var targetPartnerSearchNetwork: Option[Boolean]
  )

  def campaignFormToDbo(cf: CampaignForm): DBObject = {
    DBObject(
      "parent" -> campaignParentToDbo(cf.parent),
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

  def dboToCampaignForm(dbo: DBObject): CampaignForm = {
    CampaignForm(
      parent=dboToCampaignParent(dbo.getAs[DBObject]("parent").get),
      apiId=dbo.getAsOrElse[Option[Long]]("apiId", None),
      name=dbo.getAs[String]("name").get,
      status=dbo.getAs[String]("status"),
      servingStatus=dbo.getAs[String]("servingStatus"),
      startDate=dbo.getAs[String]("startDate"),
      endDate=dbo.getAs[String]("endDate"),
      budgetAmount=dbo.getAs[Int]("budgetAmount"),
      isSharedBudget=dbo.getAs[Boolean]("isSharedBudget"),
      advertisingChannelType=dbo.getAs[String]("advertisingChannelType"),
      adServingOptimizationStatus=dbo.getAs[String]("adServingOptimizationStatus"),
      frequencyCapImpressions=dbo.getAs[Long]("frequencyCapImpressions"),
      frequencyCapTimeUnit=dbo.getAs[String]("frequencyCapTimeUnit"),
      frequencyCapLevel=dbo.getAs[String]("frequencyCapLevel"),
      targetGoogleSearch=dbo.getAs[Boolean]("targetGoogleSearch"),
      targetSearchNetwork=dbo.getAs[Boolean]("targetSearchNetwork"),
      targetContentNetwork=dbo.getAs[Boolean]("targetContentNetwork"),
      targetPartnerSearchNetwork=dbo.getAs[Boolean]("targetPartnerSearchNetwork")
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
