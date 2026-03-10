package controllers.google.mcc.account.campaign.criterion

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import helpers.google.mcc.account.campaign.CampaignControllerHelper._
import helpers.google.mcc.account.campaign.criterion.CampaignAdScheduleControllerHelper._
import models.mongodb._
import models.mongodb.MongoExtensions._
import models.mongodb.google.Google._
import play.api.i18n.I18nSupport
import play.api.mvc._
import security.HandlerKeys

import scala.collection.immutable._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class CampaignAdScheduleController @Inject()(val controllerComponents: ControllerComponents, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {

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
          "tsecs"
        ),
        "criterion",
        googleCriterionCollection.namespace.getCollectionName,
        Some("criterion.criterionType" -> "AdSchedule")
      )))
  }

  def campaignAdSchedule(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[CampaignCriterion] with Document-based access
      val criterionDocs = googleCriterionCollection.find(
        Document("criterionType" -> "CampaignAdSchedule")
      ).skip(page * pageSize).limit(pageSize).toList
      Future(Ok(views.html.google.mcc.account.campaign.criterion.ad_schedule.campaign_ad_schedule(
        criterionDocs,
        page,
        pageSize,
        orderBy,
        filter,
        googleCriterionCollection.count(Document("criterionType" -> "CampaignAdSchedule")).toInt,
        pendingCache(Left(request))
          .filter(x =>
            x.trafficSource == TrafficSource.GOOGLE
              && x.changeCategory == ChangeCategory.CAMPAIGN_AD_SCHEDULE
          )
      )))
  }

  def newCampaignAdSchedule = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Campaign] with Document-based access
      val campaignDocs = googleCampaignCollection.find().toList
      Future(Ok(views.html.google.mcc.account.campaign.criterion.ad_schedule.new_campaign_ad_schedule(
        campaignAdScheduleForm,
        campaignDocs,
        pendingCache(Left(request))
          .filter(x =>
            x.changeType == ChangeType.NEW
              && x.trafficSource == TrafficSource.GOOGLE
              && x.changeCategory == ChangeCategory.CAMPAIGN
          )
          .map(x => documentToCampaignForm(x.changeData)),
        List()
      )))
  }

  def createCampaignAdSchedule = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignAdScheduleForm.bindFromRequest.fold(
        formWithErrors => {
          // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Campaign] with Document-based access
          val campaignDocs = googleCampaignCollection.find().toList
          Future(BadRequest(views.html.google.mcc.account.campaign.criterion.ad_schedule.new_campaign_ad_schedule(
            formWithErrors,
            campaignDocs,
            pendingCache(Left(request))
              .filter(x =>
                x.changeType == ChangeType.NEW
                  && x.trafficSource == TrafficSource.GOOGLE
                  && x.changeCategory == ChangeCategory.CAMPAIGN
              )
              .map(x => documentToCampaignForm(x.changeData)),
            List()
          )))
        },
        campaign_ad_schedule => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.CAMPAIGN_AD_SCHEDULE,
              changeData = campaignAdScheduleFormToDocument(campaign_ad_schedule)
            )
          )
          Future(Redirect(
            controllers.google.mcc.account.campaign.criterion.routes.CampaignAdScheduleController.campaignAdSchedule()
          ))
        }
      )
  }

  def bulkNewCampaignAdSchedule = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request =>
      var error_list = new ListBuffer[String]()
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[CampaignAdScheduleForm]
          val ad_schedule_data_list = Utilities.bulkImport(bulk, field_names)
          for (((ad_schedule_data, action), index) <- ad_schedule_data_list.zipWithIndex) {
            campaignAdScheduleForm.bind(ad_schedule_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              campaign_ad_schedule =>
                setPendingCache(
                  Left(request),
                  pendingCache(Left(request)) :+ PendingCacheStructure(
                    id = pendingCache(Left(request)).length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.GOOGLE,
                    changeCategory = ChangeCategory.CAMPAIGN_KEYWORD,
                    changeData = campaignAdScheduleFormToDocument(campaign_ad_schedule)
                  )
                )
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Campaign] with Document-based access
        val campaignDocs = googleCampaignCollection.find().toList
        Future(BadRequest(views.html.google.mcc.account.campaign.criterion.ad_schedule.new_campaign_ad_schedule(
          campaignAdScheduleForm,
          campaignDocs,
          pendingCache(Left(request))
            .filter(x =>
              x.changeType == ChangeType.NEW
                && x.trafficSource == TrafficSource.GOOGLE
                && x.changeCategory == ChangeCategory.CAMPAIGN
            )
            .map(x => documentToCampaignForm(x.changeData)),
          error_list.toList
        )))
      } else {
        Future(Redirect(
          controllers.google.mcc.account.campaign.criterion.routes.CampaignAdScheduleController.campaignAdSchedule()
        ))
      }
  }

  def editCampaignAdSchedule(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      googleCriterionCollection.findOne(Document("criterionApiId" -> api_id)) match {
        case None => Future(Redirect(controllers.routes.DashboardController.dashboard))
        case Some(campaign_criterion_obj) =>
          // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[CampaignCriterion]/AdSchedule with Document-based access
          val campaigns = googleCampaignCollection.find().toList
          val criterionDoc = Option(campaign_criterion_obj.toBsonDocument.get("criterion")).map(v => Document(v.asDocument())).flatMap(d => Option(d.toBsonDocument.get("object")).map(v => Document(v.asDocument())))
          val innerCriterionDoc = criterionDoc.flatMap(d => Option(d.toBsonDocument.get("criterion")).map(v => Document(v.asDocument())))
          Future(Ok(views.html.google.mcc.account.campaign.criterion.ad_schedule.edit_campaign_ad_schedule(
            api_id,
            campaignAdScheduleForm.fill(
              CampaignAdScheduleForm(
                parent = controllers.Google.CampaignCriterionParent(
                  mccObjId = Option(campaign_criterion_obj.getString("mccObjId")),
                  customerApiId = Option(campaign_criterion_obj.getLong("customerApiId")).map(_.toLong),
                  campaignApiId = Option(campaign_criterion_obj.getLong("campaignApiId")).map(_.toLong)
                ),
                apiId = innerCriterionDoc.flatMap(d => Option(d.getLong("id")).map(_.toLong)),
                isNegative = criterionDoc.flatMap(d => Option(d.getBoolean("isNegative")).map(_.booleanValue())),
                dayOfWeek = innerCriterionDoc.flatMap(d => Option(d.getString("dayOfWeek"))).getOrElse("MONDAY"),
                startHour = innerCriterionDoc.flatMap(d => Option(d.getInteger("startHour")).map(_.toInt)).getOrElse(0),
                startMinute = innerCriterionDoc.flatMap(d => Option(d.getString("startMinute"))).getOrElse("ZERO"),
                endHour = innerCriterionDoc.flatMap(d => Option(d.getInteger("endHour")).map(_.toInt)).getOrElse(0),
                endMinute = innerCriterionDoc.flatMap(d => Option(d.getString("endMinute"))).getOrElse("ZERO"),
                bidModifier = criterionDoc.flatMap(d => Option(d.getDouble("bidModifier")).map(_.toDouble)).getOrElse(1.0)
              )),
            campaigns,
            pendingCache(Left(request))
              .filter(x =>
                x.changeType == ChangeType.NEW
                  && x.trafficSource == TrafficSource.GOOGLE
                  && x.changeCategory == ChangeCategory.CAMPAIGN
              )
              .map(x => documentToCampaignForm(x.changeData))
          )))
      }
  }

  def saveCampaignAdSchedule(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignAdScheduleForm.bindFromRequest.fold(
        formWithErrors => {
          // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Campaign] with Document-based access
          val campaignDocs = googleCampaignCollection.find().toList
          Future(BadRequest(views.html.google.mcc.account.campaign.criterion.ad_schedule.edit_campaign_ad_schedule(
            api_id,
            formWithErrors,
            campaignDocs,
            // TODO: Migrate to RedisService injection - redisClient.lrange replaced with session cache
            pendingCache(Left(request))
              .filter(x =>
                x.changeType == ChangeType.NEW
                  && x.trafficSource == TrafficSource.GOOGLE
                  && x.changeCategory == ChangeCategory.CAMPAIGN
              )
              .map(x => documentToCampaignForm(x.changeData))
          )))
        },
        campaign_ad_schedule => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.CAMPAIGN_AD_SCHEDULE,
              changeData = campaignAdScheduleFormToDocument(campaign_ad_schedule)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.criterion.routes.CampaignAdScheduleController.campaignAdSchedule()))
        }
      )
  }


  def deleteCampaignAdSchedule(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
          id = pendingCache(Left(request)).length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.GOOGLE,
          changeCategory = ChangeCategory.CAMPAIGN_AD_SCHEDULE,
          changeData = Document("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.google.mcc.account.campaign.criterion.routes.CampaignAdScheduleController.campaignAdSchedule()))
  }
}
