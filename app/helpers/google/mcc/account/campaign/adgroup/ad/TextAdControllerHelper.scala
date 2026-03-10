package helpers.google.mcc.account.campaign.adgroup.ad

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import controllers.Google.AdGroupAdForm
import play.api.data.Form
import play.api.data.Forms._

import scala.collection.immutable.List
import scala.jdk.CollectionConverters._

object TextAdControllerHelper {
  case class TextAdForm(
    parent: controllers.Google.AdGroupAdParent,
    apiId: Option[Long],
    url: Option[String],
    displayUrl: Option[String],
    devicePreference: Option[Long],
    headline: Option[String],
    description1: Option[String],
    description2: Option[String],
    status: Option[String],
    approvalStatus: Option[String],
    disapprovalReasons: Option[List[String]],
    trademarkDisapproved: Option[Boolean]
  ) extends AdGroupAdForm

  def documentToTextAdForm(dbo: Document) = TextAdForm(
    parent = controllers.Google.documentToAdGroupAdParent(Option(dbo.toBsonDocument.get("parent")).map(v => Document(v.asDocument())).get),
    apiId = Option(dbo.getLong("apiId")).map(_.toLong),
    url = Option(dbo.getString("url")),
    displayUrl = Option(dbo.getString("displayUrl")),
    devicePreference = Option(dbo.getLong("devicePreference")).map(_.toLong),
    headline = Option(dbo.getString("headline")),
    description1 = Option(dbo.getString("description1")),
    description2 = Option(dbo.getString("description2")),
    status = Option(dbo.getString("status")),
    approvalStatus = Option(dbo.getString("approvalStatus")),
    disapprovalReasons = Option(dbo.getList("disapprovalReasons", classOf[String])).map(_.asScala.toList),
    trademarkDisapproved = Option(dbo.getBoolean("trademarkDisapproved")).map(_.booleanValue())
  )

  def textAdFormToDocument(taf: TextAdForm) = Document(
    "parent" -> controllers.Google.adGroupAdParentToDocument(taf.parent),
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
