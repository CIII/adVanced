package helpers.google.mcc.account.campaign.adgroup

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._

object AdGroupControllerHelper {
  case class AdGroupParent(
    mccObjId: Option[String],
    customerApiId: Option[Long],
    campaignApiId: Option[Long]
  )

  def documentToAdGroupParent(dbo: Document) = AdGroupParent(
    mccObjId=Option(dbo.getString("mccObjId")),
    customerApiId=Option(dbo.getLong("customerApiId")).map(_.toLong),
    campaignApiId=Option(dbo.getLong("campaignApiId")).map(_.toLong)
  )

  def adGroupParentToDocument(agp: AdGroupParent) = Document(
    "mccObjId" -> agp.mccObjId,
    "customerApiId" -> agp.customerApiId,
    "campaignApiId" -> agp.campaignApiId
  )

  case class AdGroupForm(
    parent: AdGroupParent,
    apiId: Option[Long],
    name: String,
    status: String,
    maxCpc: Option[Double],
    contentBidCriterionTypeGroup: Option[String]
  )

  def documentToAdGroupForm(dbo: Document) = AdGroupForm(
    parent=documentToAdGroupParent(Option(dbo.toBsonDocument.get("parent")).map(v => Document(v.asDocument())).get),
    apiId=Option(dbo.getLong("apiId")).map(_.toLong),
    name=Option(dbo.getString("name")).getOrElse(""),
    status=Option(dbo.getString("status")).getOrElse(""),
    maxCpc=Option(dbo.getDouble("maxCpc")).map(_.toDouble),
    contentBidCriterionTypeGroup=Option(dbo.getString("contentBidCriterionTypeGroup"))
  )

  def adGroupFormToDocument(agf: AdGroupForm) = Document(
    "parent" -> adGroupParentToDocument(agf.parent),
    "apiId" -> agf.apiId,
    "name" -> agf.name,
    "status" -> agf.status,
    "maxCpc" -> agf.maxCpc,
    "contentBidCriterionTypeGroup" -> agf.contentBidCriterionTypeGroup
  )

  def adGroupForm: Form[AdGroupForm] = Form(
    mapping(
      "parent" -> mapping(
        "mccObjId" -> optional(text),
        "customerApiId" -> optional(longNumber),
        "campaignApiId" -> optional(longNumber)
      )(AdGroupParent.apply)(AdGroupParent.unapply),
      "apiId" -> optional(longNumber),
      "name" -> nonEmptyText,
      "status" -> text,
      "maxCpc" -> optional(of(doubleFormat)),
      "contentBidCriterionTypeGroup" -> optional(text)
    )(AdGroupForm.apply)(AdGroupForm.unapply)
  )
}
