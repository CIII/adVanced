package helpers.google.mcc.account.campaign.adgroup

import com.mongodb.casbah.Imports._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._

object AdGroupControllerHelper {
  case class AdGroupParent(
    var mccObjId: Option[String],
    var customerApiId: Option[Long],
    var campaignApiId: Option[Long]
  )

  def dboToAdGroupParent(dbo: DBObject) = AdGroupParent(
    mccObjId=dbo.getAsOrElse[Option[String]]("mccObjId", None),
    customerApiId=dbo.getAsOrElse[Option[Long]]("customerApiId", None),
    campaignApiId=dbo.getAsOrElse[Option[Long]]("campaignApiId", None)
  )

  def adGroupParentToDbo(agp: AdGroupParent) = DBObject(
    "mccObjId" -> agp.mccObjId,
    "customerApiId" -> agp.customerApiId,
    "campaignApiId" -> agp.campaignApiId
  )

  case class AdGroupForm(
    var parent: AdGroupParent,
    var apiId: Option[Long],
    var name: String,
    var status: String,
    var maxCpc: Option[Double],
    var contentBidCriterionTypeGroup: Option[String]
  )

  def dboToAdGroupForm(dbo: DBObject) = AdGroupForm(
    parent=dboToAdGroupParent(dbo.as[DBObject]("parent")),
    apiId=dbo.getAsOrElse[Option[Long]]("apiId", None),
    name=dbo.getAsOrElse[String]("name", ""),
    status=dbo.getAsOrElse[String]("status", ""),
    maxCpc=dbo.getAsOrElse[Option[Double]]("maxCpc", None),
    contentBidCriterionTypeGroup=dbo.getAsOrElse[Option[String]]("contentBidCriterionTypeGroup", None)
  )

  def adGroupFormToDbo(agf: AdGroupForm) = DBObject(
    "parent" -> adGroupParentToDbo(agf.parent),
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
