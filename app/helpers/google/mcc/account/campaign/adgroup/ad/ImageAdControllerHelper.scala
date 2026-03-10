package helpers.google.mcc.account.campaign.adgroup.ad

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import controllers.Google.AdGroupAdForm
import play.api.data.Form
import play.api.data.Forms._
import scala.collection.immutable.List
import scala.jdk.CollectionConverters._

object ImageAdControllerHelper {
  case class ImageAdForm (
    parent: controllers.Google.AdGroupAdParent,
    apiId: Option[Long],
    url: Option[String],
    displayUrl: Option[String],
    devicePreference: Option[Long],
    imageName: String,
    imageFileSize: Option[Long],
    imageData: Option[Array[Byte]],
    status: Option[String],
    approvalStatus: Option[String],
    disapprovalReasons: Option[List[String]],
    trademarkDisapproved: Option[Boolean]
  ) extends AdGroupAdForm

  def imageAdFormToDocument(iaf: ImageAdForm) = Document(
    "parent" -> controllers.Google.adGroupAdParentToDocument(iaf.parent),
    "apiId" -> iaf.apiId,
    "url" -> iaf.url,
    "displayUrl" -> iaf.displayUrl,
    "devicePreference" -> iaf.devicePreference,
    "imageName" -> iaf.imageName,
    "imageFileSize" -> iaf.imageFileSize,
    "imageData" -> iaf.imageData,
    "status" -> iaf.status,
    "approvalStatus" -> iaf.approvalStatus,
    "disapprovalReasons" -> iaf.disapprovalReasons,
    "trademarkDisapproved" -> iaf.trademarkDisapproved
  )

  def documentToImageAdForm(dbo: Document) = ImageAdForm(
    parent = controllers.Google.documentToAdGroupAdParent(Option(dbo.toBsonDocument.get("parent")).map(v => Document(v.asDocument())).get),
    apiId = Option(dbo.getLong("apiId")).map(_.toLong),
    url = Option(dbo.getString("url")),
    displayUrl = Option(dbo.getString("displayUrl")),
    devicePreference = Option(dbo.getLong("devicePreference")).map(_.toLong),
    imageName = dbo.getString("imageName"),
    imageFileSize = Option(dbo.getLong("imageFileSize")).map(_.toLong),
    imageData = Option(dbo.get("imageData")).map(_.asInstanceOf[Array[Byte]]),
    status = Option(dbo.getString("status")),
    approvalStatus = Option(dbo.getString("approvalStatus")),
    disapprovalReasons = Option(dbo.getList("disapprovalReasons", classOf[String])).map(_.asScala.toList),
    trademarkDisapproved = Option(dbo.getBoolean("trademarkDisapproved")).map(_.booleanValue())
  )

  def imageAdForm: Form[ImageAdForm] = Form(
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
      "imageName" -> nonEmptyText,
      "imageFileSize" -> optional(longNumber),
      "imageData" -> optional(ignored(Array[Byte]())),
      "status" -> optional(text),
      "approvalStatus" -> optional(text),
      "disapprovalReasons" -> optional(list(text)),
      "trademarkDisapproved" -> optional(boolean)
      )(ImageAdForm.apply)(ImageAdForm.unapply)
  )
  
  case class ImageAdChartItem(
    val apiId: Long,
    val image: String,
    val url: String,
    val status: String
  )
  
  def getImageHtml(imageDataOpt: Option[Array[Byte]]): String = {
    imageDataOpt match {
      case Some(imageData) =>
        """<img src="/google/images/""" + new String(imageData, "UTF-8") + """ />"""
      case None => ""
    }
  }
}
