package controllers.google.mcc.account.campaign.adgroup.ad

import javax.inject.Inject
import play.api.i18n.MessagesApi
import be.objectify.deadbolt.scala.DeadboltActions
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.ActionBuilders
import play.api.mvc.Controller
import play.api.i18n.I18nSupport
import scala.concurrent.Future
import models.mongodb._
import security.HandlerKeys
import scala.concurrent.ExecutionContext.Implicits.global
import util.charts.performance.GooglePerformanceCharts._
import util.charts.ChartMetaData.getMetaData
import models.mongodb.google.GoogleAdPerformance
import models.mongodb.google.GooglePerformance
import models.mongodb.google.GoogleAdGroupPerformance
import models.mongodb.performance.PerformanceEntityFilter

class AdController @Inject()(
  val messagesApi: MessagesApi,  
  deadbolt: DeadboltActions,  
  handlers: HandlerCache,  
  actionBuilder: ActionBuilders
) extends Controller with I18nSupport {
  
  def attribution = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.campaign.adgroup.ad.ad_attribution(
        new GoogleAdPerformanceChart(
          getMetaData(
            request, 
            List(
              GoogleAdPerformance.adHtmlField, 
              GoogleAdPerformance.adDescriptionField,
              GoogleAdPerformance.adStateField,
              GooglePerformance.avgPosField
            ),
            request.getQueryString("filterById") match {
              case Some(id) => 
                List(new PerformanceEntityFilter(GoogleAdGroupPerformance.adGroupIdField, "eq", List(id.toLong)))
              case _ => List()
            },
            defaultGoogleMetaData
          )
        )
      )))
  }
  
  def attributionCSV = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future.successful(Ok.sendFile(
          new GoogleAdPerformanceChart(
          getMetaData(
            request, 
            List(GoogleAdPerformance.adHtmlField, GoogleAdPerformance.adDescriptionField), 
            request.getQueryString("filterById") match {
              case Some(id) => 
                List(new PerformanceEntityFilter(GoogleAdGroupPerformance.adGroupIdField, "eq", List(id.toLong)))
              case _ => List()
            },
            defaultGoogleMetaData
          )
        ).exportCsv("AdAttribution.csv")
      ))
  }
}