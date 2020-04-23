package util.charts.performance

import util.charts.ChartMetaData
import models.mongodb.google.Google._
import models.mongodb.google.GooglePerformance
import models.mongodb.google.GoogleCampaignPerformance
import models.mongodb.google.GoogleAdGroupPerformance
import models.mongodb.google.GoogleAdPerformance
import models.mongodb.google.GoogleGeoPerformance
import models.mongodb.performance.PerformanceEntityFactory
import models.mongodb.performance.PerformanceEntity
import models.mongodb.performance.PerformanceEntityFilter
import models.mongodb.UserAccount
import models.mongodb.SessionSettings
import Shared.Shared._
import be.objectify.deadbolt.scala.AuthenticatedRequest
import play.api.mvc.AnyContent
import models.mongodb.performance.PerformanceField
import models.mongodb.performance.PerformanceField.PerformanceFieldType._
import models.mongodb.google.GoogleAccountPerformance

object GooglePerformanceCharts {
  
  def defaultGoogleMetaData(user: UserAccount, 
    defaultFields: List[PerformanceField],
    filters: List[PerformanceEntityFilter],
    request: AuthenticatedRequest[AnyContent]) = ChartMetaData(
    1, 20, "date", 1, 
    request.path,
    filters,
    List(GooglePerformance.clicksField.fieldName, 
      GooglePerformance.costField.fieldName, 
      GooglePerformance.revenueField.fieldName, 
      GooglePerformance.conversionsField.fieldName,
      GooglePerformance.cRateField.fieldName
    ) ++ defaultFields.map { field => field.fieldName },
    Some(SessionSettings.getSettings(user).chartStartDate),
    Some(SessionSettings.getSettings(user).chartEndDate),
    List(
      PerformanceGraph("ComboChart", 
        List(
          PerformanceEntity.dateField.fieldName, 
          GooglePerformance.conversionsField.fieldName,
          GooglePerformance.cRateField.fieldName
        )
      ),
      PerformanceGraph("LineChart", 
        List(
          PerformanceEntity.dateField.fieldName, 
          GooglePerformance.costField.fieldName,
          GooglePerformance.revenueField.fieldName
        )
      )
    ),
    false
  )
    
  lazy val commonGooglePerformanceChartColumns: List[PerformanceChartColumn] = List(
    // Dimensions
    new PerformanceChartColumn(PerformanceEntity.dateField, "Date"),
    new PerformanceChartColumn(GooglePerformance.deviceField, "Device"),
    new PerformanceChartColumn(GooglePerformance.networkField, "Network"),
    new PerformanceChartColumn(GooglePerformance.clicksField, "Clicks"),
    new PerformanceChartColumn(GooglePerformance.impField, "Impressions"),
    new PerformanceChartColumn(GooglePerformance.interactionField, "Interactions"),
    new PerformanceChartColumn(GooglePerformance.conversionsField, "Conversions").withDecimalPlaces(1),
    new PerformanceChartColumn(GooglePerformance.ctrField, "CTR").withDecimalPlaces(1),
    new PerformanceChartColumn(GooglePerformance.cpcField, "CPC").withNumberFormatPrefix("$").withDecimalPlaces(1),
    new PerformanceChartColumn(GooglePerformance.cpmField, "CPM").withNumberFormatPrefix("$").withDecimalPlaces(2),
    new PerformanceChartColumn(GooglePerformance.costPerConvField, "Cost Per Conv").withNumberFormatPrefix("$").withDecimalPlaces(1),
    new PerformanceChartColumn(GooglePerformance.cRateField, "C-Rate").withNumberFormatSuffix("%").withDecimalPlaces(1),
    new PerformanceChartColumn(GooglePerformance.costField, "Cost").withNumberFormatPrefix("$").withDecimalPlaces(1),
    new PerformanceChartColumn(GooglePerformance.gmailClicksField, "Gmail Clicks"),
    new PerformanceChartColumn(GooglePerformance.gmailForwardsField, "Gmail Forwards"),
    new PerformanceChartColumn(GooglePerformance.gmailSavesField, "Gmail Saves"),
    new PerformanceChartColumn(GooglePerformance.arrivalsField, "Arrivals"),
    new PerformanceChartColumn(GooglePerformance.conuField, "ConU"),
    new PerformanceChartColumn(GooglePerformance.confField, "ConF"),
    new PerformanceChartColumn(GooglePerformance.durationField, "Duration (s)").withDecimalPlaces(1),
    new PerformanceChartColumn(GooglePerformance.revenueField, "Revenue").withNumberFormatPrefix("$"),
    new PerformanceChartColumn(GooglePerformance.lpConvUField, "LP-Conv-U").withDecimalPlaces(1),
    new PerformanceChartColumn(GooglePerformance.uFormCompleteField, "Forms Complete"),
    new PerformanceChartColumn(GooglePerformance.bounceField, "Bounce"),
    new PerformanceChartColumn(GooglePerformance.avgDurationField, "Avg. Duration").withDecimalPlaces(1),
    new PerformanceChartColumn(GooglePerformance.lpcRateField, "LPC-Rate").withNumberFormatSuffix("%").withDecimalPlaces(1),
    new PerformanceChartColumn(GooglePerformance.formArrField, "Forms Per Arrival").withDecimalPlaces(1),
    new PerformanceChartColumn(GooglePerformance.formCRateField, "Forms Per Conv").withDecimalPlaces(1),
    new PerformanceChartColumn(GooglePerformance.vplConUField, "VPL-ConU").withNumberFormatPrefix("$").withDecimalPlaces(1),
    new PerformanceChartColumn(GooglePerformance.costConUField, "Cost-ConU").withNumberFormatPrefix("$").withDecimalPlaces(1),
    new PerformanceChartColumn(GooglePerformance.bounceRateField, "Bounce Rate").withNumberFormatSuffix("%").withDecimalPlaces(1),
    new PerformanceChartColumn(GooglePerformance.costLPConvField, "Cost-LP-Conv").withNumberFormatPrefix("$").withDecimalPlaces(1)
  )
  
