package controllers.lynx.reporting

import javax.inject.Inject

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.mongodb.casbah.Imports.DBObject
import models.mongodb.PermissionGroup
import models.mongodb.lynx.TQReporting
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import security.HandlerKeys
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class SessionController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {
  def json = Action.async {
    implicit request =>
      val sessions = TQReporting.arrivalFactCollection.find(DBObject()).toList
      Future(Ok(com.mongodb.util.JSON.serialize(sessions.toString())))
  }

  def session(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name=PermissionGroup.LynxRead.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.lynx.session.sessions(
        TQReporting.arrivalFactCollection.find().skip(page * pageSize).limit(pageSize).toList.map(TQReporting.dboToArrivalFact),
        page,
        pageSize,
        orderBy,
        filter,
        TQReporting.arrivalFactCollection.count()
      )))
  }
}