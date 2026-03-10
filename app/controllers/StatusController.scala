package controllers

import javax.inject.Inject

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import org.apache.pekko.stream.scaladsl.Flow
import play.api.i18n.I18nSupport
import play.api.mvc._

class StatusController @Inject()(
  val controllerComponents: ControllerComponents,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders
) extends BaseController with I18nSupport {
  // TODO: Implement with Pekko Streams when status broadcasting is re-enabled
  def alerts = WebSocket.accept[String, String] { _ =>
    Flow[String].map(_ => "{}")
  }
}
