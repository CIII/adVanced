package controllers.google.mcc.account.campaign

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import helpers.google.mcc.account.BudgetControllerHelper._
import helpers.google.mcc.account.campaign.CampaignControllerHelper._
import models.mongodb._
import models.mongodb.MongoExtensions._
import models.mongodb.google.Google._
import models.mongodb.google.GoogleCampaignPerformance
import play.api.i18n.I18nSupport
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
import scala.concurrent.{ExecutionContext, Future}
import models.mongodb.google.GooglePerformance
import models.mongodb.performance.PerformanceEntityFilter

class CampaignController @Inject()(
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
          "campaignApiId"
        ),
        "campaign",
        googleCampaignCollection.namespace.getCollectionName
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
      // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Campaign] with Document-based access
      val campaignDocs = googleCampaignCollection.find().skip(page * pageSize).limit(pageSize).toList
      Future(Ok(views.html.google.mcc.account.campaign.campaigns(
        new ClientChart(
          List(
            new ChartColumn("id", "", "Id", number, dimension),
            new ChartColumn("name", "", "Name", string, dimension),
            new ChartColumn("status", "", "Status", string, dimension),
            new ActionColumn((rowValues: List[Any]) => "/google/mcc/account/campaign/%s/".format(rowValues.head.toString))
          ),
          campaignDocs
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
      // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Budget] with Document-based access
      val budgetDocs = googleBudgetCollection.find().toList
      Future(Ok(views.html.google.mcc.account.campaign.new_campaign(
        campaignForm,
        budgetDocs,
        // TODO: Migrate to RedisService injection - redisClient.lrange replaced with session cache
        pendingCache(Left(request))
          .filter(x => x.changeType == ChangeType.NEW && x.trafficSource == TrafficSource.GOOGLE && x.changeCategory == ChangeCategory.BUDGET)
          .map(x => documentToBudgetForm(x.changeData)),
        List()
      )))
  }

  def createCampaign = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignForm.bindFromRequest.fold(
        formWithErrors => {
          // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Budget] with Document-based access
          val budgetDocs = googleBudgetCollection.find().toList
          Future(BadRequest(views.html.google.mcc.account.campaign.new_campaign(
            formWithErrors,
            budgetDocs,
            pendingCache(Left(request))
              .filter(x => x.changeType == ChangeType.NEW && x.trafficSource == TrafficSource.GOOGLE && x.changeCategory == ChangeCategory.BUDGET)
              .map(x => documentToBudgetForm(x.changeData)),
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
              changeData = campaignFormToDocument(campaign)
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
                    changeData = campaignFormToDocument(campaign)
                  )
                )
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Budget] with Document-based access
        val budgetDocs = googleCustomerCollection.find().toList
        Future(BadRequest(views.html.google.mcc.account.campaign.new_campaign(
          campaignForm,
          budgetDocs,
          pendingCache(Left(request))
            .filter(x => x.changeType == ChangeType.NEW && x.trafficSource == TrafficSource.GOOGLE && x.changeCategory == ChangeCategory.BUDGET)
            .map(x => documentToBudgetForm(x.changeData)),
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.google.mcc.account.campaign.routes.CampaignController.campaigns()))
      }
  }

  def editCampaign(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      googleCampaignCollection.findOne(Document("apiId" -> api_id)) match {
        case None => Future(Redirect(controllers.routes.DashboardController.dashboard))
        case Some(campaign_obj) =>
          // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Campaign] with Document-based access
          val campaignDoc = Option(campaign_obj.toBsonDocument.get("campaign")).map(v => Document(v.asDocument())).flatMap(d => Option(d.toBsonDocument.get("object")).map(v => Document(v.asDocument())))
          val budgets = googleBudgetCollection.find(Document("customerApiId" -> Option(campaign_obj.getLong("customerApiId")).map(_.toLong))).toList

          val campaignId = campaignDoc.flatMap(d => Option(d.getLong("id")).map(_.toLong)).getOrElse(0L)
          Future(Ok(views.html.google.mcc.account.campaign.edit_campaign(
            campaignId,
            campaignForm.fill(
              CampaignForm(
                CampaignParent(
                  mccObjId = Option(campaign_obj.getString("mccObjId")),
                  customerApiId = Option(campaign_obj.getLong("customerApiId")).map(_.toLong)
                ),
                apiId = Some(campaignId),
                name = campaignDoc.map(_.getString("name")).getOrElse(""),
                status = campaignDoc.map(d => Option(d.getString("status")).getOrElse("ENABLED")),
                servingStatus = campaignDoc.map(d => Option(d.getString("servingStatus")).getOrElse("SERVING")),
                startDate = campaignDoc.flatMap(d => Option(d.getString("startDate"))),
                endDate = campaignDoc.flatMap(d => Option(d.getString("endDate"))),
                budgetAmount = campaignDoc.flatMap(d =>
                  Option(d.toBsonDocument.get("budget")).map(v => Document(v.asDocument())).flatMap(b => Option(b.toBsonDocument.get("amount")).map(v => Document(v.asDocument()))).flatMap(a =>
                    Option(a.getLong("microAmount")).map(m => microToDollars(m.toLong).toInt)
                  )
                ),
                isSharedBudget = campaignDoc.flatMap(d =>
                  Option(d.toBsonDocument.get("budget")).map(v => Document(v.asDocument())).flatMap(b => Option(b.getBoolean("isExplicitlyShared")).map(_.booleanValue()))
                ),
                advertisingChannelType = campaignDoc.flatMap(d => Option(d.getString("advertisingChannelType"))),
                adServingOptimizationStatus = campaignDoc.flatMap(d => Option(d.getString("adServingOptimizationStatus"))),
                frequencyCapImpressions = campaignDoc.flatMap(d =>
                  Option(d.toBsonDocument.get("frequencyCap")).map(v => Document(v.asDocument())).flatMap(fc => Option(fc.getInteger("impressions")).map(_.toInt))
                ),
                frequencyCapTimeUnit = campaignDoc.flatMap(d =>
                  Option(d.toBsonDocument.get("frequencyCap")).map(v => Document(v.asDocument())).flatMap(fc => Option(fc.getString("timeUnit")))
                ),
                frequencyCapLevel = campaignDoc.flatMap(d =>
                  Option(d.toBsonDocument.get("frequencyCap")).map(v => Document(v.asDocument())).flatMap(fc => Option(fc.getString("level")))
                ),
                targetGoogleSearch = campaignDoc.flatMap(d =>
                  Option(d.toBsonDocument.get("networkSetting")).map(v => Document(v.asDocument())).flatMap(ns => Option(ns.getBoolean("targetGoogleSearch")).map(_.booleanValue()))
                ),
                targetSearchNetwork = campaignDoc.flatMap(d =>
                  Option(d.toBsonDocument.get("networkSetting")).map(v => Document(v.asDocument())).flatMap(ns => Option(ns.getBoolean("targetSearchNetwork")).map(_.booleanValue()))
                ),
                targetContentNetwork = campaignDoc.flatMap(d =>
                  Option(d.toBsonDocument.get("networkSetting")).map(v => Document(v.asDocument())).flatMap(ns => Option(ns.getBoolean("targetContentNetwork")).map(_.booleanValue()))
                ),
                targetPartnerSearchNetwork = campaignDoc.flatMap(d =>
                  Option(d.toBsonDocument.get("networkSetting")).map(v => Document(v.asDocument())).flatMap(ns => Option(ns.getBoolean("targetPartnerSearchNetwork")).map(_.booleanValue()))
                )
              )
            ),
            budgets
          )))
      }
  }


  def saveCampaign(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignForm.bindFromRequest.fold(
        formWithErrors => {
          // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Budget] with Document-based access
          val budgetDocs = googleBudgetCollection.find().toList
          Future(BadRequest(views.html.google.mcc.account.campaign.edit_campaign(
            api_id,
            formWithErrors,
            budgetDocs
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
              changeData = campaignFormToDocument(campaign)
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
          changeData = Document("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.google.mcc.account.campaign.routes.CampaignController.campaigns()))
  }
}
