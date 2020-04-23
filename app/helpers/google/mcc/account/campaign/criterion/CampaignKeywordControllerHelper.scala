package helpers.google.mcc.account.campaign.criterion

import com.mongodb.casbah.Imports._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._

object CampaignKeywordControllerHelper {

  case class CampaignKeywordForm(
    var parent: controllers.Google.CampaignCriterionParent,
    var apiId: Option[Long],
    var isNegative: Option[Boolean],
    var text: String,
    var matchType: String,
    var bidModifier: Option[Double]
  )

  def dboToCampaignKeywordForm(dbo: DBObject): CampaignKeywordForm = {
    CampaignKeywordForm(
      parent=controllers.Google.dboToCampaignCriterionParent(dbo.getAs[DBObject]("parent").get),
      apiId=dbo.getAsOrElse[Option[Long]]("apiId", None),
      isNegative=dbo.getAsOrElse[Option[Boolean]]("isNegative", None),
      text=dbo.getAsOrElse[String]("text", ""),
      matchType=dbo.getAsOrElse[String]("matchType", ""),
      bidModifier=dbo.getAsOrElse[Option[Double]]("bidModifier", None)
    )
  }

  def campaignKeywordFormToDbo(ckf: CampaignKeywordForm): DBObject = {
    DBObject(
      "parent" -> controllers.Google.campaignCriterionParentToDbo(ckf.parent),
      "apiId" -> ckf.apiId,
      "isNegative" -> ckf.isNegative,
      "text" -> ckf.text,
      "matchType" -> ckf.matchType,
      "bidModifier" -> ckf.bidModifier
    )
  }

  def campaignKeywordForm: Form[CampaignKeywordForm] = Form(
    mapping(
      "parent" -> mapping(
        "mccObjId" -> optional(text),
        "customerApiId" -> optional(longNumber),
        "campaignApiId" -> optional(longNumber)
      )(controllers.Google.CampaignCriterionParent.apply)(controllers.Google.CampaignCriterionParent.unapply),
      "apiId" -> optional(longNumber),
      "isNegative" -> optional(boolean),
      "text" -> nonEmptyText,
      "matchType" -> nonEmptyText,
      "bidModifier" -> optional(of[Double])
    )(CampaignKeywordForm.apply)(CampaignKeywordForm.unapply)
  )
}
