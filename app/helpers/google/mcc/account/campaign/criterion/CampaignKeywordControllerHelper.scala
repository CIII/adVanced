package helpers.google.mcc.account.campaign.criterion

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._

object CampaignKeywordControllerHelper {

  case class CampaignKeywordForm(
    parent: controllers.Google.CampaignCriterionParent,
    apiId: Option[Long],
    isNegative: Option[Boolean],
    text: String,
    matchType: String,
    bidModifier: Option[Double]
  )

  def documentToCampaignKeywordForm(dbo: Document): CampaignKeywordForm = {
    CampaignKeywordForm(
      parent=controllers.Google.documentToCampaignCriterionParent(Option(dbo.toBsonDocument.get("parent")).filter(_.isDocument).map(v => Document(v.asDocument())).get),
      apiId=Option(dbo.getLong("apiId")).map(_.toLong),
      isNegative=Option(dbo.getBoolean("isNegative")).map(_.booleanValue()),
      text=Option(dbo.getString("text")).getOrElse(""),
      matchType=Option(dbo.getString("matchType")).getOrElse(""),
      bidModifier=Option(dbo.getDouble("bidModifier")).map(_.toDouble)
    )
  }

  def campaignKeywordFormToDocument(ckf: CampaignKeywordForm): Document = {
    Document(
      "parent" -> controllers.Google.campaignCriterionParentToDocument(ckf.parent),
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
