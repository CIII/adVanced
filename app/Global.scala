import Shared.Shared._
import play.api.{Application, GlobalSettings}
import scala.concurrent.duration._

object Global extends GlobalSettings {
  override def onStart(app: Application) = {
    googleReportingDaemon(
      reportType = com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType.CAMPAIGN_PERFORMANCE_REPORT,
      fields = Some(List(
        "AdNetworkType2",
        "AverageCost",
        "AverageCpc",
        "AverageCpm",
        "AveragePosition",
        "CampaignId",
        "CampaignName",
        "CampaignStatus",
        "Clicks",
        "Conversions",
        "Cost",
        "CostPerConversion",
        "CrossDeviceConversions",
        "Date",
        "DayOfWeek",
        "Device",
        "GmailForwards",
        "GmailSaves",
        "GmailSecondaryClicks",
        "Impressions",
        "InteractionRate",
        "Interactions",
        "Labels",
        "ViewThroughConversions"
      )),
      enableZeroImpressions = true,
      initial_delay = 0 hours,
      interval = 24 hours
    )
    
    googleReportingDaemon(
      reportType = com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType.KEYWORDS_PERFORMANCE_REPORT,
      fields = Some(List(
        "Date",
        "Id",
        "AdGroupId",
        "AdGroupName",
        "ApprovalStatus",
        "AveragePosition",
        "CampaignId",
        "CampaignName",
        "Clicks",
        "Cost",
        "Device",
        "FirstPageCpc",
        "Impressions",
        "KeywordMatchType",
        "Criteria",
        "CriteriaDestinationUrl",
        "CpcBid",
        "CpmBid",
        "QualityScore",
        "DayOfWeek",
        "AdNetworkType2"
      )),
      enableZeroImpressions = true,
      initial_delay = 0 hours,
      interval = 24 hours
    )

		googleReportingDaemon(
      reportType = com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType.GEO_PERFORMANCE_REPORT,
      fields = Some(List(
        "AdGroupId",
        "AdGroupName",
        "AdGroupStatus",
        "AdNetworkType2",
        "AverageCost",
        "AverageCpc",
        "AverageCpm",
        "AveragePosition",
        "CampaignId",
        "CampaignName",
        "CampaignStatus",
        "Clicks",
        "Conversions",
        "Cost",
        "CostPerConversion",
        "CrossDeviceConversions",
        "Date",
        "DayOfWeek",
        "Device",
        "Impressions",
        "InteractionRate",
        "Interactions",
        "ViewThroughConversions",
        "AccountCurrencyCode",
        "AccountDescriptiveName",
        "CityCriteriaId",
        "CountryCriteriaId",
        "CustomerDescriptiveName",
        "ExternalCustomerId",
        "IsTargetingLocation",
        "MetroCriteriaId",
        "MostSpecificCriteriaId",
        "RegionCriteriaId"
      )),
      enableZeroImpressions = true,
      initial_delay = 0 hours,
      interval = 24 hours
    )
    
    googleReportingDaemon(
      reportType = com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType.ADGROUP_PERFORMANCE_REPORT,
      fields = Some(List(
        "AdGroupId",
        "AdGroupName",
        "AdGroupStatus",
        "AdNetworkType2",
        "AverageCost",
        "AverageCpc",
        "AverageCpm",
        "AveragePosition",
        "CampaignId",
        "CampaignName",
        "CampaignStatus",
        "Clicks",
        "Conversions",
        "Cost",
        "CostPerConversion",
        "CrossDeviceConversions",
        "Date",
        "DayOfWeek",
        "Device",
        "GmailForwards",
        "GmailSaves",
        "GmailSecondaryClicks",
        "Impressions",
        "InteractionRate",
        "Interactions",
        "Labels",
        "ViewThroughConversions"
      )),
      enableZeroImpressions = true,
      initial_delay = 0 hours,
      interval = 24 hours
    )
    
    googleReportingDaemon(
      reportType = com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType.AD_PERFORMANCE_REPORT,
      fields = Some(List(
        "AdGroupId",
        "AdGroupName",
        "AdGroupStatus",
        "AdNetworkType2",
        "AdType",
        "AverageCost",
        "AverageCpc",
        "AverageCpm",
        "AveragePosition",
        "CampaignId",
        "CampaignName",
        "CampaignStatus",
        "Clicks",
        "Conversions",
        "Cost",
        "CostPerConversion",
        "CrossDeviceConversions",
        "Date",
        "DayOfWeek",
        "Description",
        "Description1",
        "Description2",
        "Device",
        "GmailForwards",
        "GmailSaves",
        "GmailSecondaryClicks",
        "Headline",
        "HeadlinePart1",
        "HeadlinePart2",
        "Id",
        "ImageCreativeImageHeight",
        "ImageCreativeImageWidth",
        "ImageCreativeName",
        "Impressions",
        "InteractionRate",
        "Interactions",
        "Labels",
        "LongHeadline",
        "Path1",
        "Path2",
        "Status",
        "ViewThroughConversions"
      )),
      enableZeroImpressions = true,
      initial_delay = 0 hours,
      interval = 24 hours
    )
    
    List(
      GeminiReportType.ad_extension_details,
      GeminiReportType.adjustment_stats,
      GeminiReportType.audience,
      GeminiReportType.conversion_rules_stats,
      GeminiReportType.keyword_stats,
      GeminiReportType.performance_stats,
      GeminiReportType.search_stats
    ).foreach { reportType =>
      yahooReportingDaemon(
        reportType,
        GeminiReportFieldMapping(reportType),
        initial_delay = 0 hours,
        interval = 24 hours
      )
    }
    msnReportingDaemon(MsnReportType.KeywordPerformanceReportRequest, initial_delay=0 hours, interval=24 hours)
    
    lynxReportingDaemon(LynxReportType.TQReportingArrivalFactsReportRequest, initial_delay=0 hours, interval=72 hours)

    googleManagementDaemon(initial_delay=0 hours, interval=24 hours)

    facebookBusinessDaemon(initial_delay=0 hours, interval=24 hours)

    createAdminRecoveryAccountIfNecessary
  }
}