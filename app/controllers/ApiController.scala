package controllers

import javax.inject.Inject

import api.TokenAuthentication
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

class ApiController @Inject()(
 val messagesApi: MessagesApi,
 deadbolt: DeadboltActions,
 handler: HandlerCache,
 actionBuilder: ActionBuilders
) extends Controller with I18nSupport with TokenAuthentication {
  def ping = withApiToken {
    token => { request =>
      Ok("Valid Token found!")
    }
  }
}