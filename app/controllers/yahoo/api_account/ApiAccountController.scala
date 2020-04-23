package controllers.yahoo.api_account

import javax.inject.Inject

import Shared.Shared.{ChangeCategory, ChangeType, PendingCacheStructure, TrafficSource, _}
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.mongodb.casbah.Imports._
import helpers.yahoo.api_account.ApiAccountControllerHelper._
import models.mongodb.{PermissionGroup, _}
import models.mongodb.yahoo.Yahoo._
import play.api.Play.current
import play.api.cache.Cache
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Security}
import scala.concurrent.ExecutionContext.Implicits.global
import security.HandlerKeys

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class ApiAccountController @Inject()(
  val messagesApi: MessagesApi,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders
) extends Controller with I18nSupport {

    def json = Action.async {
      implicit request =>
        val api_account = yahooApiAccountCollection.find(DBObject()).toList
        Future(Ok(com.mongodb.util.JSON.serialize(api_account.toString())))
    }

  def api_account(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name=PermissionGroup.YahooRead.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.yahoo.api_account.api_account(
        yahooApiAccountCollection.find().skip(page * pageSize).limit(pageSize).toList.map(yahoo.Yahoo.dboToApiAccount),
        page,
        pageSize,
        orderBy,
        filter,
        yahooApiAccountCollection.count(),
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.YAHOO && x.changeCategory == ChangeCategory.API_ACCOUNT
          )
      )))
  }


  def newApiAccount = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request => Future(Ok(views.html.yahoo.api_account.new_api_account(apiAccountForm, List())))
  }


  def editApiAccount(id: String) = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      yahooApiAccountCollection.findOne(DBObject("_id" -> id)) match {
        case Some(api_account_obj) =>
          def api_account = yahoo.Yahoo.dboToApiAccount(api_account_obj)
          Future(Ok(views.html.yahoo.api_account.edit_api_account(
            id,
            apiAccountForm.fill(
              ApiAccount(
                _id = api_account._id,
                name = api_account.name,
                clientId = api_account.clientId,
                clientSecret = api_account.clientSecret,
                refreshToken = api_account.refreshToken
              )
            )
          )))
        case None =>
          Future(BadRequest("Not Found"))
      }
  }


  def createApiAccount = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      apiAccountForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(views.html.yahoo.api_account.new_api_account(formWithErrors, List()))),
        api_account => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.YAHOO,
              changeCategory = ChangeCategory.API_ACCOUNT,
              changeData = apiAccountToDBObject(api_account)
            )
          )
          Future(Redirect(controllers.yahoo.api_account.routes.ApiAccountController.api_account(0, 10, 2, "")))
        }
      )
  }


  def saveApiAccount(id: String) = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Cache.get(pending_cache_key).getOrElse(List()).asInstanceOf[List[PendingCacheStructure]]
      apiAccountForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.yahoo.api_account.edit_api_account(
              id,
              formWithErrors
            )
          ))
        },
        api_account => {
          Shared.Shared.redisClient.lpush(
            pending_cache_key,
            (current_cache :+ PendingCacheStructure(
              id = current_cache.length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.YAHOO,
              changeCategory = ChangeCategory.API_ACCOUNT,
              changeData = com.mongodb.util.JSON.serialize(api_account).asInstanceOf[DBObject]
            )): _*
          )
          Future(Redirect(controllers.yahoo.api_account.routes.ApiAccountController.api_account()))
        }
      )
  }


  def deleteApiAccount(id: String) = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Cache.get(pending_cache_key).getOrElse(List()).asInstanceOf[List[PendingCacheStructure]]
      Shared.Shared.redisClient.lpush(
        pending_cache_key,
        (current_cache :+ PendingCacheStructure(
          id = current_cache.length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.YAHOO,
          changeCategory = ChangeCategory.API_ACCOUNT,
          changeData = DBObject("apiAccountObjId" -> id)
        )): _*
      )
      Future(Redirect(controllers.yahoo.api_account.routes.ApiAccountController.api_account()))
  }


  def bulkNewApiAccount = deadbolt.Dynamic(name=PermissionGroup.YahooWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Cache.get(pending_cache_key).getOrElse(List()).asInstanceOf[List[PendingCacheStructure]]
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[ApiAccount]
          val api_account_data_list = Utilities.bulkImport(bulk, field_names)
          for (((api_account_data, action), index) <- api_account_data_list.zipWithIndex) {
            apiAccountForm.bind(api_account_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              api_account => {
                Shared.Shared.redisClient.lpush(
                  pending_cache_key,
                  (current_cache :+ PendingCacheStructure(
                    id = current_cache.length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.YAHOO,
                    changeCategory = ChangeCategory.API_ACCOUNT,
                    changeData = apiAccountToDBObject(api_account)
                  )): _*
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.yahoo.api_account.new_api_account(
          apiAccountForm,
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.yahoo.api_account.routes.ApiAccountController.api_account()))
      }
    }
  }
}
