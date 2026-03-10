package controllers.msn.api_account.customer.account

import javax.inject.Inject

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.microsoft.bingads.v13.customermanagement.{AccountInfoWithCustomerData, User}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import models.mongodb.PermissionGroup
import models.mongodb.msn.Msn._
import play.api.i18n.I18nSupport
import play.api.mvc._
import security.HandlerKeys

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class AccountInfoController @Inject()(val controllerComponents: ControllerComponents, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {

  def account_infos(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.MSNRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.msn.api_account.customer.account_info.account_infos(
        msnAccountInfoCollection.find().toList.map { x =>
          Tuple2(
            documentToMsnEntity[User](x, "account", Some("user")),
            documentToMsnEntity[AccountInfoWithCustomerData](x, "account", Some("accountInfoWithCustomerData"))
          )
        },
        page,
        pageSize,
        orderBy,
        filter,
        msnAccountInfoCollection.countSync().toInt
      )))
  }
}