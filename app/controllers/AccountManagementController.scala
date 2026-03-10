package controllers

import javax.inject.Inject
import be.objectify.deadbolt.scala.DeadboltActions
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.ActionBuilders
import play.api.mvc._
import play.api.i18n.I18nSupport
import models.mongodb.MongoExtensions._
import models.mongodb.PermissionGroup
import models.mongodb.UserAccount
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import security.HandlerKeys

class AccountManagementController @Inject()(
  val controllerComponents: ControllerComponents,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders
)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {
  
  def accounts = deadbolt.Dynamic(name=PermissionGroup.Administrator.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit authRequest =>
      val userAccounts = UserAccount.userAccountCollection.find.toList.map{ accountObj => UserAccount.documentToUserAccount(accountObj) }
      Future(Ok(views.html.account_management(userAccounts)))
  }
}