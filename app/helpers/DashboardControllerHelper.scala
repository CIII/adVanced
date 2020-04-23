package helpers

import models.mongodb.SessionSettings
import models.mongodb.UserAccount
import models.mongodb.lynx.TQReporting
import helpers.google.mcc.account.campaign.CampaignControllerHelper
import helpers.google.mcc.account.campaign.CampaignControllerHelper._
import be.objectify.deadbolt.scala.AuthenticatedRequest
import play.api.mvc.AnyContent
import models.mongodb.google.Google._
import org.joda.time.DateTime
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports.DBObject
import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag
import util.charts.performance.PerformanceChart
import models.mongodb.performance.PerformanceEntity._
import util.charts.ChartMetaData
import models.mongodb.lynx.TQReportingPerformance
import models.mongodb.performance.PerformanceEntityFactory
import util.charts.performance.PerformanceChartColumn
import models.mongodb.performance.PerformanceEntity
import models.mongodb.performance.PerformanceEntityFilter
import util.charts.performance.PerformanceGraph
import util.charts.ChartData
import models.mongodb.performance.PerformanceEntityLoader
import models.mongodb.google.GoogleCampaignPerformance
import models.mongodb.google.GooglePerformance
import play.api.Logger

object DashboardControllerHelper {
  
  def dashboardMetaData(user: UserAccount): ChartMetaData = ChartMetaData(
    1, 1000, TQReportingPerformance.trafficSourceField.fieldName, -1, "", 
    List(PerformanceEntityFilter(
      TQReportingPerformance.trafficSourceField, 
      "in", 
      List("facebook", "google", "bing", null)
    )),
    List(TQReportingPerformance.trafficSourceField.fieldName, 
      TQReportingPerformance.arrivalsField.fieldName,
      TQReportingPerformance.revenueField.fieldName, 
      TQReportingPerformance.conversionsField.fieldName, 
      TQReportingPerformance.cRateField.fieldName, 
      TQReportingPerformance.clicksField.fieldName,
      TQReportingPerformance.costField.fieldName,
      TQReportingPerformance.conUField.fieldName, 
      TQReportingPerformance.lpConvUField.fieldName, 
      TQReportingPerformance.lpCRateField.fieldName,
      TQReportingPerformance.vplConUField.fieldName,
      TQReportingPerformance.costConUField.fieldName
    ),
    Some(SessionSettings.getSettings(user).chartStartDate),
    Some(SessionSettings.getSettings(user).chartEndDate),
    List(
      PerformanceGraph("ComboChart", 
        List(
          TQReportingPerformance.createdAtField.fieldName,
          TQReportingPerformance.conversionsField.fieldName,
          TQReportingPerformance.cRateField.fieldName
        )
      ),
      PerformanceGraph("LineChart", 
        List( 
          TQReportingPerformance.createdAtField.fieldName,
          TQReportingPerformance.revenueField.fieldName,
          TQReportingPerformance.costField.fieldName
        )
      )
    ),
    false
  )
  
