package controllers.yahoo.api_account.advertiser.campaign.targeting_attribute

import javax.inject.Inject

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller

class TargetingAttributeController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {
}
