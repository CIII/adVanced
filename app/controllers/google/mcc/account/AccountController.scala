package controllers.google.mcc.account

import javax.inject.Inject

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.google.api.ads.adwords.axis.v201609.mcm.Customer
import com.mongodb.casbah.Imports._
import models.mongodb.PermissionGroup
import models.mongodb.google.Google._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import security.HandlerKeys
import util.charts._
import util.charts.client.ChartColumn
import util.charts.client.ChartColumn.ColumnDataType._
import util.charts.client.ChartColumn.ColumnType._
import util.charts.ChartMetaData._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import util.charts.performance.GooglePerformanceCharts._
import models.mongodb.google.GoogleAccountPerformance

class AccountController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {

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
        googleCustomerCollection
      )))
  }

  def accounts = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.accounts(
        new ClientChart(
          List(
            new ChartColumn("customerId", "", "Customer Id", number, dimension),
            new ChartColumn("companyName", "", "Company Name", string, dimension),
            new ChartColumn("descriptiveName", "", "Account Name", string, dimension),
            new ChartColumn("currencyCode", "", "Currency Code", string, dimension)
          ),
          googleCustomerCollection.find().toList.map(
            dboToGoogleEntity[Customer](_, "customer", None)
          )
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
