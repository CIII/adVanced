package controllers.google.mcc.account

import javax.inject.Inject

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import models.mongodb.PermissionGroup
import models.mongodb.google.Google._
import play.api.i18n.I18nSupport
import play.api.mvc._
import security.HandlerKeys
import util.charts._
import util.charts.client.ChartColumn
import util.charts.client.ChartColumn.ColumnDataType._
import util.charts.client.ChartColumn.ColumnType._
import util.charts.ChartMetaData._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import util.charts.performance.GooglePerformanceCharts._
import models.mongodb.google.GoogleAccountPerformance

class AccountController @Inject()(val controllerComponents: ControllerComponents, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {

  def json = Action.async {
    implicit request =>
      Future(Ok(controllers.json(
        request,
        List(
          "mccObjId",
          "mccApiId",
          "customerObjId",
          "customerApiId",
          "tsecs"
        ),
        "customer",
        googleCustomerCollection.namespace.getCollectionName
      )))
  }

  def accounts = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Customer] with Document-based access
      val customerDocs = googleCustomerCollection.find().toList
      Future(Ok(views.html.google.mcc.account.accounts(
        new ClientChart(
          List(
            new ChartColumn("customerId", "", "Customer Id", number, dimension),
            new ChartColumn("companyName", "", "Company Name", string, dimension),
            new ChartColumn("descriptiveName", "", "Account Name", string, dimension),
            new ChartColumn("currencyCode", "", "Currency Code", string, dimension)
          ),
          customerDocs
        )
      )
    ))
  }

  def attribution = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.account_attribution(
        new GoogleAccountPerformanceChart(
          getMetaData(
            request,
            List(GoogleAccountPerformance.accountHtmlField),
            List(),
            defaultGoogleMetaData
          )
        )
      )))
  }

  def attributionCSV = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future.successful(Ok.sendFile(
          new GoogleAccountPerformanceChart(
          getMetaData(
            request,
            List(GoogleAccountPerformance.accountHtmlField),
            List(),
            defaultGoogleMetaData
          )
        ).exportCsv("AccountAttribution.csv")
      ))
  }
}
