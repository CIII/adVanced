package controllers.google.mcc.account.campaign.adgroup.ad

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.google.api.ads.adwords.axis.v201609.cm.{AdGroupAd, ImageAd}
import com.mongodb.casbah.Imports._
import helpers.google.mcc.account.campaign.adgroup.ad.ImageAdControllerHelper._
import models.mongodb._
import models.mongodb.google.Google._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import security.HandlerKeys
import scala.concurrent.ExecutionContext.Implicits.global
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
  val messagesApi: MessagesApi,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders
) extends Controller with I18nSupport {
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
        googleAdCollection,
        Some("adType" -> "ImageAd")
      )))
  }

  def imageAds = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.campaign.adgroup.ad.image.image_ads(
        new ClientChart(
          List(
            new ChartColumn("apiId", "", "Api Id", number, dimension),
            new ChartColumn("url", "", "Url", string, dimension),
            new ChartColumn("status", "", "Status", string, dimension),
            new ChartColumn("image", "", "Image", string, dimension),
            new ActionColumn((rowValues: List[Any]) => "/google/mcc/account/campaign/adgroup/ad/image/%s/".format(rowValues(0).toString))
          ),
          googleAdCollection.find(DBObject("adType" -> "ImageAd")).toList.map(
            y => {
              var imageAd: AdGroupAd = gson.fromJson(
                com.mongodb.util.JSON.serialize(y.as[MongoDBList]("ad").head.asInstanceOf[DBObject].as[DBObject]("object")),
                classOf[AdGroupAd]
              )
              
              ImageAdChartItem(
                apiId = imageAd.getAd.getId,
                url = imageAd.getAd.getUrl,
                status = imageAd.getStatus.toString,
                image = getImageHtml(Option(imageAd.getAd.asInstanceOf[ImageAd].getImage.getData ))
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
      googleAdCollection.findOne(DBObject("apiId" -> api_id)) match {
        case Some(image_ad_obj) =>
          val image_ad = dboToGoogleEntity[AdGroupAd](image_ad_obj, "ad", None)
          Future(Ok(views.html.google.mcc.account.campaign.adgroup.ad.image.edit_image_ad(
            image_ad.getAd.getId,
            imageAdForm.fill(
              ImageAdForm(
                controllers.Google.AdGroupAdParent(
                  mccObjId = image_ad_obj.getAsOrElse[Option[String]]("mccObjId", None),
                  customerApiId = image_ad_obj.getAsOrElse[Option[Long]]("customerApiId", None),
                  campaignApiId = image_ad_obj.getAsOrElse[Option[Long]]("campaignApiId", None),
                  adGroupApiId = image_ad_obj.getAsOrElse[Option[Long]]("adGroupApiId", None)
                ),
                apiId = Some(image_ad.getAd.getId),
                url = Some(image_ad.getAd.getDisplayUrl),
                displayUrl = Some(image_ad.getAd.getDisplayUrl),
                devicePreference = Some(image_ad.getAd.getDevicePreference),
                imageName = image_ad.getAd.asInstanceOf[ImageAd].getName,
                imageFileSize = Some(image_ad.getAd.asInstanceOf[ImageAd].getImage.getFileSize),
                imageData = Some(image_ad.getAd.asInstanceOf[ImageAd].getImage.getData),
                status = Some(image_ad.getStatus.toString),
                approvalStatus = Some(image_ad.getApprovalStatus.toString),
                disapprovalReasons = Some(image_ad.getDisapprovalReasons.toList),
                trademarkDisapproved = Some(image_ad.getTrademarkDisapproved)
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
              changeData = imageAdFormToDbo(image_ad)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.adgroup.ad.routes.ImageAdController.imageAds()))
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
                    changeData = imageAdFormToDbo(image_ad)
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
        Future(Redirect(controllers.google.mcc.account.campaign.adgroup.ad.routes.ImageAdController.imageAds()))
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
              changeData = imageAdFormToDbo(image_ad)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.adgroup.ad.routes.ImageAdController.imageAds()))
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
          changeData = DBObject("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.google.mcc.account.campaign.adgroup.ad.routes.ImageAdController.imageAds()))
  }
}


