package controllers.yahoo.api_account.advertiser.campaign

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.mongodb.casbah.Imports._
import helpers.yahoo.api_account.advertiser.campaign.CampaignControllerHelper._
import models.mongodb.yahoo.Yahoo._
import models.mongodb.{PermissionGroup, Utilities, yahoo}
import play.api.Play.current
import play.api.cache.Cache
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Security}
import security.HandlerKeys
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class CampaignController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {

  def json = Action.async {
    implicit request =>
      Future(Ok(controllers.json(
        request,
        List(
          "apiAccountObjId",
          "advertiserObjId",
          "advertiserApiId",
          "campaignObjId",
          "campaignApiId"
        ),
        "campaign",
        yahooCampaignCollection
      )))
  }

  def campaign = deadbolt.Dynamic(name=PermissionGroup.YahooRead.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.yahoo.api_account.advertiser.campaign.campaign(
        yahooCampaignCollection.find().toList.map(yahoo.Yahoo.dboToCampaign),
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.YAHOO && x.changeCategory == ChangeCategory.CAMPAIGN
          )
      )))
  }


  def newCampaign = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request => Future(Ok(views.html.yahoo.api_account.advertiser.campaign.new_campaign(campaignForm, List())))
  }


  def editCampaign(id: String) = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      yahooApiAccountCollection.findOne(DBObject("_id" -> id)) match {
        case Some(campaign_obj) =>
          def campaign = yahoo.Yahoo.dboToCampaign(campaign_obj)
          Future(Ok(views.html.yahoo.api_account.advertiser.campaign.edit_campaign(
            id,
            campaignForm.fill(
              Campaign(
                _id = campaign._id,
                advertiserObjId = campaign.advertiserObjId,
                advertiserApiId = campaign.advertiserApiId,
                budget = campaign.budget,
                budgetType = campaign.budgetType,
                campaignName = campaign.campaignName,
                channel = campaign.channel,
                apiId = campaign.apiId,
                language = campaign.language,
                objective = campaign.objective,
                status = campaign.status,
                isPartnerNetwork = campaign.isPartnerNetwork,
                defaultLandingUrl = campaign.defaultLandingUrl,
                trackingPartner = campaign.trackingPartner,
                appLocale = campaign.appLocale,
                advancedGeoPos = campaign.advancedGeoPos,
                advancedGeoNeg = campaign.advancedGeoNeg
              )
            )
          )))
        case None =>
          Future(BadRequest("Not Found"))
      }
  }


  def createCampaign = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(views.html.yahoo.api_account.advertiser.campaign.new_campaign(formWithErrors, List()))),
        campaign => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.YAHOO,
              changeCategory = ChangeCategory.CAMPAIGN,
              changeData = campaignToDBObject(campaign)
            )
          )
          Future(Redirect(controllers.yahoo.api_account.advertiser.campaign.routes.CampaignController.campaign()))
        }
      )
  }


  def saveCampaign(id: String) = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Cache.get(pending_cache_key).getOrElse(List()).asInstanceOf[List[PendingCacheStructure]]
      campaignForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.yahoo.api_account.advertiser.campaign.edit_campaign(
              id,
              formWithErrors
            )
          ))
        },
        campaign => {
          Shared.Shared.redisClient.lpush(
            pending_cache_key,
            (current_cache :+ PendingCacheStructure(
              id = current_cache.length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.YAHOO,
              changeCategory = ChangeCategory.CAMPAIGN,
              changeData = com.mongodb.util.JSON.serialize(campaign).asInstanceOf[DBObject]
            )): _*
          )
          Future(Redirect(controllers.yahoo.api_account.advertiser.campaign.routes.CampaignController.campaign()))
        }
      )
  }


  def deleteCampaign(id: String) = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
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


  def bulkNewCampaign = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Cache.get(pending_cache_key).getOrElse(List()).asInstanceOf[List[PendingCacheStructure]]
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[ApiAccount]
          val campaign_data_list = Utilities.bulkImport(bulk, field_names)
          for (((campaign_data, action), index) <- campaign_data_list.zipWithIndex) {
            campaignForm.bind(campaign_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              campaign => {
                Shared.Shared.redisClient.lpush(
                  pending_cache_key,
                  (current_cache :+ PendingCacheStructure(
                    id = current_cache.length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.YAHOO,
                    changeCategory = ChangeCategory.CAMPAIGN,
                    changeData = campaignToDBObject(campaign)
                  )): _*
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.yahoo.api_account.advertiser.campaign.new_campaign(
          campaignForm,
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.yahoo.api_account.advertiser.campaign.routes.CampaignController.campaign()))
      }
    }
  }
}
