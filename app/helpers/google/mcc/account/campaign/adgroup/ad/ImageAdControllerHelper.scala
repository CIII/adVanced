package helpers.google.mcc.account.campaign.adgroup.ad

import com.mongodb.casbah.Imports._
import controllers.Google.AdGroupAdForm
import play.api.data.Form
import play.api.data.Forms._
import scala.collection.immutable.List

object ImageAdControllerHelper {
  case class ImageAdForm (
    var parent: controllers.Google.AdGroupAdParent,
    var apiId: Option[Long],
    var url: Option[String],
    var displayUrl: Option[String],
    var devicePreference: Option[Long],
    var imageName: String,
    var imageFileSize: Option[Long],
    var imageData: Option[Array[Byte]],
    var status: Option[String],
    var approvalStatus: Option[String],
    var disapprovalReasons: Option[List[String]],
    var trademarkDisapproved: Option[Boolean]
  ) extends AdGroupAdForm

  def imageAdFormToDbo(iaf: ImageAdForm) = DBObject(
    "parent" -> controllers.Google.adGroupAdParentToDbo(iaf.parent),
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

  def dboToImageAdForm(dbo: DBObject) = ImageAdForm(
    parent = controllers.Google.dboToAdGroupAdParent(dbo.as[DBObject]("parent")),
    apiId = dbo.getAsOrElse[Option[Long]]("apiId", None),
    url = dbo.getAsOrElse[Option[String]]("url", None),
    displayUrl = dbo.getAsOrElse[Option[String]]("displayUrl", None),
    devicePreference = dbo.getAsOrElse[Option[Long]]("devicePreference", None),
    imageName = dbo.getAs[String]("imageName").get,
    imageFileSize = dbo.getAsOrElse[Option[Long]]("imageFileSize", None),
    imageData = dbo.getAsOrElse[Option[Array[Byte]]]("imageData", None),
    status = dbo.getAsOrElse[Option[String]]("status", None),
    approvalStatus = dbo.getAsOrElse[Option[String]]("approvalStatus", None),
    disapprovalReasons = dbo.getAsOrElse[Option[List[String]]]("disapprovalReasons", None),
    trademarkDisapproved = dbo.getAsOrElse[Option[Boolean]]("trademarkDisapproved", None)
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
