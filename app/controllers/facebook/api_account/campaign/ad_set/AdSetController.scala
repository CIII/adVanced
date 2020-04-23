package controllers.facebook.api_account.campaign.ad_set

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import be.objectify.deadbolt.scala.cache.HandlerCache
import com.facebook.ads.sdk.{AdSet, Campaign}
import com.mongodb.casbah.Imports._
import helpers.facebook.api_account.campaign.ad_set.AdSetControllerHelper._
import helpers.google.mcc.account.campaign.CampaignControllerHelper.CampaignParent
import models.mongodb.{PermissionGroup, Utilities}
import models.mongodb.facebook.Facebook._
import org.bson.types.ObjectId
import org.joda.time.DateTime

import collection.JavaConverters._
import play.api.i18n.{I18nSupport, MessagesApi}

import scala.concurrent.duration._
import play.api.mvc.BodyParsers.parse
import play.api.mvc.{Action, Controller}
import play.api.cache.{Cache, CacheApi}
import security.HandlerKeys

import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class AdSetController @Inject()(
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
        facebookCampaignCollection
      )))
  }

  def ad_sets(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.FacebookRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val adSets = facebookAdSetCollection.find().skip(page * pageSize).limit(pageSize).toList
      Future(Ok(views.html.facebook.api_account.campaign.ad_set.ad_sets(
        adSets.map(dboToFacebookEntity[AdSet](_, "adSet", None)),
        page,
        pageSize,
        orderBy,
        filter,
        facebookAdSetCollection.count(),
        cache.get(pendingCacheKey(Left(request)))
          .getOrElse(List())
          .asInstanceOf[List[PendingCacheStructure]]
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
      facebookAdSetCollection.findOne(DBObject("apiId" -> api_id)) match {
        case Some(adSetObj) =>
          def adSet = dboToFacebookEntity[AdSet](adSetObj, "adSet", None)
          Future(Ok(views.html.facebook.api_account.campaign.ad_set.edit_ad_set(
            api_id,
            adSetForm.fill(
              AdSetForm(
                AdSetParent(
                  apiAccountObjId = adSetObj.getAsOrElse[Option[String]]("apiAccountObjId", None),
                  campaignApiId = adSetObj.getAsOrElse[Option[String]]("campaignApiId", None)
                ),
                apiId = Some(adSet.getId),
                accountId = adSet.getFieldAccountId,
                adLabels = None,//adSet.getFieldAdlabels.asScala.toList.map(_.),
                campaignId = adSet.getFieldCampaignId,
                configuredStatus = adSet.getFieldConfiguredStatus.toString,
                createdTime = adSet.getFieldCreatedTime,
                dailyBudget = Some(adSet.getFieldDailyBudget),
                endTime = Some(adSet.getFieldEndTime),
                frequencyCap = Some(adSet.getFieldFrequencyCap),
                frequencyCapResetPeriod = Some(adSet.getFieldFrequencyCapResetPeriod),
                isAutoBid = adSet.getFieldIsAutobid,
                lifetimeBudget = Some(adSet.getFieldLifetimeBudget),
                effectiveStatus = Some(adSet.getFieldEffectiveStatus.toString),
                spendCap = None,
                canUseSpendCap = None,
                name = adSet.getFieldName,
                status = Some(adSet.getFieldStatus.toString),
                updatedTime = Some(adSet.getFieldUpdatedTime)
              )
            )
          )))
        case None =>
          Future(Redirect(controllers.facebook.api_account.campaign.ad_set.routes.AdSetController.ad_sets()))
      }
  }

  def createAdSet = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val current_cache = Await.result(Shared.Shared.redisClient.lrange[PendingCacheStructure](pendingCacheKey(Left(request)), 0, -1), 5 seconds).toList
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
          Shared.Shared.redisClient.lpush(
            pendingCacheKey(Left(request)),
            current_cache :+ PendingCacheStructure(
              id = current_cache.length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.FACEBOOK,
              changeCategory = ChangeCategory.AD_SET,
              changeData = adSetFormToDbo(adSet)
            ): _*
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
          val field_names = Utilities.getCaseClassParameter[AdSet]
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
                    changeData = adSetFormToDbo(adSet)
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
              changeData = adSetFormToDbo(adSet)
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
          changeData = DBObject("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.facebook.api_account.campaign.ad_set.routes.AdSetController.ad_sets()))
  }
}
