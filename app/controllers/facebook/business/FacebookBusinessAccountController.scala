package controllers.facebook.business

import play.api.mvc.Controller
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.google.inject.Inject
import play.api.i18n.MessagesApi
import play.api.i18n.I18nSupport
import models.mongodb.PermissionGroup
import security.HandlerKeys
import scala.concurrent.Future
import play.api.mvc.Results._
import scala.concurrent.ExecutionContext.Implicits.global
import models.mongodb._
import models.mongodb.google.Google._
import Shared.Shared._
import models.mongodb.facebook.Facebook._

class FacebookBusinessAccountController @Inject()(
    val messagesApi: MessagesApi, 
    deadbolt: DeadboltActions, 
    handlers: HandlerCache, 
    actionBuilder: ActionBuilders
) extends Controller with I18nSupport {
  
  def businessAccount(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name=PermissionGroup.FacebookRead.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.facebook.business.business_account(
        facebookBusinessAccountCollection.find().skip(page * pageSize).limit(pageSize).toList.map(FacebookBusinessAccount.fromDBO),
        page,
        pageSize,
        orderBy,
        filter,
        facebookBusinessAccountCollection.count(),
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.FACEBOOK && x.changeCategory == ChangeCategory.AD_STUDY
          )
      )))
  }
}