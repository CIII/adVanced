package controllers.yahoo.api_account.advertiser.campaign.keyword

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.mongodb.casbah.Imports._
import helpers.yahoo.api_account.advertiser.campaign.keyword.KeywordControllerHelper._
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

class KeywordController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {

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
          "adGroupApiId",
          "keywordObjId",
          "keywordApiId"
        ),
        "keyword",
        yahooKeywordCollection
      )))
  }

  def keyword(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.YahooRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.yahoo.api_account.advertiser.campaign.keyword.keyword(
        yahooKeywordCollection.find().skip(page * pageSize).limit(pageSize).toList.map(yahoo.Yahoo.dboToKeyword),
        page,
        pageSize,
        orderBy,
        filter,
        yahooKeywordCollection.count(),
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.YAHOO && x.changeCategory == ChangeCategory.CAMPAIGN_KEYWORD
          )
      )))
  }


  def newKeyword = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request => Future(Ok(views.html.yahoo.api_account.advertiser.campaign.keyword.new_keyword(campaignKeywordForm, List())))
  }


  def editKeyword(id: String) = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      yahooKeywordCollection.findOne(DBObject("_id" -> id)) match {
        case Some(campaign_keyword_obj) =>
          def campaign_keyword = yahoo.Yahoo.dboToKeyword(campaign_keyword_obj)
          /*Ok(views.html.yahoo.api_account.advertiser.campaign.keyword.edit_keyword(
              id,
              campaignForm.fill(
                Keyword(
                  _id = campaign_keyword._id,

                )
              )
            ))*/
          Future(BadRequest("Not Found"))
        case None =>
          Future(BadRequest("Not Found"))
      }
  }


  def createKeyword = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignKeywordForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(views.html.yahoo.api_account.advertiser.campaign.keyword.new_keyword(formWithErrors, List()))),
        campaign_keyword => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.YAHOO,
              changeCategory = ChangeCategory.CAMPAIGN_KEYWORD,
              changeData = keywordToDBObject(campaign_keyword)
            )
          )
          Future(Redirect(controllers.yahoo.api_account.advertiser.campaign.keyword.routes.KeywordController.keyword(0, 10, 2, "")))
        }
      )
  }


  def saveKeyword(id: String) = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Cache.get(pending_cache_key).getOrElse(List()).asInstanceOf[List[PendingCacheStructure]]
      campaignKeywordForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.yahoo.api_account.advertiser.campaign.keyword.edit_keyword(
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
              changeCategory = ChangeCategory.CAMPAIGN_KEYWORD,
              changeData = com.mongodb.util.JSON.serialize(campaign).asInstanceOf[DBObject]
            )): _*
          )
          Future(Redirect(controllers.yahoo.api_account.advertiser.campaign.keyword.routes.KeywordController.keyword()))
        }
      )
  }


  def deleteKeyword(id: String) = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Cache.get(pending_cache_key).getOrElse(List()).asInstanceOf[List[PendingCacheStructure]]
      Shared.Shared.redisClient.lpush(
        pending_cache_key,
        (current_cache :+ PendingCacheStructure(
          id = current_cache.length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.YAHOO,
          changeCategory = ChangeCategory.CAMPAIGN_KEYWORD,
          changeData = DBObject("keywordObjId" -> id)
        )): _*
      )
      Future(Redirect(controllers.yahoo.api_account.advertiser.campaign.routes.CampaignController.campaign()))
  }


  def bulkNewKeyword = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Cache.get(pending_cache_key).getOrElse(List()).asInstanceOf[List[PendingCacheStructure]]
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[ApiAccount]
          val campaign_keyword_data_list = Utilities.bulkImport(bulk, field_names)
          for (((campaign_keyword_data, action), index) <- campaign_keyword_data_list.zipWithIndex) {
            campaignKeywordForm.bind(campaign_keyword_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              campaign_keyword => {
                Shared.Shared.redisClient.lpush(
                  pending_cache_key,
                  (current_cache :+ PendingCacheStructure(
                    id = current_cache.length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.YAHOO,
                    changeCategory = ChangeCategory.CAMPAIGN_KEYWORD,
                    changeData = keywordToDBObject(campaign_keyword)
                  )): _*
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.yahoo.api_account.advertiser.campaign.keyword.new_keyword(
          campaignKeywordForm,
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.yahoo.api_account.advertiser.campaign.keyword.routes.KeywordController.keyword()))
      }
    }
  }
}