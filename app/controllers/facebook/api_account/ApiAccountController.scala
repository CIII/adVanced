package controllers.facebook.api_account

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.mongodb.casbah.Imports._
import helpers.facebook.api_account.ApiAccountControllerHelper._
import models.mongodb.facebook.Facebook._
import models.mongodb.{PermissionGroup, Utilities}
import org.bson.types.ObjectId
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import security.HandlerKeys

import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApiAccountController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {

  def api_accounts(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name=PermissionGroup.FacebookRead.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.facebook.api_account.api_accounts(
        facebookApiAccountCollection.find().toList.map(dboToApiAccount),
        page,
        pageSize,
        orderBy,
        filter,
        facebookApiAccountCollection.count(),
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.FACEBOOK && x.changeCategory == ChangeCategory.API_ACCOUNT
          )
      )))
  }


  def newApiAccount = deadbolt.Dynamic(name=PermissionGroup.FacebookWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request => Future(Ok(views.html.facebook.api_account.new_api_account(facebookApiAccountForm, List())))
  }


  def editApiAccount(id: String) = deadbolt.Dynamic(name=PermissionGroup.FacebookWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      facebookApiAccountCollection.findOne(DBObject("_id" -> new ObjectId(id))) match {
        case Some(account_obj) =>
          val apiAccount = dboToApiAccount(account_obj)
          Future(Ok(views.html.facebook.api_account.edit_api_account(
            id,
            facebookApiAccountForm.fill(
              FacebookApiAccount(
                _id = apiAccount._id,
                accountId = apiAccount.accountId,
                applicationSecret = apiAccount.applicationSecret,
                accessToken = apiAccount.accessToken
              )
            )
          )))
        case None =>
          Future(BadRequest("Not Found"))
      }
  }


  def createApiAccount = deadbolt.Dynamic(name=PermissionGroup.FacebookWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      facebookApiAccountForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(views.html.facebook.api_account.new_api_account(formWithErrors, List()))),
        api_account => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.FACEBOOK,
              changeCategory = ChangeCategory.API_ACCOUNT,
              changeData = apiAccountToDBO(api_account)
            )
          )
          Future(Redirect(controllers.facebook.api_account.routes.ApiAccountController.api_accounts()))
        }
      )
  }


  def saveApiAccount(id: String) = deadbolt.Dynamic(name=PermissionGroup.FacebookWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
      implicit request =>
        facebookApiAccountForm.bindFromRequest.fold(
          formWithErrors => {
            Future(BadRequest(
              views.html.facebook.api_account.edit_api_account(
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
                trafficSource = TrafficSource.FACEBOOK,
                changeCategory = ChangeCategory.API_ACCOUNT,
                changeData = apiAccountToDBO(api_account)
              )
            )
            Future(Redirect(controllers.facebook.api_account.routes.ApiAccountController.api_accounts()))
          })
  }

  def deleteApiAccount(id: String) = deadbolt.Dynamic(name=PermissionGroup.FacebookWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
          id = pendingCache(Left(request)).length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.FACEBOOK,
          changeCategory = ChangeCategory.API_ACCOUNT,
          changeData = DBObject("apiId" -> id)
        )
      )
      Future(Redirect(controllers.facebook.api_account.routes.ApiAccountController.api_accounts()))
  }


  def bulkNewApiAccount = deadbolt.Dynamic(name = PermissionGroup.FacebookWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[FacebookApiAccount]
          val account_data_list = Utilities.bulkImport(bulk, field_names)
          for (((account_data, action), index) <- account_data_list.zipWithIndex) {
            facebookApiAccountForm.bind(account_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              api_account => {
                setPendingCache(
                  Left(request),
                  pendingCache(Left(request)) :+ PendingCacheStructure(
                    id = pendingCache(Left(request)).length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.FACEBOOK,
                    changeCategory = ChangeCategory.API_ACCOUNT,
                    changeData = apiAccountToDBO(api_account)
                  )
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.facebook.api_account.new_api_account(
          facebookApiAccountForm,
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.facebook.api_account.routes.ApiAccountController.api_accounts()))
      }
    }
  }
}
