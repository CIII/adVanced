package controllers.google.mcc.account.campaign.adgroup

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.google.api.ads.adwords.axis.v201609.cm.{AdGroup, Campaign}
import com.mongodb.casbah.Imports._
import helpers.google.mcc.account.campaign.CampaignControllerHelper._
import helpers.google.mcc.account.campaign.adgroup.AdGroupControllerHelper._
import models.mongodb._
import models.mongodb.google.Google._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import security.HandlerKeys
import util.charts.ChartMetaData.getMetaData
import util.charts._
import util.charts.client.{ActionColumn, ChartColumn}
import util.charts.client.ChartColumn.ColumnDataType._
import util.charts.client.ChartColumn.ColumnType._
import util.charts.performance.GooglePerformanceCharts._
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import com.google.api.ads.adwords.lib.selectorfields.v201609.cm.AdGroupField
import com.google.api.ads.adwords.axis.v201609.cm.CpcBid
import models.mongodb.google.GoogleAdGroupPerformance
import models.mongodb.google.GoogleCampaignPerformance
import models.mongodb.performance.PerformanceEntityFilter
import models.mongodb.google.GooglePerformance

class AdGroupController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {

  def json = Action {
    implicit request =>
      Ok(controllers.json(
        request,
        List(
          "mccObjId",
          "customerObjId",
          "customerApiId",
          "campaignObjId",
          "campaignApiId",
          "adGroupObjId",
          "adGroupApiId"
        ),
        "adGroup",
        googleAdGroupCollection
      ))
  }
  