  class GoogleAccountPerformanceChart(
    metaData: ChartMetaData
  ) extends PerformanceChart[GoogleAccountPerformance](
    List(
      new PerformanceChartColumn(GoogleAccountPerformance.accountHtmlField, "Account"){
        override def filterField: PerformanceField = GooglePerformance.accountField
      }
    ) ++ commonGooglePerformanceChartColumns,
    metaData,
    PerformanceEntityFactory.createGoogleAccountPerformance,
    googleReportCollection(com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType.CAMPAIGN_PERFORMANCE_REPORT)
  )   
  
  class GoogleCampaignPerformanceChart(
    metaData: ChartMetaData
  ) extends PerformanceChart[GoogleCampaignPerformance](
    List(
      new PerformanceChartColumn(GoogleCampaignPerformance.campaignHtmlField, "Campaign"){
        override def filterField: PerformanceField = GoogleCampaignPerformance.campaignNameField
      },
      new PerformanceChartColumn(GoogleCampaignPerformance.campaignIdField, "Campaign Id").withGroupingSymbol(""),
      new PerformanceChartColumn(GoogleCampaignPerformance.campaignStateField, "Campaign State"),
      new PerformanceChartColumn(GoogleCampaignPerformance.campaignBudgetEditHtmlField, "Budget"){
        override def isFilterable: Boolean = false
      }.withDecimalPlaces(1).withNumberFormatPrefix("$"),
      new PerformanceChartColumn(GooglePerformance.accountField, "Account")
    ) ++ commonGooglePerformanceChartColumns,
    metaData,
    PerformanceEntityFactory.createGoogleCampaignPerformance,
    googleReportCollection(com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType.CAMPAIGN_PERFORMANCE_REPORT)
  ){
    
    override lazy val entities: List[GoogleCampaignPerformance] = {
      entityLoader.withMatchStage(
        dateFilters ++ metaData.chartFilters.filter { filter => filter.field.fieldType == dimension }
      ).withGroupSumStage()
      .withLookup(
          googleCampaignCollection, 
          GoogleCampaignPerformance.campaignIdField, 
          new PerformanceField("apiId", dimension), 
          "campaigns"
      ).withUnwind("$campaigns", true)
      .withUnwind("$campaigns.campaign", true) 
      .withProject()
      .withMatchStage(metaData.chartFilters.filter { filter => filter.field.fieldType == measure })
      .withSortStage(-1)
      .withPaginationStages(metaData.page, metaData.pageSize)
      .execute
    }
    
  }
  
  class GoogleAdGroupPerformanceChart(
    metaData: ChartMetaData
  ) extends PerformanceChart[GoogleAdGroupPerformance](
    List(
      new PerformanceChartColumn(GoogleAdGroupPerformance.adGroupHtmlField, "AdGroup"){
        override def filterField: PerformanceField = GoogleAdGroupPerformance.adGroupNameField
      },
      new PerformanceChartColumn(GoogleAdGroupPerformance.adGroupIdField, "AdGroup Id").withGroupingSymbol(""),
      new PerformanceChartColumn(GoogleAdGroupPerformance.adGroupStateField, "AdGroup State"),
      new PerformanceChartColumn(GoogleAdGroupPerformance.maxCpcHtmlField, "Max Cpc"),
      new PerformanceChartColumn(GoogleCampaignPerformance.campaignNameField, "Campaign"),
      new PerformanceChartColumn(GoogleCampaignPerformance.campaignIdField, "Campaign Id").withGroupingSymbol(""),
      new PerformanceChartColumn(GoogleCampaignPerformance.campaignStateField, "Campaign State"),
      new PerformanceChartColumn(GooglePerformance.accountField, "Account")
    ) ++ commonGooglePerformanceChartColumns,
    metaData,
    PerformanceEntityFactory.createGoogleAdGroupPerformance,
    googleReportCollection(com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType.ADGROUP_PERFORMANCE_REPORT)
  )
  
