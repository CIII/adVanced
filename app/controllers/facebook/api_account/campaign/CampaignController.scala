package controllers.facebook.api_account.campaign

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.facebook.ads.sdk.Campaign
import com.mongodb.casbah.Imports._
import helpers.facebook.api_account.campaign.CampaignControllerHelper._
import models.mongodb.facebook.Facebook._
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import security.HandlerKeys

import scala.collection.JavaConverters._
import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

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
          "id",
          "account_id",
          "adlabels",
          "brand_lift_studies",
          "budget_rebalance_flag",
          "buying_type",
          "can_use_spend_cap",
          "configured_status",
          "created_time",
          "effective_status",
          "name",
          "objective",
          "recommendations",
          "spend_cap",
          "start_time",
          "status",
          "stop_time",
          "updated_time"
        ),
        "campaign",
        facebookCampaignCollection
      )))
  }

  def campaigns(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.FacebookRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val campaigns = facebookCampaignCollection.find().skip(page * pageSize).limit(pageSize).toList
      Future(Ok(views.html.facebook.api_account.campaign.campaigns(
        campaigns.map(dboToFacebookEntity[Campaign](_, "campaign", None)),
        page,
        pageSize,
        orderBy,
        filter,
        facebookCampaignCollection.count(),
        cache.get(pendingCacheKey(Left(request)))
          .getOrElse(List())
          .asInstanceOf[List[PendingCacheStructure]]
          .filter(x => x.trafficSource == TrafficSource.FACEBOOK && x.changeCategory == ChangeCategory.CAMPAIGN)
      )))
  }

  def newCampaign = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.facebook.api_account.campaign.new_campaign(
        campaignForm,
        List()
      )))
  }

  def editCampaign(api_id: String) = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      facebookCampaignCollection.findOne(DBObject("apiId" -> api_id)) match {
        case Some(campaignObj) =>
          def campaign = dboToFacebookEntity[Campaign](campaignObj, "campaign", None)
          Future(Ok(views.html.facebook.api_account.campaign.edit_campaign(
            api_id,
            campaignForm.fill(
              CampaignForm(
                apiId = Some(campaign.getId),
                name = campaign.getFieldName,
                objective = Some(campaign.getFieldObjective),
                accountId = campaign.getFieldAccountId,
                adLabels = Some(List()),
                budgetRebalanceFlag = campaign.getFieldBudgetRebalanceFlag,
                buyingType = campaign.getFieldBuyingType,
                canUseSpendCap = campaign.getFieldCanUseSpendCap,
                configuredStatus = campaign.getFieldConfiguredStatus.toString,
                createdTime = Some(campaign.getFieldCreatedTime),
                effectiveStatus = campaign.getFieldEffectiveStatus.toString,
                spendCap = Some(campaign.getFieldSpendCap),
                startTime = campaign.getFieldStartTime match {
                  case _ if campaign.getFieldStartTime != null =>
                    Some(DateTime.parse(campaign.getFieldStartTime))
                  case _ => None
                },
                status = Some(campaign.getFieldStatus.toString),
                stopTime = campaign.getFieldStopTime match {
                  case _ if campaign.getFieldStopTime != null =>
                    Some(DateTime.parse(campaign.getFieldStopTime))
                  case _ => None
                },
                updatedTime = campaign.getFieldUpdatedTime match {
                  case _ if campaign.getFieldUpdatedTime != null =>
                    Some(DateTime.parse(campaign.getFieldUpdatedTime))
                  case _ => None
                }
              )
            )
          )))
        case None =>
          Future(Redirect(controllers.facebook.api_account.campaign.routes.CampaignController.campaigns()))
      }
  }

  def createCampaign = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val current_cache = Await.result(Shared.Shared.redisClient.lrange[PendingCacheStructure](pendingCacheKey(Left(request)), 0, -1), 5 seconds).toList
      campaignForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.facebook.api_account.campaign.new_campaign(
              formWithErrors,
              List()
            )
          ))
        },
        campaign => {
          Shared.Shared.redisClient.lpush(
            pendingCacheKey(Left(request)),
            current_cache :+ PendingCacheStructure(
              id = current_cache.length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.FACEBOOK,
              changeCategory = ChangeCategory.CAMPAIGN,
              changeData = campaignFormToDbo(campaign)
            ): _*
          )
          Future(Redirect(controllers.facebook.api_account.campaign.routes.CampaignController.campaigns()))
        }
      )
  }

  def bulkNewCampaign = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[Campaign]
          val campaign_data_list = Utilities.bulkImport(bulk, field_names)
          for (((campaign_data, action), index) <- campaign_data_list.zipWithIndex) {
            campaignForm.bind(campaign_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              campaign => {
                setPendingCache(
                  Left(request),
                  pendingCache(Left(request)) :+ PendingCacheStructure(
                    id = pendingCache(Left(request)).length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.FACEBOOK,
                    changeCategory = ChangeCategory.CAMPAIGN,
                    changeData = campaignFormToDbo(campaign)
                  )
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.facebook.api_account.campaign.new_campaign(
          campaignForm,
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.facebook.api_account.campaign.routes.CampaignController.campaigns()))
      }
    }
  }


  def saveCampaign(api_id: String) = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.facebook.api_account.campaign.edit_campaign(
              api_id,
              formWithErrors
            )
          ))
        },
        campaign => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.FACEBOOK,
              changeCategory = ChangeCategory.CAMPAIGN,
              changeData = campaignFormToDbo(campaign)
            )
          )
          Future(Redirect(controllers.facebook.api_account.campaign.routes.CampaignController.campaigns()))
        }
      )
  }


  def deleteCampaign(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
          id = pendingCache(Left(request)).length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.FACEBOOK,
          changeCategory = ChangeCategory.CAMPAIGN,
          changeData = DBObject("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.facebook.api_account.campaign.routes.CampaignController.campaigns()))
  }
}
