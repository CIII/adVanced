package helpers.facebook.api_account.campaign

import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import play.api.data.{Form, Forms}
import play.api.data.Forms.{longNumber, mapping, optional, text}

object CampaignControllerHelper {
  case class CampaignParent(
    var apiAccountObjId: Option[String]
  )

  def campaignParentToDbo(cp: CampaignParent) = DBObject(
    "apiAccountObjId" -> cp.apiAccountObjId
  )

  def dboToCampaignParent(dbo: DBObject) = CampaignParent(
    apiAccountObjId=dbo.getAsOrElse[Option[String]]("apiAccountObjId", None)
  )

  case class CampaignForm(
    var apiId: Option[String],
    var accountId: String,
    var adLabels: Option[List[String]],
    var budgetRebalanceFlag: Boolean,
    var buyingType: String,
    var canUseSpendCap: Boolean,
    var configuredStatus: String,
    var createdTime: Option[String],
    var effectiveStatus: String,
    var name: String,
    var objective: Option[String],
    var spendCap: Option[Long],
    var startTime: Option[DateTime],
    var status: Option[String],
    var stopTime: Option[DateTime],
    var updatedTime: Option[DateTime]
  )

  def campaignFormToDbo(cf: CampaignForm): DBObject = {
    DBObject(
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
      "startTime" -> cf.startTime,
      "status" -> cf.status,
      "stopTime" -> cf.stopTime,
      "updatedTime" -> cf.updatedTime
    )
  }

  def dboToCampaignForm(dbo: DBObject): CampaignForm = {
    CampaignForm(
      apiId = dbo.getAsOrElse[Option[String]]("apiId", None),
      accountId = dbo.get("accountId").asInstanceOf[String],
      adLabels = dbo.getAs[List[String]]("adLabels"),
      budgetRebalanceFlag = dbo.get("budgetRebalanceFlag").asInstanceOf[Boolean],
      buyingType = dbo.get("buyingType").asInstanceOf[String],
      canUseSpendCap = dbo.get("canUseSpendCap").asInstanceOf[Boolean],
      configuredStatus = dbo.get("configuredStatus").asInstanceOf[String],
      createdTime = dbo.getAs[String]("createdTime"),
      effectiveStatus = dbo.get("effectiveStatus").asInstanceOf[String],
      name = dbo.get("name").asInstanceOf[String],
      objective = dbo.getAs[String]("objective"),
      spendCap = dbo.getAs[Long]("spendCap"),
      startTime = dbo.getAs[DateTime]("startTime"),
      status = dbo.getAs[String]("status"),
      stopTime = dbo.getAs[DateTime]("stopTime"),
      updatedTime = dbo.getAs[DateTime]("updatedTime")
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
      "startTime" -> optional(Forms.jodaDate),
      "status" -> optional(text),
      "stopTime" -> optional(Forms.jodaDate),
      "updatedTime" -> optional(Forms.jodaDate)
    )(CampaignForm.apply)(CampaignForm.unapply)
  )
}