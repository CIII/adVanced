package controllers.yahoo.reporting

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.mongodb.casbah.Imports._
import helpers.yahoo.reporting.ReportingControllerHelper._
import models.mongodb.yahoo.Yahoo._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReportingController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {

  def json(
    reportType: String,
    advertiserId: String,
    campaignId: String,
    adGroupId: String,
    adId: String,
    keywordId: String,
    startDate: String,
    endDate: String
  ) = Action.async {
    implicit request =>
      Future(Ok(
        com.mongodb.util.JSON.serialize(
          MongoDBList(
            yahooReportCollection(GeminiReportType.withName(reportType.toLowerCase)).find(buildQry(advertiserId, campaignId, adGroupId, adId, keywordId, startDate, endDate)).toList: _*
          )
        )
      ))
  }

  def csv(
    reportType: String,
    advertiserId: String,
    campaignId: String,
    adGroupId: String,
    adId: String,
    keywordId: String,
    startDate: String,
    endDate: String
  ) = Action.async {
    implicit request =>
      Future(Ok(
        mongoToCsv(
          yahooReportCollection(GeminiReportType.withName(reportType)).find(buildQry(advertiserId, campaignId, adGroupId, adId, keywordId, startDate, endDate))
            .toList
        )
      ))
  }
}