  class GoogleAdPerformanceChart(
    metaData: ChartMetaData
  ) extends PerformanceChart[GoogleAdPerformance](
    List(
      new PerformanceChartColumn(GoogleAdPerformance.adHtmlField, "Ad"){
        override def filterField: PerformanceField = GoogleAdPerformance.adNameField
      },
      new PerformanceChartColumn(GoogleAdPerformance.adIdField, "Ad Id").withGroupingSymbol(""),
      new PerformanceChartColumn(GoogleAdPerformance.adStateField, "Ad State"),
      new PerformanceChartColumn(GoogleAdPerformance.adTypeField, "Ad Type"),
      new PerformanceChartColumn(GoogleAdPerformance.adDescriptionField, "Description"),
      new PerformanceChartColumn(GoogleAdPerformance.adDescriptionLine1Field, "Description Line 1"),
      new PerformanceChartColumn(GoogleAdPerformance.headline1Field, "Headline 1"),
      new PerformanceChartColumn(GoogleAdPerformance.headline2Field, "Headline 2"),
      new PerformanceChartColumn(GoogleAdPerformance.longHeadlineField, "Long Headline"),
      new PerformanceChartColumn(GoogleAdPerformance.imageAdNameField, "Image Ad Name"),
      new PerformanceChartColumn(GoogleAdPerformance.imageHeightField, "Image Height"),
      new PerformanceChartColumn(GoogleAdPerformance.imageWidthField, "Image Width"),
      new PerformanceChartColumn(GoogleAdPerformance.path1Field, "Path 1"),
      new PerformanceChartColumn(GoogleAdPerformance.path2Field, "Path 2"),
      new PerformanceChartColumn(GoogleAdGroupPerformance.adGroupNameField, "AdGroup"),
      new PerformanceChartColumn(GoogleAdGroupPerformance.adGroupIdField, "AdGroup Id").withGroupingSymbol(""),
      new PerformanceChartColumn(GoogleAdGroupPerformance.adGroupStateField, "AdGroup State"),
      new PerformanceChartColumn(GoogleCampaignPerformance.campaignNameField, "Campaign"),
      new PerformanceChartColumn(GoogleCampaignPerformance.campaignIdField, "Campaign Id").withGroupingSymbol(""),
      new PerformanceChartColumn(GoogleCampaignPerformance.campaignStateField, "Campaign State"),
      new PerformanceChartColumn(GooglePerformance.accountField, "Account")
    ) ++ commonGooglePerformanceChartColumns,
    metaData,
    PerformanceEntityFactory.createGoogleAdPerformance,
    googleReportCollection(com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType.AD_PERFORMANCE_REPORT)
  )
  
  class GoogleGeoPerformanceChart(
    metaData: ChartMetaData
  ) extends PerformanceChart[GoogleGeoPerformance](
    List(
      new PerformanceChartColumn(GoogleGeoPerformance.regionField, "Region").withGroupingSymbol(""),
      new PerformanceChartColumn(GoogleGeoPerformance.cityField, "City"),
      new PerformanceChartColumn(GoogleGeoPerformance.mostSpecificLocationField, "Most Specific Loc"),
      new PerformanceChartColumn(GoogleGeoPerformance.targetableField, "Is Targetable"),
      new PerformanceChartColumn(GoogleGeoPerformance.clientNameField, "Client Name"),
      new PerformanceChartColumn(GoogleGeoPerformance.currencyField, "Currency"),
      new PerformanceChartColumn(GoogleGeoPerformance.countryTerritoryField, "Country/Territory"),
      new PerformanceChartColumn(GoogleGeoPerformance.metroAreaField, "Metro Area"),
      new PerformanceChartColumn(GoogleGeoPerformance.customerIdField, "Customer Id"),
      new PerformanceChartColumn(GoogleAdGroupPerformance.adGroupNameField, "AdGroup"),
      new PerformanceChartColumn(GoogleAdGroupPerformance.adGroupIdField, "AdGroup Id"),
      new PerformanceChartColumn(GoogleAdGroupPerformance.adGroupStateField, "AdGroup State"),
      new PerformanceChartColumn(GoogleCampaignPerformance.campaignNameField, "Campaign"),
      new PerformanceChartColumn(GoogleCampaignPerformance.campaignIdField, "Campaign Id").withGroupingSymbol(""),
      new PerformanceChartColumn(GoogleCampaignPerformance.campaignStateField, "Campaign State"),
      new PerformanceChartColumn(GooglePerformance.accountField, "Account")
    ) ++ commonGooglePerformanceChartColumns,
    metaData,
    PerformanceEntityFactory.createGoogleGeoPerformance,
    googleReportCollection(com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType.GEO_PERFORMANCE_REPORT)
  )
}