package controllers.yahoo.api_account.advertiser.campaign.adgroup.keyword

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.mongodb.casbah.Imports._
import helpers.yahoo.api_account.advertiser.campaign.adgroup.keyword.KeywordControllerHelper._
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
          "adgroupObjId",
          "adgroupApiId",
          "adGroupObjId",
          "adGroupApiId",
          "keywordObjId",
          "keywordApiId"
        ),
        "keyword",
        yahooKeywordCollection
      )))
  }

  def keyword(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name=PermissionGroup.YahooRead.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.yahoo.api_account.advertiser.campaign.adgroup.keyword.keyword(
        yahooKeywordCollection.find().skip(page * pageSize).limit(pageSize).toList.map(yahoo.Yahoo.dboToKeyword),
        page,
        pageSize,
        orderBy,
        filter,
        yahooKeywordCollection.count(),
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.YAHOO && x.changeCategory == ChangeCategory.KEYWORD
          )
      )))
  }


  def newKeyword = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request => Future(Ok(views.html.yahoo.api_account.advertiser.campaign.adgroup.keyword.new_keyword(adGroupKeywordForm, List())))
  }


  def editKeyword(id: String) = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      yahooKeywordCollection.findOne(DBObject("_id" -> id)) match {
        case Some(adgroup_keyword_obj) =>
          def adgroup_keyword = yahoo.Yahoo.dboToKeyword(adgroup_keyword_obj)
          /*Ok(views.html.yahoo.api_account.advertiser.adgroup.keyword.edit_keyword(
              id,
              adgroupForm.fill(
                Keyword(
                  _id = adgroup_keyword._id,

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
      adGroupKeywordForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(views.html.yahoo.api_account.advertiser.campaign.adgroup.keyword.new_keyword(formWithErrors, List()))),
        adgroup_keyword => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.YAHOO,
              changeCategory = ChangeCategory.CAMPAIGN_KEYWORD,
              changeData = keywordToDBObject(adgroup_keyword)
            )
          )
          Future(Redirect(controllers.yahoo.api_account.advertiser.campaign.adgroup.keyword.routes.KeywordController.keyword(0, 10, 2, "")))
        }
      )
  }


  def saveKeyword(id: String) = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Cache.get(pending_cache_key).getOrElse(List()).asInstanceOf[List[PendingCacheStructure]]
      adGroupKeywordForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.yahoo.api_account.advertiser.campaign.adgroup.keyword.edit_keyword(
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
              changeCategory = ChangeCategory.KEYWORD,
              changeData = com.mongodb.util.JSON.serialize(adgroup).asInstanceOf[DBObject]
            )): _*
          )
          Future(Redirect(controllers.yahoo.api_account.advertiser.campaign.adgroup.keyword.routes.KeywordController.keyword()))
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
          changeCategory = ChangeCategory.KEYWORD,
          changeData = DBObject("keywordObjId" -> id)
        )): _*
      )
      Future(Redirect(controllers.yahoo.api_account.advertiser.campaign.adgroup.keyword.routes.KeywordController.keyword()))
  }


  def bulkNewKeyword = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Cache.get(pending_cache_key).getOrElse(List()).asInstanceOf[List[PendingCacheStructure]]
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[ApiAccount]
          val adgroup_keyword_data_list = Utilities.bulkImport(bulk, field_names)
          for (((adgroup_keyword_data, action), index) <- adgroup_keyword_data_list.zipWithIndex) {
            adGroupKeywordForm.bind(adgroup_keyword_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              adgroup_keyword => {
                Shared.Shared.redisClient.lpush(
                  pending_cache_key,
                  (current_cache :+ PendingCacheStructure(
                    id = current_cache.length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.YAHOO,
                    changeCategory = ChangeCategory.KEYWORD,
                    changeData = keywordToDBObject(adgroup_keyword)
                  )): _*
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.yahoo.api_account.advertiser.campaign.adgroup.keyword.new_keyword(
          adGroupKeywordForm,
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.yahoo.api_account.advertiser.campaign.adgroup.keyword.routes.KeywordController.keyword()))
      }
    }
  }
}