package controllers.facebook.reporting

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import helpers.facebook.reporting.ReportingControllerHelper._
import models.mongodb.MongoExtensions._
import models.mongodb.facebook.Facebook._
import play.api.i18n.I18nSupport
import play.api.mvc._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future


class ReportingController @Inject()(val controllerComponents: ControllerComponents, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {

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
        facebookReportCollection("unimplemented").find().toList
          .map(_.toJson()).mkString("[", ",", "]")
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
          facebookReportCollection("unimplemented").find(buildQry(campaignId, adGroupId, adId, keywordId, startDate, endDate))
            .toList
        )
      ))
  }
}