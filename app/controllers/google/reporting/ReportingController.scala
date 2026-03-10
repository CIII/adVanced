package controllers.google.reporting

import javax.inject.Inject

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

// TODO: Re-implement with Google Ads API v18 and async MongoDB
class ReportingController @Inject()(val controllerComponents: ControllerComponents, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {

  def json(
    reportType: String,
    campaignId: String,
    adGroupId: String,
    adId: String,
    keywordId: String,
    startDate: String,
    endDate: String
  ) = Action.async { implicit request =>
    Future.successful(Ok(Json.obj("error" -> "Google reporting not yet migrated to async MongoDB")))
  }

  def csv(
    reportType: String,
    campaignId: String,
    adGroupId: String,
    adId: String,
    keywordId: String,
    startDate: String,
    endDate: String
  ) = Action.async { implicit request =>
    Future.successful(Ok("Google reporting not yet migrated to async MongoDB"))
  }
}
