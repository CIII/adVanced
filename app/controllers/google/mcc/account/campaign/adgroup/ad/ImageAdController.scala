package controllers.google.mcc.account.campaign.adgroup.ad

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import helpers.google.mcc.account.campaign.adgroup.ad.ImageAdControllerHelper._
import models.mongodb._
import models.mongodb.MongoExtensions._
import models.mongodb.google.Google._
import play.api.i18n.I18nSupport
import play.api.mvc._
import security.HandlerKeys
import scala.concurrent.ExecutionContext
import util.charts._
import util.charts.client.ChartColumn.ColumnDataType._
import util.charts.client.ChartColumn.ColumnType._
import util.charts.client._
import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import util.charts.client.ActionColumn
import util.charts.client.ChartColumn

class ImageAdController @Inject()(
  val controllerComponents: ControllerComponents,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders
)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {
  def json = Action.async {
    implicit request =>
      Future(Ok(controllers.json(
        request,
        List(
          "mccObjId",
          "customerObjId",
          "customerApiId",
          "campaignObjId",
          "campaignApiId",
          "adGroupObjId",
          "adGroupApiId",
          "adObjId",
          "adApiId"
        ),
        "ad",
        googleAdCollection.namespace.getCollectionName,
        Some("adType" -> "ImageAd")
      )))
  }

  def imageAds = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      // TODO: Migrate to Google Ads API v18 - replaced AdGroupAd/ImageAd deserialization with Document-based access
      Future(Ok(views.html.google.mcc.account.campaign.adgroup.ad.image.image_ads(
        new ClientChart(
          List(
            new ChartColumn("apiId", "", "Api Id", number, dimension),
            new ChartColumn("url", "", "Url", string, dimension),
            new ChartColumn("status", "", "Status", string, dimension),
            new ChartColumn("image", "", "Image", string, dimension),
            new ActionColumn((rowValues: List[Any]) => "/google/mcc/account/campaign/adgroup/ad/image/%s/".format(rowValues(0).toString))
          ),
          googleAdCollection.find(Document("adType" -> "ImageAd")).toList.map(
            y => {
              val adDoc = Option(y.toBsonDocument.get("ad")).map(v => Document(v.asDocument())).flatMap(d => Option(d.toBsonDocument.get("object")).map(v => Document(v.asDocument())))
              val innerAdDoc = adDoc.flatMap(d => Option(d.toBsonDocument.get("ad")).map(v => Document(v.asDocument())))
              val imageDoc = innerAdDoc.flatMap(d => Option(d.toBsonDocument.get("image")).map(v => Document(v.asDocument())))

              ImageAdChartItem(
                apiId = innerAdDoc.flatMap(d => Option(d.getLong("id")).map(_.toLong)).getOrElse(0L),
                url = innerAdDoc.flatMap(d => Option(d.getString("url"))).getOrElse(""),
                status = adDoc.flatMap(d => Option(d.getString("status"))).getOrElse("UNKNOWN"),
                image = getImageHtml(imageDoc.flatMap(d => Option(d.getString("data"))).map(_.getBytes("UTF-8")))
              )
            }
          )
        ),
        pendingCache(Left(request))
          .filter(x =>
            x.trafficSource == TrafficSource.GOOGLE
              && x.changeCategory == ChangeCategory.IMAGE_AD
          )
      )))
  }

  def newImageAd = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.campaign.adgroup.ad.image.new_image_ad(
        imageAdForm,
        List()
      )))
  }

  def editImageAd(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      googleAdCollection.findOne(Document("apiId" -> api_id)) match {
        case Some(image_ad_obj) =>
          // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[AdGroupAd] with Document-based access
          val adDoc = Option(image_ad_obj.toBsonDocument.get("ad")).map(v => Document(v.asDocument())).flatMap(d => Option(d.toBsonDocument.get("object")).map(v => Document(v.asDocument())))
          val innerAdDoc = adDoc.flatMap(d => Option(d.toBsonDocument.get("ad")).map(v => Document(v.asDocument())))
          val imageDoc = innerAdDoc.flatMap(d => Option(d.toBsonDocument.get("image")).map(v => Document(v.asDocument())))
          val adId = innerAdDoc.flatMap(d => Option(d.getLong("id")).map(_.toLong)).getOrElse(0L)
          Future(Ok(views.html.google.mcc.account.campaign.adgroup.ad.image.edit_image_ad(
            adId,
            imageAdForm.fill(
              ImageAdForm(
                controllers.Google.AdGroupAdParent(
                  mccObjId = Option(image_ad_obj.getString("mccObjId")),
                  customerApiId = Option(image_ad_obj.getLong("customerApiId")).map(_.toLong),
                  campaignApiId = Option(image_ad_obj.getLong("campaignApiId")).map(_.toLong),
                  adGroupApiId = Option(image_ad_obj.getLong("adGroupApiId")).map(_.toLong)
                ),
                apiId = Some(adId),
                url = innerAdDoc.flatMap(d => Option(d.getString("displayUrl"))),
                displayUrl = innerAdDoc.flatMap(d => Option(d.getString("displayUrl"))),
                devicePreference = innerAdDoc.flatMap(d => Option(d.getLong("devicePreference")).map(_.toLong)),
                imageName = innerAdDoc.flatMap(d => Option(d.getString("name"))).getOrElse(""),
                imageFileSize = imageDoc.flatMap(d => Option(d.getLong("fileSize")).map(_.toLong)),
                imageData = imageDoc.flatMap(d => Option(d.getString("data"))).map(_.getBytes("UTF-8")),
                status = adDoc.flatMap(d => Option(d.getString("status"))),
                approvalStatus = adDoc.flatMap(d => Option(d.getString("approvalStatus"))),
                disapprovalReasons = adDoc.flatMap(d => Option(d.getString("disapprovalReasons")).map(r => r.split(",").toList)),
                trademarkDisapproved = adDoc.flatMap(d => Option(d.getBoolean("trademarkDisapproved")).map(_.booleanValue()))
              )
            )
          )))
        case None =>
          Future(BadRequest("NOT FOUND"))
      }
  }


  def image(byteString: String) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
    val MimeType = "image/png"
    try {
      val imageData: Array[Byte] = byteString.getBytes("UTF-8")
      Future(Ok(imageData).as(MimeType))
    }
    catch {
      case e: IllegalArgumentException =>
        Future(BadRequest("Could not generate image. Error: " + e.toString))
    }
  }


  def createImageAd = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      imageAdForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.google.mcc.account.campaign.adgroup.ad.image.new_image_ad(
              formWithErrors,
              List()
            )
          ))
        },
        image_ad => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.IMAGE_AD,
              changeData = imageAdFormToDocument(image_ad)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.adgroup.ad.routes.ImageAdController.imageAds))
        }
      )
  }


  def bulkNewImageAd = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request =>
      var error_list = new ListBuffer[String]()
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[ImageAdForm]
          val image_ad_data_list = Utilities.bulkImport(bulk, field_names)
          for (((image_ad_data, action), index) <- image_ad_data_list.zipWithIndex) {
            imageAdForm.bind(image_ad_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              image_ad =>
                setPendingCache(
                  Left(request),
                  pendingCache(Left(request)) :+ PendingCacheStructure(
                    id = pendingCache(Left(request)).length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.GOOGLE,
                    changeCategory = ChangeCategory.IMAGE_AD,
                    changeData = imageAdFormToDocument(image_ad)
                  )
                )
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.google.mcc.account.campaign.adgroup.ad.image.new_image_ad(
          imageAdForm,
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.google.mcc.account.campaign.adgroup.ad.routes.ImageAdController.imageAds))
      }
  }


  def saveImageAd(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      imageAdForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.google.mcc.account.campaign.adgroup.ad.image.edit_image_ad(
              api_id,
              formWithErrors
            )
          ))
        },
        image_ad => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.IMAGE_AD,
              changeData = imageAdFormToDocument(image_ad)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.adgroup.ad.routes.ImageAdController.imageAds))
        }
      )
  }


  def deleteImageAd(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
          id = pendingCache(Left(request)).length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.GOOGLE,
          changeCategory = ChangeCategory.IMAGE_AD,
          changeData = Document("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.google.mcc.account.campaign.adgroup.ad.routes.ImageAdController.imageAds))
  }
}
