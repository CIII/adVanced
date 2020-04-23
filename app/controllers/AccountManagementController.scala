package controllers

import javax.inject.Inject
import play.api.i18n.MessagesApi
import be.objectify.deadbolt.scala.DeadboltActions
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.ActionBuilders
import play.api.mvc.Controller
import play.api.i18n.I18nSupport
import models.mongodb.PermissionGroup
import models.mongodb.UserAccount
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import security.HandlerKeys

class AccountManagementController @Inject()(
  val messagesApi: MessagesApi,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders
) extends Controller with I18nSupport {
  
  def accounts = deadbolt.Dynamic(name=PermissionGroup.Administrator.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit authRequest =>
      val userAccounts = UserAccount.userAccountCollection.find.toList.map{ accountObj => UserAccount.dboToUserAccount(accountObj) }
      Future(Ok(views.html.account_management(userAccounts)))
  }
}