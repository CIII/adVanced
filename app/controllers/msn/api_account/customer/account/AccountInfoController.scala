package controllers.msn.api_account.customer.account

import javax.inject.Inject

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.microsoft.bingads.customermanagement.{AccountInfoWithCustomerData, User}
import com.mongodb.casbah.Imports._
import models.mongodb.PermissionGroup
import models.mongodb.msn.Msn._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import security.HandlerKeys

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AccountInfoController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {

  def account_infos(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.MSNRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.msn.api_account.customer.account_info.account_infos(
        msnAccountInfoCollection.find().toList.map { x =>
          Tuple2(
            dboToMsnEntity[User](x, "account", Some("user")),
            dboToMsnEntity[AccountInfoWithCustomerData](x, "account", Some("accountInfoWithCustomerData"))
          )
        },
        page,
        pageSize,
        orderBy,
        filter,
        msnAccountInfoCollection.count()
      )))
  }
}