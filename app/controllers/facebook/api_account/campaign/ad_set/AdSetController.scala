package controllers.facebook.api_account.campaign.ad_set

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import be.objectify.deadbolt.scala.cache.HandlerCache
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import helpers.facebook.api_account.campaign.ad_set.AdSetControllerHelper._
import models.mongodb.MongoExtensions._
import models.mongodb.{PermissionGroup, Utilities}
import models.mongodb.facebook.Facebook._
import org.bson.types.ObjectId
import org.joda.time.DateTime

import scala.jdk.CollectionConverters._
import play.api.i18n.I18nSupport

import play.api.mvc._
import security.HandlerKeys

import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class AdSetController @Inject()(
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
          "adset_schedule",
          "attribution_window_days",
          "bid_amount",
          "bid_info",
          "billing_event",
          "budget_remaining",
          "campaign_id",
          "configured_status",
          "created_time",
          "creative_sequence",
          "daily_budget",
          "effective_status",
          "end_time",
          "frequency_cap",
          "frequency_cap_reset_period",
          "frequency_control_specs",
          "is_auto_bid"
        ),
        "campaign",
        facebookCampaignCollection.namespace.getCollectionName
      )))
  }

  def ad_sets(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.FacebookRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val adSets = facebookAdSetCollection.find().skip(page * pageSize).limit(pageSize).toList
      Future(Ok(views.html.facebook.api_account.campaign.ad_set.ad_sets(
        adSets.map(documentToFacebookEntity(_, "adSet", None)),
        page,
        pageSize,
        orderBy,
        filter,
        facebookAdSetCollection.countSync().toInt,
        pendingCache(Left(request))
          .filter(x => x.trafficSource == TrafficSource.FACEBOOK && x.changeCategory == ChangeCategory.CAMPAIGN)
      )))
  }

  def newAdSet = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.facebook.api_account.campaign.ad_set.new_ad_set(
        adSetForm,
        List()
      )))
  }

  def editAdSet(api_id: String) = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      facebookAdSetCollection.findOne(Document("apiId" -> api_id)) match {
        case Some(adSetObj) =>
          val adSet = documentToFacebookEntity(adSetObj, "adSet", None)
          Future(Ok(views.html.facebook.api_account.campaign.ad_set.edit_ad_set(
            api_id,
            adSetForm.fill(
              AdSetForm(
                AdSetParent(
                  apiAccountObjId = Option(adSetObj.getString("apiAccountObjId")),
                  campaignApiId = Option(adSetObj.getString("campaignApiId"))
                ),
                apiId = Option(adSet.getString("id")),
                accountId = Option(adSet.getString("account_id")).getOrElse(""),
                adLabels = None,
                bidAmount = Option(adSet.getLong("bid_amount")).map(_.toLong).getOrElse(0L),
                billingEvent = Option(adSet.getString("billing_event")).getOrElse(""),
                budgetRemaining = Option(adSet.getLong("budget_remaining")).map(_.toLong),
                campaignId = Option(adSet.getString("campaign_id")).getOrElse(""),
                configuredStatus = Option(adSet.getString("configured_status")).getOrElse(""),
                createdTime = Option(adSet.getString("created_time")).map(DateTime.parse).getOrElse(org.joda.time.DateTime.now),
                dailyBudget = Option(adSet.getLong("daily_budget")).map(_.toLong),
                endTime = Option(adSet.getString("end_time")).flatMap {
                  case s if s != null && s.nonEmpty => Some(DateTime.parse(s))
                  case _ => None
                },
                frequencyCap = Option(adSet.getInteger("frequency_cap")).map(_.intValue()),
                frequencyCapResetPeriod = Option(adSet.getInteger("frequency_cap_reset_period")).map(_.intValue()),
                isAutoBid = Option(adSet.getBoolean("is_autobid")).exists(_.booleanValue()),
                lifetimeBudget = Option(adSet.getLong("lifetime_budget")).map(_.toLong),
                effectiveStatus = Option(adSet.getString("effective_status")),
                optimizationGoal = Option(adSet.getString("optimization_goal")),
                startTime = Option(adSet.getString("start_time")).flatMap {
                  case s if s != null && s.nonEmpty => Some(DateTime.parse(s))
                  case _ => None
                },
                name = Option(adSet.getString("name")).getOrElse(""),
                status = Option(adSet.getString("status")),
                updatedTime = Option(adSet.getString("updated_time"))
              )
            )
          )))
        case None =>
          Future(Redirect(controllers.facebook.api_account.campaign.ad_set.routes.AdSetController.ad_sets()))
      }
  }

  def createAdSet = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      adSetForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.facebook.api_account.campaign.ad_set.new_ad_set(
              formWithErrors,
              List()
            )
          ))
        },
        adSet => {
          // TODO: Migrate to RedisService injection - redisClient.lpush removed
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.FACEBOOK,
              changeCategory = ChangeCategory.AD_SET,
              changeData = adSetFormToDocument(adSet)
            )
          )
          Future(Redirect(controllers.facebook.api_account.campaign.ad_set.routes.AdSetController.ad_sets()))
        }
      )
  }

  def bulkNewAdSet = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[AdSetForm]
          val ad_set_data_list = Utilities.bulkImport(bulk, field_names)
          for (((ad_set_data, action), index) <- ad_set_data_list.zipWithIndex) {
            adSetForm.bind(ad_set_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              adSet => {
                setPendingCache(
                  Left(request),
                  pendingCache(Left(request)) :+ PendingCacheStructure(
                    id = pendingCache(Left(request)).length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.FACEBOOK,
                    changeCategory = ChangeCategory.AD_SET,
                    changeData = adSetFormToDocument(adSet)
                  )
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.facebook.api_account.campaign.ad_set.new_ad_set(
          adSetForm,
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.facebook.api_account.campaign.ad_set.routes.AdSetController.ad_sets()))
      }
    }
  }


  def saveAdSet(api_id: String) = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      adSetForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.facebook.api_account.campaign.ad_set.edit_ad_set(
              api_id,
              formWithErrors
            )
          ))
        },
        adSet => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.FACEBOOK,
              changeCategory = ChangeCategory.AD_SET,
              changeData = adSetFormToDocument(adSet)
            )
          )
          Future(Redirect(controllers.facebook.api_account.campaign.ad_set.routes.AdSetController.ad_sets()))
        }
      )
  }


  def deleteAdSet(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
          id = pendingCache(Left(request)).length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.FACEBOOK,
          changeCategory = ChangeCategory.AD_SET,
          changeData = Document("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.facebook.api_account.campaign.ad_set.routes.AdSetController.ad_sets()))
  }
}
