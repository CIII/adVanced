package sync.google.adwords.account

import java.text.SimpleDateFormat
import java.util.Date

import akka.event.LoggingAdapter
import com.google.api.ads.adwords.axis.utils.v201609.SelectorBuilder
import com.google.api.ads.adwords.axis.v201609.ch.{CustomerChangeData, CustomerSyncSelector, CustomerSyncServiceInterface}
import com.google.api.ads.adwords.axis.v201609.cm.{Campaign, CampaignPage, CampaignServiceInterface, DateTimeRange, Selector, _}
import com.google.api.ads.adwords.axis.v201609.mcm._
import com.google.api.ads.adwords.lib.selectorfields.v201609.cm.{BiddingStrategyField, BudgetField, ManagedCustomerField}
import com.google.api.ads.adwords.lib.utils.v201609.ReportDownloader
import org.apache.commons.lang.ArrayUtils
import sync.google.adwords.AdWordsHelper

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

/**
 * Helper Class for AdWords Account
 */
class CustomerHelper(AdWords: AdWordsHelper, log: LoggingAdapter) {

  private val campaignService: CampaignServiceInterface = AdWords.adWordsServices.get(AdWords.adWordsSession, classOf[CampaignServiceInterface])
  private val customerSyncService = AdWords.adWordsServices.get(AdWords.adWordsSession, classOf[CustomerSyncServiceInterface])
  private val budgetService = AdWords.adWordsServices.get(AdWords.adWordsSession, classOf[BudgetServiceInterface])
  private val biddingStrategyService = AdWords.adWordsServices.get(AdWords.adWordsSession, classOf[BiddingStrategyServiceInterface])
  private val dateFormatter = new SimpleDateFormat("yyyyMMdd HHmmss")


  def getCustomerChanges: Option[CustomerChangeData] = {
    val campaignIds = List[java.lang.Long]()
    val selector: Selector = new SelectorBuilder()
      .fields(ManagedCustomerField.CustomerId)
      .build()
    val campaigns: CampaignPage = campaignService.get(selector)
    if (campaigns.getEntries != null) {
      for (campaign: Campaign <- campaigns.getEntries) {
        campaignIds :+ campaign.getId
      }
    }

    val dateTimeRange: DateTimeRange = new DateTimeRange()
    dateTimeRange.setMin(dateFormatter.format(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24)))
    dateTimeRange.setMax(dateFormatter.format(new Date()))

    val customerSyncSelector: CustomerSyncSelector = new CustomerSyncSelector()
    customerSyncSelector.setDateTimeRange(dateTimeRange)
    customerSyncSelector.setCampaignIds(ArrayUtils.toPrimitive(campaignIds.toArray))

    val accountChanges: CustomerChangeData = customerSyncService.get(customerSyncSelector)


