package controllers.google.mcc.account.campaign

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.google.api.ads.adwords.axis.v201609.cm.{Budget, Campaign}
import com.mongodb.casbah.Imports._
import helpers.google.mcc.account.BudgetControllerHelper._
import helpers.google.mcc.account.campaign.CampaignControllerHelper._
import models.mongodb._
import models.mongodb.google.Google._
import models.mongodb.google.GoogleCampaignPerformance
import play.api.cache.CacheApi
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import security.HandlerKeys
import util.charts.ChartMetaData._
import util.charts._
import util.charts.client.{ActionColumn, ChartColumn}
import util.charts.client.ChartColumn.ColumnDataType._
import util.charts.client.ChartColumn.ColumnType._
import util.charts.performance.GooglePerformanceCharts._

import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import models.mongodb.google.GooglePerformance
import models.mongodb.performance.PerformanceEntityFilter

class CampaignController @Inject()(
  val messagesApi: MessagesApi,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders,
  cache: CacheApi
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
          "campaignApiId"
        ),
        "campaign",
        googleCampaignCollection
      )))
  }

  def attribution = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.campaign.campaign_attribution(
        new GoogleCampaignPerformanceChart(
          getMetaData(
            request, 
            List(GoogleCampaignPerformance.campaignHtmlField, 
                GoogleCampaignPerformance.campaignBudgetEditHtmlField, 
                GoogleCampaignPerformance.campaignStateField,
                GooglePerformance.impField,
                GooglePerformance.ctrField,
                GooglePerformance.cpcField,
                GooglePerformance.cpmField,
                GooglePerformance.costPerConvField),
            request.getQueryString("filterById") match {
              case Some(account) => 
                List(new PerformanceEntityFilter(GooglePerformance.accountField, "eq", List(account)))
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
        new GoogleCampaignPerformanceChart(
          getMetaData(
            request, 
            List(GoogleCampaignPerformance.campaignHtmlField, GoogleCampaignPerformance.campaignBudgetEditHtmlField),
            request.getQueryString("filterById") match {
              case Some(account) => 
                List(new PerformanceEntityFilter(GooglePerformance.accountField, "eq", List(account)))
              case _ => List()
            },
            defaultGoogleMetaData
          )
        ).exportCsv("CampaignAttribution.csv")
      ))
  }
  
  def campaigns(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.campaign.campaigns(
        new ClientChart(
          List(
            new ChartColumn("id", "", "Id", number, dimension),
            new ChartColumn("name", "", "Name", string, dimension),
            new ChartColumn("status", "", "Status", string, dimension),
            new ActionColumn((rowValues: List[Any]) => "/google/mcc/account/campaign/%s/".format(rowValues.head.toString))
          ),
          googleCampaignCollection.find().skip(page * pageSize).limit(pageSize).toList.map(dboToGoogleEntity[Campaign](_, "campaign", None))
        ),
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.GOOGLE && x.changeCategory == ChangeCategory.CAMPAIGN
          )
      )))
  }

  def newCampaign = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      Future(Ok(views.html.google.mcc.account.campaign.new_campaign(
        campaignForm,
        googleBudgetCollection.find().toList.map(dboToGoogleEntity[Budget](_, "budget", None)),
        Await.result(Shared.Shared.redisClient.lrange[PendingCacheStructure](pending_cache_key, 0, -1), 5 seconds).toList
          .filter(x => x.changeType == ChangeType.NEW && x.trafficSource == TrafficSource.GOOGLE && x.changeCategory == ChangeCategory.BUDGET)
          .map(x => dboToBudgetForm(x.changeData.asDBObject)),
        List()
      )))
  }

  def createCampaign = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(views.html.google.mcc.account.campaign.new_campaign(
            formWithErrors,
            googleBudgetCollection.find().toList.map(dboToGoogleEntity[Budget](_, "budget", None)),
            pendingCache(Left(request))
              .filter(x => x.changeType == ChangeType.NEW && x.trafficSource == TrafficSource.GOOGLE && x.changeCategory == ChangeCategory.BUDGET)
              .map(x => dboToBudgetForm(x.changeData.asDBObject)),
            List()
          )))
        },
        campaign => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.CAMPAIGN,
              changeData = campaignFormToDbo(campaign)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.routes.CampaignController.campaigns()))
        }
      )
  }

  def bulkNewCampaign = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request =>
      var error_list = new ListBuffer[String]()
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[CampaignForm]
          val campaign_data_list = Utilities.bulkImport(bulk, field_names)
          for (((campaign_data, action), index) <- campaign_data_list.zipWithIndex) {
            campaignForm.bind(campaign_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              campaign =>
                setPendingCache(
                  Left(request),
                  pendingCache(Left(request)) :+ PendingCacheStructure(
                    id = pendingCache(Left(request)).length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.GOOGLE,
                    changeCategory = ChangeCategory.CAMPAIGN,
                    changeData = campaignFormToDbo(campaign)
                  )
                )
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.google.mcc.account.campaign.new_campaign(
          campaignForm,
          googleCustomerCollection.find().toList.map(dboToGoogleEntity[Budget](_, "budget", None)),
          pendingCache(Left(request))
            .filter(x => x.changeType == ChangeType.NEW && x.trafficSource == TrafficSource.GOOGLE && x.changeCategory == ChangeCategory.BUDGET)
            .map(x => dboToBudgetForm(x.changeData.asDBObject)),
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.google.mcc.account.campaign.routes.CampaignController.campaigns()))
      }
  }

  def editCampaign(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      googleCampaignCollection.findOne(DBObject("apiId" -> api_id)) match {
        case None => Future(Redirect(controllers.routes.DashboardController.dashboard()))
        case Some(campaign_obj) =>
          val campaign = dboToGoogleEntity[Campaign](campaign_obj, "campaign", None)
          val budgets = googleBudgetCollection.find(DBObject("customerApiId" -> campaign_obj.getAsOrElse[Option[Long]]("customerApiId", None))).toList
          
          Future(Ok(views.html.google.mcc.account.campaign.edit_campaign(
            campaign.getId,
            campaignForm.fill(
              CampaignForm(
                CampaignParent(
                  mccObjId = campaign_obj.getAsOrElse[Option[String]]("mccObjId", None),
                  customerApiId = campaign_obj.getAsOrElse[Option[Long]]("customerApiId", None)
                ),
                apiId = Some(campaign.getId),
                name = campaign.getName,
                status = Some(campaign.getStatus.toString),
                servingStatus = Some(campaign.getServingStatus.toString),
                startDate = Some(campaign.getStartDate),
                endDate = Some(campaign.getEndDate),
                budgetAmount = Some(microToDollars(campaign.getBudget.getAmount.getMicroAmount).toInt),
                isSharedBudget = Some(campaign.getBudget.getIsExplicitlyShared),
                advertisingChannelType = Some(campaign.getAdvertisingChannelType.toString),
                adServingOptimizationStatus = Some(campaign.getAdServingOptimizationStatus.toString),
                frequencyCapImpressions = try {
                  Option(campaign.getFrequencyCap.getImpressions)
                } catch {
                  case _ => None
                },
                frequencyCapTimeUnit = try {
                  Option(campaign.getFrequencyCap.getTimeUnit.toString)
                } catch {
                  case _ => None
                },
                frequencyCapLevel = try {
                  Option(campaign.getFrequencyCap.getLevel.toString)
                } catch {
                  case _ => None
                },
                targetGoogleSearch = Some(campaign.getNetworkSetting.getTargetGoogleSearch),
                targetSearchNetwork = Some(campaign.getNetworkSetting.getTargetSearchNetwork),
                targetContentNetwork = Some(campaign.getNetworkSetting.getTargetContentNetwork),
                targetPartnerSearchNetwork = Some(campaign.getNetworkSetting.getTargetPartnerSearchNetwork)
              )
            ),
            budgets.map(dboToGoogleEntity[Budget](_, "budget", None))
          )))
      }
  }


  def saveCampaign(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(views.html.google.mcc.account.campaign.edit_campaign(
            api_id,
            formWithErrors,
            googleBudgetCollection.find().toList.map(dboToGoogleEntity[Budget](_, "budget", None))
          )))
        },
        campaign => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.CAMPAIGN,
              changeData = campaignFormToDbo(campaign)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.routes.CampaignController.campaigns()))
        }
      )
  }

  def deleteCampaign(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
          id = pendingCache(Left(request)).length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.GOOGLE,
          changeCategory = ChangeCategory.CAMPAIGN,
          changeData = DBObject("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.google.mcc.account.campaign.routes.CampaignController.campaigns()))
  }
}
