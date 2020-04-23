package controllers.google.mcc.account.campaign.adgroup.ad

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.google.api.ads.adwords.axis.v201609.cm.{AdGroupAd, TextAd}
import com.mongodb.casbah.Imports._
import helpers.google.mcc.account.campaign.adgroup.ad.TextAdControllerHelper
import models.mongodb._
import models.mongodb.google.Google._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import security.HandlerKeys
import util.charts._
import util.charts.client.ChartColumn.ColumnDataType._
import util.charts.client.ChartColumn.ColumnType._
import util.charts.client._
import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import util.charts.client.ActionColumn
import util.charts.client.ChartColumn

class TextAdController @Inject()(
  val messagesApi: MessagesApi, 
  deadbolt: DeadboltActions, 
  handlers: HandlerCache, 
  actionBuilder: ActionBuilders
) extends Controller with I18nSupport {

  import TextAdControllerHelper._

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
          "adApiId",
          "tsecs"
        ),
        "ad",
        googleAdCollection,
        Some("adType" -> "TextAd")
      )))
  }

  def textAds = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.campaign.adgroup.ad.text.text_ads(
        new ClientChart(
          List(
            new ChartColumn("apiId", "", "Api Id", number, dimension),
            new ChartColumn("url", "", "Url", string, dimension),
            new ChartColumn("headline", "", "Headline", string, dimension),
            new ActionColumn((rowValues: List[Any]) => "/google/mcc/account/campaign/adgroup/ad/text/%s/".format(rowValues(0).toString))
          ),
          googleAdCollection.find(DBObject("adType" -> "TextAd")).toList.map(
              textAdObj => {
                val ad = dboToGoogleEntity[AdGroupAd](textAdObj, "ad", None)
                TextAdChartItem(
                  apiId = ad.getAd.getId,
                  url = ad.getAd.getUrl,
                  headline = ad.getAd.asInstanceOf[TextAd].getHeadline
                )
              }
          )
        ),
        pendingCache(Left(request))
          .filter(x =>
            x.trafficSource == TrafficSource.GOOGLE
              && x.changeCategory == ChangeCategory.TEXT_AD
          )
      )))
  }

  def newTextAd = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.campaign.adgroup.ad.text.new_text_ad(
        textAdForm,
        List()
      )))
  }

  def editTextAd(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      googleAdCollection.findOne(DBObject("apiId" -> api_id)) match {
        case Some(text_ad_obj) =>
          val text_ad = dboToGoogleEntity[AdGroupAd](text_ad_obj, "ad", None)
          Future(Ok(views.html.google.mcc.account.campaign.adgroup.ad.text.edit_text_ad(
            api_id,
            textAdForm.fill(
              TextAdForm(
                controllers.Google.AdGroupAdParent(
                  mccObjId = text_ad_obj.getAsOrElse[Option[String]]("mccObjId", None),
                  customerApiId = text_ad_obj.getAsOrElse[Option[Long]]("customerApiId", None),
                  campaignApiId = text_ad_obj.getAsOrElse[Option[Long]]("campaignApiId", None),
                  adGroupApiId = text_ad_obj.getAsOrElse[Option[Long]]("adGroupApiId", None)
                ),
                apiId = Some(text_ad.getAd.getId),
                url = Some(text_ad.getAd.getUrl),
                displayUrl = Some(text_ad.getAd.getDisplayUrl),
                devicePreference = Some(text_ad.getAd.getDevicePreference),
                headline = Some(text_ad.getAd.asInstanceOf[com.google.api.ads.adwords.axis.v201609.cm.TextAd].getHeadline),
                description1 = Some(text_ad.getAd.asInstanceOf[com.google.api.ads.adwords.axis.v201609.cm.TextAd].getDescription1),
                description2 = Some(text_ad.getAd.asInstanceOf[com.google.api.ads.adwords.axis.v201609.cm.TextAd].getDescription2),
                status = Some(text_ad.getStatus.toString),
                approvalStatus = Some(text_ad.getApprovalStatus.toString),
                disapprovalReasons = Some(text_ad.getDisapprovalReasons.toList),
                trademarkDisapproved = Some(text_ad.getTrademarkDisapproved)
              )
            )
          )))
        case None =>
          Future(BadRequest("Not Found"))
      }
  }


  def createTextAd = deadbolt.Dynamic(name=PermissionGroup.GoogleWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      textAdForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.google.mcc.account.campaign.adgroup.ad.text.new_text_ad(
              formWithErrors,
              List()
            )
          ))
        },
        text_ad => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.TEXT_AD,
              changeData = textAdFormToDbo(text_ad)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.adgroup.ad.routes.TextAdController.textAds()))
        }
      )
  }


  def bulkNewTextAd = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[TextAdForm]
          val text_ad_data_list = Utilities.bulkImport(bulk, field_names)
          for (((text_ad_data, action), index) <- text_ad_data_list.zipWithIndex) {
            textAdForm.bind(text_ad_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              text_ad => {
                setPendingCache(
                  Left(request),
                  pendingCache(Left(request)) :+ PendingCacheStructure(
                    id = pendingCache(Left(request)).length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.GOOGLE,
                    changeCategory = ChangeCategory.TEXT_AD,
                    changeData = textAdFormToDbo(text_ad)
                  )
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.google.mcc.account.campaign.adgroup.ad.text.new_text_ad(
          textAdForm,
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.google.mcc.account.campaign.adgroup.ad.routes.TextAdController.textAds()))
      }
    }
  }


  def saveTextAd(api_id: Long) = deadbolt.Dynamic(name=PermissionGroup.GoogleWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      textAdForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(views.html.google.mcc.account.campaign.adgroup.ad.text.edit_text_ad(api_id, formWithErrors))),
        text_ad => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.TEXT_AD,
              changeData = textAdFormToDbo(text_ad)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.adgroup.ad.routes.TextAdController.textAds()))
        }
      )
  }


  def deleteTextAd(api_id: Long) = deadbolt.Dynamic(name=PermissionGroup.GoogleWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
          id = pendingCache(Left(request)).length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.GOOGLE,
          changeCategory = ChangeCategory.TEXT_AD,
          changeData = DBObject("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.google.mcc.account.campaign.adgroup.ad.routes.TextAdController.textAds()))
  }
}


