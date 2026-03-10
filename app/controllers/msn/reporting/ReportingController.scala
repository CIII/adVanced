package controllers.msn.reporting

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import helpers.msn.reporting.ReportingControllerHelper._
import models.mongodb.msn.Msn._
import models.mongodb.MongoExtensions._
import play.api.i18n.I18nSupport
import play.api.mvc._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ReportingController @Inject()(
  val controllerComponents: ControllerComponents,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders
)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {

  // TODO: msnReportCollection was previously a map from MsnReportType -> MongoCollection.
  // It is now a single MongoCollection[Document]. Filter by reportType field instead.
  private def getReportDocs(reportType: String, qry: Document): List[Document] = {
    val fullQry = qry ++ Document("reportType" -> reportType)
    msnReportCollection.find(fullQry).toList
  }

  // TODO: Implement proper CSV serialisation from MongoDB documents.
  private def mongoToCsv(docs: List[Document]): String = {
    docs.map(_.toJson()).mkString("[", ",", "]")
  }

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
        getReportDocs(reportType, buildQry(campaignId, adGroupId, adId, keywordId, startDate, endDate))
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
          getReportDocs(reportType, buildQry(campaignId, adGroupId, adId, keywordId, startDate, endDate))
        )
      ))
  }
}
