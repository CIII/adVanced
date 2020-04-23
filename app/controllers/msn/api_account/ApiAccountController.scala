package controllers.msn.api_account

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.mongodb.casbah.Imports._
import helpers.msn.api_account.ApiAccountControllerHelper
import helpers.msn.api_account.ApiAccountControllerHelper._
import models.mongodb._
import models.mongodb.msn.Msn._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import security.HandlerKeys

import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ApiAccountController @Inject()(
  val messagesApi: MessagesApi,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders
) extends Controller with I18nSupport {

  def api_accounts(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name=PermissionGroup.MSNRead.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.msn.api_account.api_accounts(
        msnApiAccountCollection.find().toList.map(dboToApiAccount),
        page,
        pageSize,
        orderBy,
        filter,
        msnApiAccountCollection.count(),
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.MSN && x.changeCategory == ChangeCategory.API_ACCOUNT
          )
      )))
  }


  def newApiAccount = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request => Future(Ok(views.html.msn.api_account.new_api_account(apiAccountForm, List())))
  }


  def editApiAccount(id: String) = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      msnApiAccountCollection.findOne(DBObject("_id" -> new ObjectId(id))) match {
        case Some(account_obj) =>
          val apiAccount = dboToApiAccount(account_obj)
          Future(Ok(views.html.msn.api_account.edit_api_account(
            id,
            apiAccountForm.fill(
              ApiAccount(
                _id = apiAccount._id,
                name = apiAccount.name,
                userName = apiAccount.userName,
                password = apiAccount.password,
                developerToken = apiAccount.developerToken
              )
            )
          )))
        case None =>
          Future(BadRequest("Not Found"))
      }
  }


  def createApiAccount = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      apiAccountForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(views.html.msn.api_account.new_api_account(formWithErrors, List()))),
        api_account => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.MSN,
              changeCategory = ChangeCategory.API_ACCOUNT,
              changeData = apiAccountToDbo(api_account)
            )
          )
          Future(Redirect(controllers.msn.api_account.routes.ApiAccountController.api_accounts()))
        }
      )
  }


  def saveApiAccount(id: String) = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      apiAccountForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.msn.api_account.edit_api_account(
              id,
              formWithErrors
            )
          ))
        },
        api_account => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.MSN,
              changeCategory = ChangeCategory.API_ACCOUNT,
              changeData = apiAccountToDbo(api_account)
            )
          )
          Future(Redirect(controllers.msn.api_account.routes.ApiAccountController.api_accounts()))
        }
      )
  }

  def deleteApiAccount(id: String) = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
          id = pendingCache(Left(request)).length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.MSN,
          changeCategory = ChangeCategory.API_ACCOUNT,
          changeData = DBObject("apiId" -> id)
        )
      )
      Future(Redirect(controllers.msn.api_account.routes.ApiAccountController.api_accounts()))
  }


  def bulkNewApiAccount = deadbolt.Dynamic(name = PermissionGroup.MSNWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[ApiAccount]
          val account_data_list = Utilities.bulkImport(bulk, field_names)
          for (((account_data, action), index) <- account_data_list.zipWithIndex) {
            apiAccountForm.bind(account_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              api_account => {
                setPendingCache(
                  Left(request),
                  pendingCache(Left(request)) :+ PendingCacheStructure(
                    id = pendingCache(Left(request)).length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.MSN,
                    changeCategory = ChangeCategory.API_ACCOUNT,
                    changeData = apiAccountToDbo(api_account)
                  )
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.msn.api_account.new_api_account(
          apiAccountForm,
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.msn.api_account.routes.ApiAccountController.api_accounts()))
      }
    }
  }
}
