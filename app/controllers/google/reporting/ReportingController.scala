package controllers.google.reporting

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType
import com.mongodb.casbah.Imports._
import helpers.google.reporting.ReportingControllerHelper._
import models.mongodb.google.Google._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReportingController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {

  def json(
    reportType: String,
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
            googleReportCollection(ReportDefinitionReportType.fromValue(reportType.toUpperCase)).find(buildQry(campaignId, adGroupId, adId, keywordId, startDate, endDate)).toList: _*
          )
        )
      ))
  }

  def csv(
    reportType: String,
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
          googleReportCollection(ReportDefinitionReportType.fromValue(reportType)).find(buildQry(campaignId, adGroupId, adId, keywordId, startDate, endDate))
            .toList
        )
      ))
  }
}