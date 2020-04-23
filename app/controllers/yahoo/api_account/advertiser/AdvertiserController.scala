package controllers.yahoo.api_account.advertiser

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.mongodb.casbah.Imports._
import helpers.yahoo.api_account.advertiser.AdvertiserControllerHelper._
import models.mongodb.yahoo.Yahoo._
import models.mongodb.{PermissionGroup, Utilities, yahoo}
import play.api.Play.current
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Security}
import security.HandlerKeys
import scala.concurrent.ExecutionContext.Implicits.global

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class AdvertiserController @Inject()(
  val messagesApi: MessagesApi,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders
) extends Controller with I18nSupport {

  def json = Action.async {
    implicit request =>
      Future(Ok(controllers.json(
        request,
        List(
          "apiAccountObjId",
          "advertiserObjId",
          "advertiserApiId",
          "tsecs"
        ),
        "advertiser",
        yahooAdvertiserCollection
      )))
  }

  def advertiser(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name=PermissionGroup.YahooRead.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.yahoo.api_account.advertiser.advertiser(
        yahooAdvertiserCollection.find().skip(page * pageSize).limit(pageSize).toList.map(x => yahoo.Yahoo.dboToAdvertiser(x.as[List[DBObject]]("advertiser").head.as[DBObject]("object"))),
        page,
        pageSize,
        orderBy,
        filter,
        yahooAdvertiserCollection.count(),
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.YAHOO && x.changeCategory == ChangeCategory.ADVERTISER
          )
      )))
  }


  def newAdvertiser = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request => Future(Ok(views.html.yahoo.api_account.advertiser.new_advertiser(advertiserForm, List())))
  }


  def editAdvertiser(id: String) = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      yahooApiAccountCollection.findOne(DBObject("_id" -> id)) match {
        case Some(advertiser_obj) =>
          def advertiser = yahoo.Yahoo.dboToAdvertiser(advertiser_obj)
          Future(Ok(views.html.yahoo.api_account.advertiser.edit_advertiser(id, advertiserForm.fill(advertiser))))
        case None =>
          Future(BadRequest("Not Found"))
      }
  }


  def createAdvertiser = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      advertiserForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(views.html.yahoo.api_account.advertiser.new_advertiser(formWithErrors, List()))),
        advertiser => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.YAHOO,
              changeCategory = ChangeCategory.ADVERTISER,
              changeData = advertiserToDBObject(advertiser)
            )
          )
          Future(Redirect(controllers.yahoo.api_account.advertiser.routes.AdvertiserController.advertiser(0, 10, 2, "")))
        }
      )
  }


  def saveAdvertiser(id: String) = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Await.result(Shared.Shared.redisClient.lrange[PendingCacheStructure](pending_cache_key, 0, -1), 5 seconds)
      advertiserForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.yahoo.api_account.advertiser.edit_advertiser(
              id,
              formWithErrors
            )
          ))
        },
        advertiser => {
          Shared.Shared.redisClient.lpush(
            pending_cache_key,
            (current_cache :+ PendingCacheStructure(
              id = current_cache.length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.YAHOO,
              changeCategory = ChangeCategory.ADVERTISER,
              changeData = com.mongodb.util.JSON.serialize(advertiser).asInstanceOf[DBObject]
            )): _*
          )
          Future(Redirect(controllers.yahoo.api_account.advertiser.routes.AdvertiserController.advertiser()))
        }
      )
  }


  def deleteAdvertiser(id: String) = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Await.result(Shared.Shared.redisClient.lrange[PendingCacheStructure](pending_cache_key, 0, -1), 5 seconds)
      Shared.Shared.redisClient.lpush(
        pending_cache_key,
        (current_cache :+ PendingCacheStructure(
          id = current_cache.length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.YAHOO,
          changeCategory = ChangeCategory.ADVERTISER,
          changeData = DBObject("advertiserObjId" -> id)
        )): _*
      )
      Future(Redirect(controllers.yahoo.api_account.advertiser.routes.AdvertiserController.advertiser()))
  }


  def bulkNewAdvertiser = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Await.result(Shared.Shared.redisClient.lrange[PendingCacheStructure](pending_cache_key, 0, -1), 5 seconds)
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[ApiAccount]
          val advertiser_data_list = Utilities.bulkImport(bulk, field_names)
          for (((advertiser_data, action), index) <- advertiser_data_list.zipWithIndex) {
            advertiserForm.bind(advertiser_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              advertiser => {
                Shared.Shared.redisClient.lpush(
                  pending_cache_key,
                  (current_cache :+ PendingCacheStructure(
                    id = current_cache.length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.YAHOO,
                    changeCategory = ChangeCategory.API_ACCOUNT,
                    changeData = advertiserToDBObject(advertiser)
                  )): _*
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.yahoo.api_account.advertiser.new_advertiser(
          advertiserForm,
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.yahoo.api_account.advertiser.routes.AdvertiserController.advertiser()))
      }
    }
  }
}