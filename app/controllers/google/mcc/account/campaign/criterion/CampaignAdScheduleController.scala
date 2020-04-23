package controllers.google.mcc.account.campaign.criterion

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.google.api.ads.adwords.axis.v201609.cm.{AdSchedule, Campaign, CampaignCriterion}
import com.mongodb.casbah.Imports._
import helpers.google.mcc.account.campaign.CampaignControllerHelper._
import helpers.google.mcc.account.campaign.criterion.CampaignAdScheduleControllerHelper._
import models.mongodb._
import models.mongodb.google.Google._
import play.api.Play.current
import play.api.cache._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import security.HandlerKeys

import scala.collection.immutable._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class CampaignAdScheduleController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {

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
        googleCriterionCollection,
        Some("criterion.criterionType" -> "AdSchedule")
      )))
  }

  def campaignAdSchedule(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.campaign.criterion.ad_schedule.campaign_ad_schedule(
        googleCriterionCollection.find(
          DBObject("criterionType" -> "CampaignAdSchedule")
        ).skip(page * pageSize).limit(pageSize).toList.map(dboToGoogleEntity[CampaignCriterion](_, "criterion", None)),
        page,
        pageSize,
        orderBy,
        filter,
        googleCriterionCollection.count(DBObject("criterionType" -> "CampaignAdSchedule")),
        pendingCache(Left(request))
          .filter(x =>
            x.trafficSource == TrafficSource.GOOGLE
              && x.changeCategory == ChangeCategory.CAMPAIGN_AD_SCHEDULE
          )
      )))
  }

  def newCampaignAdSchedule = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.campaign.criterion.ad_schedule.new_campaign_ad_schedule(
        campaignAdScheduleForm,
        googleCampaignCollection.find().toList.map(dboToGoogleEntity[Campaign](_, "campaign", None)),
        pendingCache(Left(request))
          .filter(x =>
            x.changeType == ChangeType.NEW
              && x.trafficSource == TrafficSource.GOOGLE
              && x.changeCategory == ChangeCategory.CAMPAIGN
          )
          .map(x => dboToCampaignForm(x.changeData.asDBObject)),
        List()
      )))
  }

  def createCampaignAdSchedule = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignAdScheduleForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(views.html.google.mcc.account.campaign.criterion.ad_schedule.new_campaign_ad_schedule(
            formWithErrors,
            googleCampaignCollection.find().toList.map(dboToGoogleEntity[Campaign](_, "campaign", None)),
            pendingCache(Left(request))
              .filter(x =>
                x.changeType == ChangeType.NEW
                  && x.trafficSource == TrafficSource.GOOGLE
                  && x.changeCategory == ChangeCategory.CAMPAIGN
              )
              .map(x => dboToCampaignForm(x.changeData.asDBObject)),
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
              changeData = campaignAdScheduleFormToDbo(campaign_ad_schedule)
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
                    changeData = campaignAdScheduleFormToDbo(campaign_ad_schedule)
                  )
                )
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.google.mcc.account.campaign.criterion.ad_schedule.new_campaign_ad_schedule(
          campaignAdScheduleForm,
          googleCampaignCollection.find().toList.map(dboToGoogleEntity[Campaign](_, "campaign", None)),
          pendingCache(Left(request))
            .filter(x =>
              x.changeType == ChangeType.NEW
                && x.trafficSource == TrafficSource.GOOGLE
                && x.changeCategory == ChangeCategory.CAMPAIGN
            )
            .map(x => dboToCampaignForm(x.changeData.asDBObject)),
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
      googleCriterionCollection.findOne(DBObject("criterionApiId" -> api_id)) match {
        case None => Future(Redirect(controllers.routes.DashboardController.dashboard()))
        case Some(campaign_criterion_obj) =>
          val campaigns = googleCampaignCollection.find().toList
          val campaign_ad_schedule = dboToGoogleEntity[CampaignCriterion](campaign_criterion_obj, "criterion", None)
          Future(Ok(views.html.google.mcc.account.campaign.criterion.ad_schedule.edit_campaign_ad_schedule(
            api_id,
            campaignAdScheduleForm.fill(
              CampaignAdScheduleForm(
                parent = controllers.Google.CampaignCriterionParent(
                  mccObjId = campaign_criterion_obj.getAsOrElse[Option[String]]("mccObjId", None),
                  customerApiId = campaign_criterion_obj.getAsOrElse[Option[Long]]("customerApiId", None),
                  campaignApiId = campaign_criterion_obj.getAsOrElse[Option[Long]]("campaignApiId", None)
                ),
                apiId = Some(campaign_ad_schedule.getCriterion.getId),
                isNegative = Some(campaign_ad_schedule.getIsNegative),
                dayOfWeek = campaign_ad_schedule.getCriterion.asInstanceOf[AdSchedule].getDayOfWeek.toString,
                startHour = campaign_ad_schedule.getCriterion.asInstanceOf[AdSchedule].getStartHour,
                startMinute = campaign_ad_schedule.getCriterion.asInstanceOf[AdSchedule].getStartMinute.toString,
                endHour = campaign_ad_schedule.getCriterion.asInstanceOf[AdSchedule].getEndHour,
                endMinute = campaign_ad_schedule.getCriterion.asInstanceOf[AdSchedule].getEndMinute.toString,
                bidModifier = campaign_ad_schedule.getBidModifier
              )),
            campaigns.map(dboToGoogleEntity[Campaign](_, "campaign", None)),
            pendingCache(Left(request))
              .filter(x =>
                x.changeType == ChangeType.NEW
                  && x.trafficSource == TrafficSource.GOOGLE
                  && x.changeCategory == ChangeCategory.CAMPAIGN
              )
              .map(x => dboToCampaignForm(x.changeData.asDBObject))
          )))
      }
  }

  def saveCampaignAdSchedule(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignAdScheduleForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(views.html.google.mcc.account.campaign.criterion.ad_schedule.edit_campaign_ad_schedule(
          api_id,
          formWithErrors,
          googleCampaignCollection.find().toList.map(dboToGoogleEntity[Campaign](_, "campaign", None)),
          Await.result(Shared.Shared.redisClient.lrange[PendingCacheStructure](pendingCacheKey(Left(request)), 0, -1), 5 seconds).toList
            .filter(x =>
              x.changeType == ChangeType.NEW
                && x.trafficSource == TrafficSource.GOOGLE
                && x.changeCategory == ChangeCategory.CAMPAIGN
            )
            .map(x => dboToCampaignForm(x.changeData.asDBObject))
        ))),
        campaign_ad_schedule => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.CAMPAIGN_AD_SCHEDULE,
              changeData = campaignAdScheduleFormToDbo(campaign_ad_schedule)
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
          changeData = DBObject("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.google.mcc.account.campaign.criterion.routes.CampaignAdScheduleController.campaignAdSchedule()))
  }
}
