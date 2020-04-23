package helpers.google.mcc.account.campaign.adgroup.ad

import com.mongodb.casbah.Imports._
import controllers.Google.AdGroupAdForm
import play.api.data.Form
import play.api.data.Forms._

import scala.collection.immutable.List

object TextAdControllerHelper {
  case class TextAdForm(
    var parent: controllers.Google.AdGroupAdParent,
    var apiId: Option[Long],
    var url: Option[String],
    var displayUrl: Option[String],
    var devicePreference: Option[Long],
    var headline: Option[String],
    var description1: Option[String],
    var description2: Option[String],
    var status: Option[String],
    var approvalStatus: Option[String],
    var disapprovalReasons: Option[List[String]],
    var trademarkDisapproved: Option[Boolean]
  ) extends AdGroupAdForm

  def dboToTextAdForm(dbo: DBObject) = TextAdForm(
    parent = controllers.Google.dboToAdGroupAdParent(dbo.as[DBObject]("parent")),
    apiId = dbo.getAsOrElse[Option[Long]]("apiId", None),
    url = dbo.getAsOrElse[Option[String]]("url", None),
    displayUrl = dbo.getAsOrElse[Option[String]]("displayUrl", None),
    devicePreference = dbo.getAsOrElse[Option[Long]]("devicePreference", None),
    headline = dbo.getAsOrElse[Option[String]]("headline", None),
    description1 = dbo.getAsOrElse[Option[String]]("description1", None),
    description2 = dbo.getAsOrElse[Option[String]]("description2", None),
    status = dbo.getAsOrElse[Option[String]]("status", None),
    approvalStatus = dbo.getAsOrElse[Option[String]]("approvalStatus", None),
    disapprovalReasons = dbo.getAsOrElse[Option[List[String]]]("disapprovalReasons", None),
    trademarkDisapproved = dbo.getAsOrElse[Option[Boolean]]("trademarkDisapproved", None)
  )

  def textAdFormToDbo(taf: TextAdForm) = DBObject(
    "parent" -> controllers.Google.adGroupAdParentToDbo(taf.parent),
    "apiId" -> taf.apiId,
    "url" -> taf.url,
    "displayUrl" -> taf.displayUrl,
    "devicePreference" -> taf.devicePreference,
    "headline" -> taf.headline,
    "description1" -> taf.description1,
    "description2" -> taf.description2,
    "status" -> taf.status,
    "approvalStatus" -> taf.approvalStatus,
    "disapprovalReasons" -> taf.disapprovalReasons,
    "trademarkDisapproved" -> taf.trademarkDisapproved
  )

  def textAdForm: Form[TextAdForm] = Form(
    mapping(
      "parent" -> mapping(
        "mccObjId" -> optional(text),
        "customerApiId" -> optional(longNumber),
        "campaignApiId" -> optional(longNumber),
        "adGroupApiId" -> optional(longNumber)
      )(controllers.Google.AdGroupAdParent.apply)(controllers.Google.AdGroupAdParent.unapply),
      "apiId" -> optional(longNumber),
      "url" -> optional(text),
      "displayUrl" -> optional(text),
      "devicePreference" -> optional(longNumber),
      "headline" -> optional(text),
      "description1" -> optional(text),
      "description2" -> optional(text),
      "status" -> optional(text),
      "approvalStatus" -> optional(text),
      "disapprovalReasons" -> optional(list(text)),
      "trademarkDisapproved" -> optional(boolean)
    )(TextAdForm.apply)(TextAdForm.unapply)
  )
  
  case class TextAdChartItem(
    val apiId: Long,
    val url: String,
    val headline: String
  )
}
