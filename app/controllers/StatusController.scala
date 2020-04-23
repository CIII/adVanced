package controllers

import javax.inject.Inject

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

class StatusController @Inject()(
  val messagesApi: MessagesApi,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders
) extends Controller with I18nSupport {
  def alerts = WebSocket.tryAccept[String] { implicit request =>
    sync.status.Status.attach(request.session.get(Security.username).get)
  }
}