  class DashboardPerformanceChart(
    metaData: ChartMetaData
  ) extends PerformanceChart[TQReportingPerformance](
    List(
      new PerformanceChartColumn(TQReportingPerformance.createdAtField, "Date"),
      new PerformanceChartColumn(TQReportingPerformance.trafficSourceField, "Traffic Source"),
      new PerformanceChartColumn(TQReportingPerformance.arrivalsField, "Arrivals"),
      new PerformanceChartColumn(TQReportingPerformance.revenueField, "Revenue").withDecimalPlaces(1).withNumberFormatPrefix("$"),
      new PerformanceChartColumn(TQReportingPerformance.pageLoadsField, "Page Loads"),
      new PerformanceChartColumn(TQReportingPerformance.conversionsField, "Conversions").withDecimalPlaces(1),
      new PerformanceChartColumn(TQReportingPerformance.cRateField, "C-Rate").withDecimalPlaces(1).withNumberFormatSuffix("%"),
      new PerformanceChartColumn(TQReportingPerformance.conUField, "ConU"),
      new PerformanceChartColumn(TQReportingPerformance.lpConvUField, "LP-Conv-U").withDecimalPlaces(1),
      new PerformanceChartColumn(TQReportingPerformance.lpCRateField, "LP-C-Rate").withDecimalPlaces(1).withNumberFormatSuffix("%"),
      new PerformanceChartColumn(TQReportingPerformance.vplConUField, "VPL-ConU").withDecimalPlaces(1).withNumberFormatSuffix("%"),
      new PerformanceChartColumn(TQReportingPerformance.costField, "Cost").withDecimalPlaces(1).withNumberFormatPrefix("$"),
      new PerformanceChartColumn(TQReportingPerformance.clicksField, "Clicks"),
      new PerformanceChartColumn(TQReportingPerformance.costConUField, "Cost-ConU").withDecimalPlaces(1).withNumberFormatPrefix("$")
    ),
    metaData,
    PerformanceEntityFactory.createTQReportingPerformance,
    TQReporting.arrivalFactCollection
  ){
    override val dateField = TQReportingPerformance.createdAtField
    val googleData: List[GoogleCampaignPerformance] = {
      val dataLoader = new PerformanceEntityLoader[GoogleCampaignPerformance](
        List(
          PerformanceEntity.dateField,
          GooglePerformance.costField,
          GooglePerformance.clicksField
        ),
        googleReportCollection(com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType.CAMPAIGN_PERFORMANCE_REPORT),
        PerformanceEntityFactory.createGoogleCampaignPerformance
      )
      
      dataLoader.withMatchStage(List(
        PerformanceEntityFilter(PerformanceEntity.dateField, "gte", List(metaData.startDate.get.toString(dtf))),
        PerformanceEntityFilter(PerformanceEntity.dateField, "lte", List(metaData.endDate.get.toString(dtf)))
      )).withGroupSumStage()
      .withProject()
      .execute
    }
    
    lazy val totalGoogleCost: Double = googleData.foldLeft[Double](0.0)((num, entity) => entity.cost + num)
    lazy val totalGoogleClicks: Int = googleData.foldLeft[Int](0)((num, entity) => entity.clicks + num)
    lazy val googleDataDateMap: Map[String, GoogleCampaignPerformance] = {
      var dateMap: Map[String, GoogleCampaignPerformance] = Map()
      googleData.map { 
        campaign => dateMap += (campaign.date.toString(dtf) -> campaign) 
      }
      
      dateMap
    }
    
    /**
     * Update the entities with the external data that we pulled in from Google
     */
    entities.foreach { 
      entity => entity.trafficSource match {
        case "google" => 
          entity.cost(totalGoogleCost)
          entity.clicks(totalGoogleClicks)
          entity.costConU(entity.costConU)
        case _ =>
          entity.cost(0.0)
          entity.clicks(0)
          entity.costConU(0)
      }
    }   
    
    /**
     * Update each of the entities for the graph with the data we pulled in from google.  Once
     * we have other traffic sources, we'll want to sum the data from each 
     */
    override def getGraphEntities(graph: PerformanceGraph): List[TQReportingPerformance] = {
      val entities: List[TQReportingPerformance] = super.getGraphEntities(graph)
      entities.foreach { 
        entity =>
          val entityDateStr = entity.createdAt.toString(dtf)
          if(graph.fieldNames.contains(TQReportingPerformance.costField.fieldName) || 
              graph.fieldNames.contains(TQReportingPerformance.costConUField.fieldName)){
            if(googleDataDateMap.contains(entityDateStr)){
              entity.cost(googleDataDateMap(entityDateStr).cost)
            } else {
              entity.cost(0.0)
            }
          }
          
          if(graph.fieldNames.contains(TQReportingPerformance.clicksField.fieldName)){
            if(googleDataDateMap.contains(entityDateStr)){
              entity.clicks(googleDataDateMap(entityDateStr).clicks)
            } else {
              entity.clicks(0)
            }
          }
          
          if(graph.fieldNames.contains(TQReportingPerformance.costConUField.fieldName)){
            if(googleDataDateMap.contains(entityDateStr)){
              entity.costConU(entity.costConU)
            } else {
              entity.costConU(0.0)
            }
          }
      }
      
      entities
    }
  }
}