  def attribution = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.campaign.adgroup.adgroup_attribution(
        new GoogleAdGroupPerformanceChart(
          getMetaData(
            request, 
            List(GoogleAdGroupPerformance.adGroupHtmlField,
              GoogleAdGroupPerformance.adGroupStateField,
              GooglePerformance.ctrField,
              GooglePerformance.cpcField,
              GooglePerformance.avgPosField,
              GooglePerformance.costPerConvField,
              GooglePerformance.cRateField,
              GooglePerformance.viewThroughConvField
            ),
            request.getQueryString("filterById") match {
              case Some(id) => 
                List(new PerformanceEntityFilter(GoogleCampaignPerformance.campaignIdField, "eq", List(id.toLong)))
              case _ => List()
            },
            defaultGoogleMetaData
          )
        )
      )))
  }
  
  def attributionCSV = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future.successful(Ok.sendFile(
        new GoogleAdGroupPerformanceChart(
          getMetaData(
            request, 
            List(GoogleAdGroupPerformance.adGroupHtmlField),
            request.getQueryString("filterById") match {
              case Some(id) => 
                List(new PerformanceEntityFilter(GoogleCampaignPerformance.campaignIdField, "eq", List(id.toLong)))
              case _ => List()
            },
            defaultGoogleMetaData
          )
        ).exportCsv("AdGroupAttribution.csv")
      ))
  }
  def adGroups = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.campaign.adgroup.ad_groups(
        new ClientChart(
          List(
            new ChartColumn("id", "", "Id", number, dimension),
            new ChartColumn("name", "", "Name", string, dimension),
            new ChartColumn("status", "", "Status", string, dimension),
            new ActionColumn((rowValues: List[Any]) => "/google/mcc/account/campaign/adgroup/%s/".format(rowValues(0).toString))
          ),
          googleAdGroupCollection.find().toList.map(
            dboToGoogleEntity[AdGroup](_, "adGroup", None)
          )
        ),
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.GOOGLE
                && x.changeCategory == ChangeCategory.AD_GROUP
          )
      )))
  }

  def newAdGroup = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.campaign.adgroup.new_ad_group(
        adGroupForm,
        googleCampaignCollection.find().toList.map(dboToGoogleEntity[Campaign](_, "campaign", None)),
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.GOOGLE
                && x.changeCategory == ChangeCategory.CAMPAIGN
                && x.changeType == ChangeType.NEW
          )
          .map(x => dboToCampaignForm(x.changeData.asDBObject)),
        List()
      )))
  }

  def createAdGroup = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val current_cache = Await.result(Shared.Shared.redisClient.lrange[PendingCacheStructure](pendingCacheKey(Left(request)), 0, -1), 5 seconds).toList
      adGroupForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.google.mcc.account.campaign.adgroup.new_ad_group(
              formWithErrors,
              googleCampaignCollection.find().toList.map(dboToGoogleEntity[Campaign](_, "campaign", None)),
              pendingCache(Left(request))
                .filter(x =>
                  x.trafficSource == TrafficSource.GOOGLE
                    && x.changeCategory == ChangeCategory.CAMPAIGN
                    && x.changeType == ChangeType.NEW
                )
                .map(x => dboToCampaignForm(x.changeData.asDBObject)),
              List()
            )
          ))
        },
        ad_group => {
          Shared.Shared.redisClient.lpush(
            pendingCacheKey(Left(request)),
            (current_cache :+ PendingCacheStructure(
              id = current_cache.length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.AD_GROUP,
              changeData = adGroupFormToDbo(ad_group)
            )): _*
          )
          Future(Redirect(controllers.google.mcc.account.campaign.adgroup.routes.AdGroupController.adGroups()))
        }
      )
  }


  def bulkNewAdGroup = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[AdGroup]
          val ad_group_data_list = Utilities.bulkImport(bulk, field_names)
          for (((ad_group_data, action), index) <- ad_group_data_list.zipWithIndex) {
            adGroupForm.bind(ad_group_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              ad_group => {
                setPendingCache(
                  Left(request),
                  pendingCache(Left(request)) :+ PendingCacheStructure(
                    id = pendingCache(Left(request)).length + 1,
                    changeType = ChangeType.withName(action),
                    trafficSource = TrafficSource.GOOGLE,
                    changeCategory = ChangeCategory.AD_GROUP,
                    changeData = adGroupFormToDbo(ad_group)
                  )
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.google.mcc.account.campaign.adgroup.new_ad_group(
          adGroupForm,
          googleCampaignCollection.find().toList.map(dboToGoogleEntity[Campaign](_, "campaign", None)),
          pendingCache(Left(request))
            .filter(x =>
              x.trafficSource == TrafficSource.GOOGLE
                && x.changeCategory == ChangeCategory.AD_GROUP
                && x.changeType == ChangeType.NEW
            )
            .map(x => dboToCampaignForm(x.changeData.asDBObject)),
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.google.mcc.account.campaign.adgroup.routes.AdGroupController.adGroups()))
      }
    }
  }

  def editAdGroup(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      googleAdGroupCollection.findOne(DBObject("apiId" -> api_id)) match {
        case None => Future(Redirect(controllers.routes.DashboardController.dashboard()))
        case Some(ad_group_obj) =>
          val campaigns = googleCampaignCollection.find().toList
          def ad_group = dboToGoogleEntity[AdGroup](ad_group_obj, "adGroup", None)
          Future(Ok(views.html.google.mcc.account.campaign.adgroup.edit_ad_group(
            ad_group.getId,
            adGroupForm.fill(adGroupDboToAdGroupForm(ad_group_obj)),
            campaigns.map(dboToGoogleEntity[Campaign](_, "campaign", None)),
            pendingCache(Left(request)).filter(x =>
              x.trafficSource == TrafficSource.GOOGLE
                && x.changeCategory == ChangeCategory.CAMPAIGN
                && x.changeType == ChangeType.NEW
            ).map(x => dboToCampaignForm(x.changeData.asDBObject))
          )))
      }
  }
  
  def adGroupDboToAdGroupForm(adGroupObj: DBObject): AdGroupForm = {
    val adGroup = dboToGoogleEntity[AdGroup](adGroupObj, "adGroup", None)
    AdGroupForm(
      AdGroupParent(
        mccObjId = adGroupObj.getAsOrElse[Option[String]]("mccObjId", None),
        customerApiId = adGroupObj.getAsOrElse[Option[Long]]("customerApiId", None),
        campaignApiId = adGroupObj.getAsOrElse[Option[Long]]("campaignApiId", None)
      ),
      apiId = Some(adGroup.getId),
      name = adGroup.getName,
      status = adGroup.getStatus.toString,
      maxCpc = Option(microToDollars(adGroup.getBiddingStrategyConfiguration.getBids().filter { 
        bid => bid.getBidsType().equalsIgnoreCase(AdGroupField.CpcBid.toString)
      }.head.asInstanceOf[CpcBid].getBid.getMicroAmount)),
      contentBidCriterionTypeGroup = adGroup.getContentBidCriterionTypeGroup match {
        case typeGroup if (typeGroup != null) => Some(typeGroup.toString)
        case _ => None
      }
    )
  }

  def saveAdGroup(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      adGroupForm.bindFromRequest.fold(formWithErrors => {
        Future(BadRequest(
          views.html.google.mcc.account.campaign.adgroup.edit_ad_group(
            api_id,
            formWithErrors,
            googleCampaignCollection.find().toList.map(dboToGoogleEntity[Campaign](_, "campaign", None)),
            pendingCache(Left(request))
              .filter(x =>
                x.trafficSource == TrafficSource.GOOGLE
                  && x.changeCategory == ChangeCategory.CAMPAIGN
                  && x.changeType == ChangeType.NEW
              )
              .map(x => dboToCampaignForm(x.changeData.asDBObject))
          )
        ))
      },
        ad_group => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.AD_GROUP,
              changeData = adGroupFormToDbo(ad_group)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.adgroup.routes.AdGroupController.adGroups()))
        }
      )
  }

  def deleteAdGroup(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
          id = pendingCache(Left(request)).length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.GOOGLE,
          changeCategory = ChangeCategory.AD_GROUP,
          changeData = DBObject("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.google.mcc.account.campaign.adgroup.routes.AdGroupController.adGroups()))
  }
  
  def maxCpc(adGroupId: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request => 
      request.getQueryString("dollarAmt") match {
        case Some(dollarAmtStr) =>
          val dollarAmt = dollarAmtStr.toDouble
          googleAdGroupCollection.findOne(DBObject("adGroup.object.id" -> adGroupId)) match {
            case Some(adGroupObj) =>
              val adGroupForm = adGroupDboToAdGroupForm(adGroupObj)
              adGroupForm.maxCpc = Some(dollarAmt)
              setPendingCache(
                Left(request),
                pendingCache(Left(request)) :+ PendingCacheStructure(
                  id = pendingCache(Left(request)).length + 1,
                  changeType = ChangeType.UPDATE,
                  trafficSource = TrafficSource.GOOGLE,
                  changeCategory = ChangeCategory.AD_GROUP,
                  changeData = adGroupFormToDbo(adGroupForm)
                )
              )
              
              Future(Ok(s"Successfully created update task for AdGroup $adGroupId"))
              
            case _ => Future(BadRequest("No AdGroup found for specified Id"))
          }
          
        case _ => Future(BadRequest("dollarAmt is a required field"))
      }
  }
}
