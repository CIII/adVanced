package controllers.google.mcc.account.campaign.adgroup.ad

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import helpers.google.mcc.account.campaign.adgroup.ad.TextAdControllerHelper
import models.mongodb._
import models.mongodb.MongoExtensions._
import models.mongodb.google.Google._
import play.api.i18n.I18nSupport
import play.api.mvc._
import security.HandlerKeys
import util.charts._
import util.charts.client.ChartColumn.ColumnDataType._
import util.charts.client.ChartColumn.ColumnType._
import util.charts.client._
import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import util.charts.client.ActionColumn
import util.charts.client.ChartColumn

class TextAdController @Inject()(
  val controllerComponents: ControllerComponents,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders
)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {

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
        googleAdCollection.namespace.getCollectionName,
        Some("adType" -> "TextAd")
      )))
  }

  def textAds = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[AdGroupAd] with Document-based access
      Future(Ok(views.html.google.mcc.account.campaign.adgroup.ad.text.text_ads(
        new ClientChart(
          List(
            new ChartColumn("apiId", "", "Api Id", number, dimension),
            new ChartColumn("url", "", "Url", string, dimension),
            new ChartColumn("headline", "", "Headline", string, dimension),
            new ActionColumn((rowValues: List[Any]) => "/google/mcc/account/campaign/adgroup/ad/text/%s/".format(rowValues(0).toString))
          ),
          googleAdCollection.find(Document("adType" -> "TextAd")).toList.map(
              textAdObj => {
                val adDoc = Option(textAdObj.toBsonDocument.get("ad")).map(v => Document(v.asDocument())).flatMap(d => Option(d.toBsonDocument.get("object")).map(v => Document(v.asDocument())))
                val innerAdDoc = adDoc.flatMap(d => Option(d.toBsonDocument.get("ad")).map(v => Document(v.asDocument())))
                TextAdChartItem(
                  apiId = innerAdDoc.flatMap(d => Option(d.getLong("id")).map(_.toLong)).getOrElse(0L),
                  url = innerAdDoc.flatMap(d => Option(d.getString("url"))).getOrElse(""),
                  headline = innerAdDoc.flatMap(d => Option(d.getString("headline"))).getOrElse("")
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
      googleAdCollection.findOne(Document("apiId" -> api_id)) match {
        case Some(text_ad_obj) =>
          // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[AdGroupAd] with Document-based access
          val adDoc = Option(text_ad_obj.toBsonDocument.get("ad")).map(v => Document(v.asDocument())).flatMap(d => Option(d.toBsonDocument.get("object")).map(v => Document(v.asDocument())))
          val innerAdDoc = adDoc.flatMap(d => Option(d.toBsonDocument.get("ad")).map(v => Document(v.asDocument())))
          Future(Ok(views.html.google.mcc.account.campaign.adgroup.ad.text.edit_text_ad(
            api_id,
            textAdForm.fill(
              TextAdForm(
                controllers.Google.AdGroupAdParent(
                  mccObjId = Option(text_ad_obj.getString("mccObjId")),
                  customerApiId = Option(text_ad_obj.getLong("customerApiId")).map(_.toLong),
                  campaignApiId = Option(text_ad_obj.getLong("campaignApiId")).map(_.toLong),
                  adGroupApiId = Option(text_ad_obj.getLong("adGroupApiId")).map(_.toLong)
                ),
                apiId = innerAdDoc.flatMap(d => Option(d.getLong("id")).map(_.toLong)),
                url = innerAdDoc.flatMap(d => Option(d.getString("url"))),
                displayUrl = innerAdDoc.flatMap(d => Option(d.getString("displayUrl"))),
                devicePreference = innerAdDoc.flatMap(d => Option(d.getLong("devicePreference")).map(_.toLong)),
                headline = innerAdDoc.flatMap(d => Option(d.getString("headline"))),
                description1 = innerAdDoc.flatMap(d => Option(d.getString("description1"))),
                description2 = innerAdDoc.flatMap(d => Option(d.getString("description2"))),
                status = adDoc.flatMap(d => Option(d.getString("status"))),
                approvalStatus = adDoc.flatMap(d => Option(d.getString("approvalStatus"))),
                disapprovalReasons = adDoc.flatMap(d => Option(d.getString("disapprovalReasons")).map(r => r.split(",").toList)),
                trademarkDisapproved = adDoc.flatMap(d => Option(d.getBoolean("trademarkDisapproved")).map(_.booleanValue()))
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
              changeData = textAdFormToDocument(text_ad)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.adgroup.ad.routes.TextAdController.textAds))
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
                    changeData = textAdFormToDocument(text_ad)
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
        Future(Redirect(controllers.google.mcc.account.campaign.adgroup.ad.routes.TextAdController.textAds))
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
              changeData = textAdFormToDocument(text_ad)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.adgroup.ad.routes.TextAdController.textAds))
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
          changeData = Document("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.google.mcc.account.campaign.adgroup.ad.routes.TextAdController.textAds))
  }
}
