package controllers.facebook.api_account.campaign

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import helpers.facebook.api_account.campaign.CampaignControllerHelper._
import models.mongodb.MongoExtensions._
import models.mongodb.PermissionGroup
import models.mongodb.Utilities
import models.mongodb.facebook.Facebook._
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.i18n.I18nSupport
import play.api.mvc._
import security.HandlerKeys

import scala.jdk.CollectionConverters._
import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

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
        facebookCampaignCollection.namespace.getCollectionName
      )))
  }

  def campaigns(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.FacebookRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val campaigns = facebookCampaignCollection.find().skip(page * pageSize).limit(pageSize).toList
      Future(Ok(views.html.facebook.api_account.campaign.campaigns(
        campaigns.map(documentToFacebookEntity(_, "campaign", None)),
        page,
        pageSize,
        orderBy,
        filter,
        facebookCampaignCollection.countSync().toInt,
        pendingCache(Left(request))
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
      facebookCampaignCollection.findOne(Document("apiId" -> api_id)) match {
        case Some(campaignObj) =>
          val campaign = documentToFacebookEntity(campaignObj, "campaign", None)
          Future(Ok(views.html.facebook.api_account.campaign.edit_campaign(
            api_id,
            campaignForm.fill(
              CampaignForm(
                apiId = Option(campaign.getString("id")),
                name = Option(campaign.getString("name")).getOrElse(""),
                objective = Option(campaign.getString("objective")),
                accountId = Option(campaign.getString("account_id")).getOrElse(""),
                adLabels = Some(List()),
                budgetRebalanceFlag = Option(campaign.getBoolean("budget_rebalance_flag")).exists(_.booleanValue()),
                buyingType = Option(campaign.getString("buying_type")).getOrElse(""),
                canUseSpendCap = Option(campaign.getBoolean("can_use_spend_cap")).exists(_.booleanValue()),
                configuredStatus = Option(campaign.getString("configured_status")).getOrElse(""),
                createdTime = Option(campaign.getString("created_time")),
                effectiveStatus = Option(campaign.getString("effective_status")).getOrElse(""),
                spendCap = Option(campaign.getLong("spend_cap")).map(_.toLong),
                startTime = Option(campaign.getString("start_time")).flatMap {
                  case s if s != null && s.nonEmpty => Some(DateTime.parse(s))
                  case _ => None
                },
                status = Option(campaign.getString("status")),
                stopTime = Option(campaign.getString("stop_time")).flatMap {
                  case s if s != null && s.nonEmpty => Some(DateTime.parse(s))
                  case _ => None
                },
                updatedTime = Option(campaign.getString("updated_time")).flatMap {
                  case s if s != null && s.nonEmpty => Some(DateTime.parse(s))
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
          // TODO: Migrate to RedisService injection - redisClient.lpush removed
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.FACEBOOK,
              changeCategory = ChangeCategory.CAMPAIGN,
              changeData = campaignFormToDocument(campaign)
            )
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
          val field_names = Utilities.getCaseClassParameter[CampaignForm]
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
                    changeData = campaignFormToDocument(campaign)
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
              changeData = campaignFormToDocument(campaign)
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
          changeData = Document("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.facebook.api_account.campaign.routes.CampaignController.campaigns()))
  }
}