    if (accountChanges != null && accountChanges.getChangedCampaigns != null) {
      Some(accountChanges)
    } else {
      None
    }
  }

  def getCustomers = AdWords.adWordsServices.get(AdWords.adWordsSession, classOf[CustomerServiceInterface]).getCustomers

  def getManagedCustomers(customer: Customer, fields: List[ManagedCustomerField], offset: Int=0, predicates: Option[Array[Predicate]]=None, sel: Option[Selector]=None): ManagedCustomerPage = {
    AdWords.adWordsSession.setClientCustomerId(customer.getCustomerId.toString)

    val customerService = AdWords.adWordsServices.get(AdWords.adWordsSession, classOf[ManagedCustomerServiceInterface])
    val builder = new SelectorBuilder()
    val paging = new Paging()
    paging.setNumberResults(AdWords.PAGE_SIZE)
    paging.setStartIndex(offset)
    val selector = sel match {
      case Some(s) => s
      case _ =>
        builder
          .offset(offset)
          .fields(fields: _*)
          .limit(AdWords.PAGE_SIZE)
          .build()
    }

    predicates match {
      case Some(p) =>
        selector.setPredicates(p)
      case _ =>
    }
    selector.setPaging(paging)
    val customerPage = customerService.get(selector)
    log.info(s"CLARENCE DEBUG -- MANAGED CUSTOMERS -> ${customerPage.getEntries.toList.map(c => s"${c.getName} - ${c.getCustomerId}").mkString("\n")}")
    customerPage
  }

  def getBudgets(fields: List[BudgetField], offset: Int=0, predicates: Option[Array[Predicate]]=None, sel: Option[Selector]=None): BudgetPage = {
    val paging = new Paging {
      setNumberResults(AdWords.PAGE_SIZE)
      setStartIndex(offset)
    }
    val builder = new SelectorBuilder()
    val selector = sel match {
      case Some(s) => s
      case _ =>
        builder
          .offset(offset)
          .fields(fields: _*)
          .limit(AdWords.PAGE_SIZE)
          .build()
    }

    predicates match {
      case Some(p) =>
        selector.setPredicates(p)
      case _ =>
    }
    selector.setPaging(paging)
    budgetService.get(selector)
  }

  def getSharedBiddingStrategies(fields: List[BiddingStrategyField], offset: Int=0, predicates: Option[Array[Predicate]]=None, sel: Option[Selector]=None): BiddingStrategyPage = {
    val paging = new Paging {
      setNumberResults(AdWords.PAGE_SIZE)
      setStartIndex(offset)
    }
    val builder = new SelectorBuilder()
    val selector = sel match {
      case Some(s) => s
      case _ =>
        builder
          .offset(offset)
          .fields(fields: _*)
          .limit(AdWords.PAGE_SIZE)
          .build()
    }

    predicates match {
      case Some(p) =>
        selector.setPredicates(p)
      case _ =>
    }
    selector.setPaging(paging)
    biddingStrategyService.get(selector)
  }

  def downloadReport(
    reportType: com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType,
    fields: Option[List[String]],
    predicates: Option[List[com.google.api.ads.adwords.lib.jaxb.v201609.Predicate]],
    dateRange: com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionDateRangeType,
    downloadFormat: com.google.api.ads.adwords.lib.jaxb.v201609.DownloadFormat,
    enableZeroImpressions: Boolean = false
  ): Option[String] = {
    var reportFields = fields.getOrElse(List())
    val reportPredicates = predicates.getOrElse(List())
    fields match {
      case None =>
        val reportDefinitionService = AdWords.adWordsServices.get(AdWords.adWordsSession, classOf[ReportDefinitionServiceInterface])
        reportDefinitionService.getReportFields(
          com.google.api.ads.adwords.axis.v201609.cm.ReportDefinitionReportType.fromValue(reportType.value)
        ).foreach(x =>
          x.getFieldName match {
            case (
              "ConversionRateManyPerClickSignificance" |
              "ConversionRateSignificance" |
              "ViewThroughConversionsSignificance" |
              "AdvertiserExperimentSegmentationBin" |
              "ConversionCategoryName" |
              "ConversionTrackerId" |
              "ConversionTypeName" |
              "AveragePageviews" |
              "AverageTimeOnSite" |
              "BounceRate" |
              "ClickAssistedConversions" |
              "ClickAssistedConversionsOverLastClickConversions" |
              "ClickConversionRateSignificance" |
              "ClickAssistedConversionValue" |
              "ClickSignificance" |
              "ImpressionAssistedConversionValue" |
              "ImpressionAssistedConversions" |
              "ImpressionAssistedConversionsOverLastClickConversions" |
              "PercentNewVisitors" |
              "SearchExactMatchImpressionShare" |
              "SearchImpressionShare" |
              "SearchRankLostImpressionShare" |
              "ConversionManyPerClickSignificance" |
              "ConvertedClicksSignificance"
              ) =>
            case _ =>
              reportFields = reportFields :+ x.getFieldName
          })
      case _ =>
    }
    val selector = new com.google.api.ads.adwords.lib.jaxb.v201609.Selector()
    selector.getFields.addAll(reportFields.asJava)
    selector.getPredicates.addAll(reportPredicates.asJava)

    val reportDefinition = new com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinition()
    reportDefinition.setReportName("%s report # %d".format(
      reportType.value,
      System.currentTimeMillis() / 1000
    ))
    reportDefinition.setReportType(reportType)
    reportDefinition.setDownloadFormat(downloadFormat)
    reportDefinition.setDateRangeType(dateRange)
    //reportDefinition.setIncludeZeroImpressions(enableZeroImpressions)
    reportDefinition.setSelector(selector)

    val response = new ReportDownloader(AdWords.adWordsSession).downloadReport(reportDefinition)
    val scanner = new java.util.Scanner(response.getInputStream, "UTF-8").useDelimiter("\\A")
    scanner.hasNext match {
      case true =>
        Some(scanner.next)
      case _ => None
    }
  }
}