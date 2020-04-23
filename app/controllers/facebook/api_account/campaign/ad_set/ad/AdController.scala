package controllers.facebook.api_account.campaign.ad_set.ad

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.facebook.ads.sdk.Ad
import com.mongodb.casbah.Imports._
import helpers.facebook.api_account.campaign.ad_set.ad.AdControllerHelper._
import models.mongodb.facebook.Facebook._
import models.mongodb.{PermissionGroup, Utilities}
import play.api.cache.CacheApi
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import security.HandlerKeys

import scala.collection.JavaConverters._
import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class AdController @Inject()(
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
        facebookCampaignCollection
      )))
  }

  def ads(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.FacebookRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val ads = facebookAdCollection.find().skip(page * pageSize).limit(pageSize).toList
      Future(Ok(views.html.facebook.api_account.campaign.ad_set.ad.ads(
        ads.map(dboToFacebookEntity[Ad](_, "ad", None)),
        page,
        pageSize,
        orderBy,
        filter,
        facebookAdCollection.count(),
        cache.get(pendingCacheKey(Left(request)))
          .getOrElse(List())
          .asInstanceOf[List[PendingCacheStructure]]
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
      facebookAdCollection.findOne(DBObject("apiId" -> api_id)) match {
        case Some(adObj) =>
          def ad = dboToFacebookEntity[Ad](adObj, "ad", None)
          Future(Ok(views.html.facebook.api_account.campaign.ad_set.ad.edit_ad(
            api_id,
            adForm.fill(
              AdForm(
                AdParent(
                  apiAccountObjId = adObj.getAs[String]("apiAccountObjId"),
                  campaignApiId = adObj.getAs[String]("campaignApiId"),
                  adSetApiId = adObj.getAs[String]("adSetApiId")
                ),
                apiId = Some(ad.getId),
                accountId = ad.getFieldAccountId,
                adSetId = ad.getFieldAdsetId,
                campaignId = ad.fetch().getId,
                adLabels = Some(ad.getFieldAdlabels.toArray.toList.asInstanceOf[List[String]]),
                adCreatives = Some(
                  ad.getAdCreatives.execute.asScala.map { c =>
                    AdCreative(
                      adImage = Some(c.getFieldImageHash),
                      adLink = c.getFieldImageUrl,
                      adMessage = Some(c.getFieldName)
                    )
                  }.toList
                ),
                bidAmount = Some(ad.getFieldBidAmount),
                createdTime = Some(ad.getFieldCreatedTime),
                effectiveStatus = Some(ad.getFieldEffectiveStatus.toString),
                name = ad.getFieldName,
                status = Some(ad.getFieldStatus.toString),
                updatedTime = Some(ad.getFieldUpdatedTime)
              )
            )
          )))
        case None =>
          Future(Redirect(controllers.facebook.api_account.campaign.ad_set.ad.routes.AdController.ads()))
      }
  }

  def createAd = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val current_cache = Await.result(Shared.Shared.redisClient.lrange[PendingCacheStructure](pendingCacheKey(Left(request)), 0, -1), 5 seconds).toList
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
          Shared.Shared.redisClient.lpush(
            pendingCacheKey(Left(request)),
            current_cache :+ PendingCacheStructure(
              id = current_cache.length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.FACEBOOK,
              changeCategory = ChangeCategory.AD,
              changeData = adFormToDbo(ad)
            ): _*
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
          val field_names = Utilities.getCaseClassParameter[Ad]
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
                    changeData = adFormToDbo(ad)
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
              changeData = adFormToDbo(ad)
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
          changeData = DBObject("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.facebook.api_account.campaign.ad_set.ad.routes.AdController.ads()))
  }
}
