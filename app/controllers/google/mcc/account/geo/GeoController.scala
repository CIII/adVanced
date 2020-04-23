package controllers.google.mcc.account.geo

import javax.inject.Inject

import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import be.objectify.deadbolt.scala.cache.HandlerCache
import models.mongodb.PermissionGroup
import models.mongodb.google.GoogleGeoPerformance
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import security.HandlerKeys
import util.charts.ChartMetaData._
import util.charts.performance.GooglePerformanceCharts._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import models.mongodb.performance.PerformanceEntityFilter

class GeoController @Inject()(
  val messagesApi: MessagesApi,  
  deadbolt: DeadboltActions,  
  handlers: HandlerCache,  
  actionBuilder: ActionBuilders
) extends Controller with I18nSupport {
  
  def attribution = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.geo.geo_attribution(
        new GoogleGeoPerformanceChart(
          getMetaData(
            request, 
            List(GoogleGeoPerformance.regionField),
            List(),
            defaultGoogleMetaData
          )
        )
      )))
  }
  
  def attributionCSV = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future.successful(Ok.sendFile(
        new GoogleGeoPerformanceChart(
          getMetaData(
            request, 
            List(GoogleGeoPerformance.regionField),
            List(),
            defaultGoogleMetaData
          )
        ).exportCsv("GeoAttribution.csv")
      ))
  }
}