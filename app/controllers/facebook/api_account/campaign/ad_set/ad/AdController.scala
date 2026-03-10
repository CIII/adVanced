package controllers.facebook.api_account.campaign.ad_set.ad

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import helpers.facebook.api_account.campaign.ad_set.ad.AdControllerHelper._
import models.mongodb.MongoExtensions._
import models.mongodb.facebook.Facebook._
import models.mongodb.{PermissionGroup, Utilities}
import play.api.i18n.I18nSupport
import play.api.mvc._
import security.HandlerKeys

import scala.jdk.CollectionConverters._
import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class AdController @Inject()(
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
          "apiId",
          "accountId",
          "adLabels",
          "adCreatives",
          "brandLiftStudies",
          "budgetRebalanceFlag",
          "buyingType",
          "canCreateBrandLiftStudy",
          "canUseSpendCap",
          "configuredStatus",
          "createdTime",
          "effectiveStatus",
          "name",
          "objective",
          "recommendations",
          "spendCap",
          "startTime",
          "status",
          "stopTime",
          "updatedTime"
        ),
        "ad",
        facebookCampaignCollection.namespace.getCollectionName
      )))
  }

  def ads(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.FacebookRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val ads = facebookAdCollection.find().skip(page * pageSize).limit(pageSize).toList
      Future(Ok(views.html.facebook.api_account.campaign.ad_set.ad.ads(
        ads.map(documentToFacebookEntity(_, "ad", None)),
        page,
        pageSize,
        orderBy,
        filter,
        facebookAdCollection.countSync().toInt,
        pendingCache(Left(request))
          .filter(x => x.trafficSource == TrafficSource.FACEBOOK && x.changeCategory == ChangeCategory.AD)
      )))
  }

  def newAd = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.facebook.api_account.campaign.ad_set.ad.new_ad(
        adForm,
        List()
      )))
  }

  def editAd(api_id: String) = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      facebookAdCollection.findOne(Document("apiId" -> api_id)) match {
        case Some(adObj) =>
          val ad = documentToFacebookEntity(adObj, "ad", None)
          Future(Ok(views.html.facebook.api_account.campaign.ad_set.ad.edit_ad(
            api_id,
            adForm.fill(
              AdForm(
                AdParent(
                  apiAccountObjId = Option(adObj.getString("apiAccountObjId")),
                  campaignApiId = Option(adObj.getString("campaignApiId")),
                  adSetApiId = Option(adObj.getString("adSetApiId"))
                ),
                apiId = Option(ad.getString("id")),
                accountId = Option(ad.getString("account_id")).getOrElse(""),
                adSetId = Option(ad.getString("adset_id")).getOrElse(""),
                campaignId = Option(ad.getString("campaign_id")).getOrElse(""),
                adLabels = None,
                adCreatives = None,
                bidAmount = Option(ad.getLong("bid_amount")).map(_.toLong),
                createdTime = Option(ad.getString("created_time")),
                effectiveStatus = Option(ad.getString("effective_status")),
                name = Option(ad.getString("name")).getOrElse(""),
                status = Option(ad.getString("status")),
                updatedTime = Option(ad.getString("updated_time"))
              )
            )
          )))
        case None =>
          Future(Redirect(controllers.facebook.api_account.campaign.ad_set.ad.routes.AdController.ads()))
      }
  }

  def createAd = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      adForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.facebook.api_account.campaign.ad_set.ad.new_ad(
              formWithErrors,
              List()
            )
          ))
        },
        ad => {
          // TODO: Migrate to RedisService injection - redisClient.lpush removed
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.FACEBOOK,
              changeCategory = ChangeCategory.AD,
              changeData = adFormToDocument(ad)
            )
          )
          Future(Redirect(controllers.facebook.api_account.campaign.ad_set.ad.routes.AdController.ads()))
        }
      )
  }

  def bulkNewAd = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[AdForm]
          val ad_data_list = Utilities.bulkImport(bulk, field_names)
          for (((ad_data, action), index) <- ad_data_list.zipWithIndex) {
            adForm.bind(ad_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              ad => {
                setPendingCache(
                  Left(request),
                  pendingCache(Left(request)) :+ PendingCacheStructure(
                    id = pendingCache(Left(request)).length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.FACEBOOK,
                    changeCategory = ChangeCategory.AD,
                    changeData = adFormToDocument(ad)
                  )
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.facebook.api_account.campaign.ad_set.ad.new_ad(
          adForm,
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.facebook.api_account.campaign.ad_set.ad.routes.AdController.ads()))
      }
    }
  }


  def saveAd(api_id: String) = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      adForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.facebook.api_account.campaign.ad_set.ad.edit_ad(
              api_id,
              formWithErrors
            )
          ))
        },
        ad => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.FACEBOOK,
              changeCategory = ChangeCategory.AD,
              changeData = adFormToDocument(ad)
            )
          )
          Future(Redirect(controllers.facebook.api_account.campaign.ad_set.ad.routes.AdController.ads()))
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
          changeCategory = ChangeCategory.AD,
          changeData = Document("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.facebook.api_account.campaign.ad_set.ad.routes.AdController.ads()))
  }
}
