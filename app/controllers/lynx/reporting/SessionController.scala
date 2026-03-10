package controllers.lynx.reporting

import javax.inject.Inject

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.PermissionGroup
import models.mongodb.MongoExtensions._
import models.mongodb.lynx.TQReporting
import play.api.i18n.I18nSupport
import play.api.mvc._
import security.HandlerKeys
import scala.concurrent.ExecutionContext

import scala.concurrent.Future

class SessionController @Inject()(val controllerComponents: ControllerComponents, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {
  def json = Action.async {
    implicit request =>
      val sessions = TQReporting.arrivalFactCollection.find(Document()).toList
      Future(Ok(sessions.map(_.toJson()).mkString("[", ",", "]")))
  }

  def session(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name=PermissionGroup.LynxRead.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.lynx.session.sessions(
        TQReporting.arrivalFactCollection.find().skip(page * pageSize).limit(pageSize).toList.map(TQReporting.documentToArrivalFact),
        page,
        pageSize,
        orderBy,
        filter,
        TQReporting.arrivalFactCollection.countSync().toInt
      )))
  }
}