package controllers.yahoo.api_account.advertiser.campaign.adgroup

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.mongodb.casbah.Imports._
import helpers.yahoo.api_account.advertiser.campaign.adgroup.AdGroupControllerHelper._
import models.mongodb.yahoo.Yahoo._
import models.mongodb.{PermissionGroup, Utilities, yahoo}
import play.api.Play.current
import play.api.cache.Cache
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Security}
import security.HandlerKeys

import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AdGroupController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {

  def json = Action.async {
    implicit request =>
      Future(Ok(controllers.json(
        request,
        List(
          "apiAccountObjId",
          "advertiserObjId",
          "advertiserApiId",
          "campaignObjId",
          "campaignApiId",
          "adGroupObjId",
          "adGroupApiId"
        ),
        "adGroup",
        yahooAdGroupCollection
      )))
  }

  def adgroup(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name=PermissionGroup.YahooRead.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.yahoo.api_account.advertiser.campaign.adgroup.adgroup(
        yahooAdGroupCollection.find().skip(page * pageSize).limit(pageSize).toList.map(yahoo.Yahoo.dboToAdGroup),
        page,
        pageSize,
        orderBy,
        filter,
        yahooAdGroupCollection.count(),
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.YAHOO && x.changeCategory == ChangeCategory.AD_GROUP
          )
      )))
  }


  def newAdGroup = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request => Future(Ok(views.html.yahoo.api_account.advertiser.campaign.adgroup.new_adgroup(adGroupForm, List())))
  }


  def editAdGroup(id: String) = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      yahooAdGroupCollection.findOne(DBObject("_id" -> id)) match {
        case Some(adgroup_obj) =>
          def adgroup = yahoo.Yahoo.dboToAdGroup(adgroup_obj)
          Future(Ok(views.html.yahoo.api_account.advertiser.campaign.adgroup.edit_adgroup(
            id,
            adGroupForm.fill(
              AdGroup(
                _id = adgroup._id,
                advertiserObjId = adgroup.advertiserObjId,
                advertiserApiId = adgroup.advertiserApiId,
                campaignObjId = adgroup.campaignObjId,
                campaignApiId = adgroup.campaignApiId,
                apiId = adgroup.apiId,
                adGroupName = adgroup.adGroupName,
                bidSet = adgroup.bidSet,
                status = adgroup.status,
                startDateStr = adgroup.startDateStr,
                endDateStr = adgroup.endDateStr,
                advancedGeoPos = adgroup.advancedGeoPos,
                advancedGeoNeg = adgroup.advancedGeoNeg,
                biddingStrategy = adgroup.biddingStrategy,
                epcaGoal = adgroup.epcaGoal
              )
            )
          )))
        case None =>
          Future(BadRequest("Not Found"))
      }
  }


  def createAdGroup = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      adGroupForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(views.html.yahoo.api_account.advertiser.campaign.adgroup.new_adgroup(formWithErrors, List()))),
        adgroup => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.YAHOO,
              changeCategory = ChangeCategory.AD_GROUP,
              changeData = adGroupToDBObject(adgroup)
            )
          )
          Future(Redirect(controllers.yahoo.api_account.advertiser.campaign.adgroup.routes.AdGroupController.adgroup(0, 10, 2, "")))
        }
      )
  }


  def saveAdGroup(id: String) = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Cache.get(pending_cache_key).getOrElse(List()).asInstanceOf[List[PendingCacheStructure]]
      adGroupForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.yahoo.api_account.advertiser.campaign.adgroup.edit_adgroup(
              id,
              formWithErrors
            )
          ))
        },
        adgroup => {
          Shared.Shared.redisClient.lpush(
            pending_cache_key,
            (current_cache :+ PendingCacheStructure(
              id = current_cache.length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.YAHOO,
              changeCategory = ChangeCategory.AD_GROUP,
              changeData = com.mongodb.util.JSON.serialize(adgroup).asInstanceOf[DBObject]
            )): _*
          )
          Future(Redirect(controllers.yahoo.api_account.advertiser.campaign.adgroup.routes.AdGroupController.adgroup()))
        }
      )
  }


  def deleteAdGroup(id: String) = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Cache.get(pending_cache_key).getOrElse(List()).asInstanceOf[List[PendingCacheStructure]]
      Shared.Shared.redisClient.lpush(
        pending_cache_key,
        (current_cache :+ PendingCacheStructure(
          id = current_cache.length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.YAHOO,
          changeCategory = ChangeCategory.CAMPAIGN,
          changeData = DBObject("campaignObjId" -> id)
        )): _*
      )
      Future(Redirect(controllers.yahoo.api_account.advertiser.campaign.routes.CampaignController.campaign()))
  }


  def bulkNewAdGroup = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Cache.get(pending_cache_key).getOrElse(List()).asInstanceOf[List[PendingCacheStructure]]
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[ApiAccount]
          val campaign_data_list = Utilities.bulkImport(bulk, field_names)
          for (((campaign_data, action), index) <- campaign_data_list.zipWithIndex) {
            adGroupForm.bind(campaign_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              adgroup => {
                Shared.Shared.redisClient.lpush(
                  pending_cache_key,
                  (current_cache :+ PendingCacheStructure(
                    id = current_cache.length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.YAHOO,
                    changeCategory = ChangeCategory.CAMPAIGN,
                    changeData = adGroupToDBObject(adgroup)
                  )): _*
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.yahoo.api_account.advertiser.campaign.adgroup.new_adgroup(
          adGroupForm,
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.yahoo.api_account.advertiser.campaign.adgroup.routes.AdGroupController.adgroup()))
      }
    }
  }
}