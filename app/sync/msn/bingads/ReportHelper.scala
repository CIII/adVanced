package sync.msn.bingads

import Shared.Shared.MsnReportRequest
import com.microsoft.bingads.reporting._

object ReportHelper {

  def getKeywordPerformanceReportRequest(msnReportRequest: MsnReportRequest, bingAdsHelper: BingAdsHelper): KeywordPerformanceReportRequest = {
    val report = new KeywordPerformanceReportRequest
    report.setFormat(ReportFormat.CSV)
    report.setReportName("Keyword Performance Report Request")
    report.setReturnOnlyCompleteData(false)

    val accountIds = new ArrayOflong()
    accountIds.getLongs.add(bingAdsHelper.authData.getAccountId)

    val accountThroughAdGroupReportScope = new AccountThroughAdGroupReportScope
    accountThroughAdGroupReportScope.setAccountIds(accountIds)
    report.setScope(accountThroughAdGroupReportScope)
    report.getScope.setCampaigns(null)
    report.getScope.setAdGroups(null)

    val reportTime = new ReportTime
    val startDate = new com.microsoft.bingads.reporting.Date
    startDate.setDay(msnReportRequest.startDate.getDayOfMonth)
    startDate.setMonth(msnReportRequest.startDate.getMonthOfYear)
    startDate.setYear(msnReportRequest.startDate.getYear)

    val endDate = new com.microsoft.bingads.reporting.Date
    endDate.setDay(msnReportRequest.startDate.getDayOfMonth)
    endDate.setMonth(msnReportRequest.startDate.getMonthOfYear)
    endDate.setYear(msnReportRequest.startDate.getYear)

    reportTime.setCustomDateRangeStart(startDate)
    reportTime.setCustomDateRangeEnd(endDate)
    report.setTime(reportTime)

    val keywordPerformanceReportColumns = new ArrayOfKeywordPerformanceReportColumn()
    keywordPerformanceReportColumns.getKeywordPerformanceReportColumns.add(KeywordPerformanceReportColumn.TIME_PERIOD)
    keywordPerformanceReportColumns.getKeywordPerformanceReportColumns.add(KeywordPerformanceReportColumn.ACCOUNT_ID)
    keywordPerformanceReportColumns.getKeywordPerformanceReportColumns.add(KeywordPerformanceReportColumn.CAMPAIGN_ID)
    keywordPerformanceReportColumns.getKeywordPerformanceReportColumns.add(KeywordPerformanceReportColumn.KEYWORD)
    keywordPerformanceReportColumns.getKeywordPerformanceReportColumns.add(KeywordPerformanceReportColumn.KEYWORD_ID)
    keywordPerformanceReportColumns.getKeywordPerformanceReportColumns.add(KeywordPerformanceReportColumn.DEVICE_TYPE)
    keywordPerformanceReportColumns.getKeywordPerformanceReportColumns.add(KeywordPerformanceReportColumn.BID_MATCH_TYPE)
    keywordPerformanceReportColumns.getKeywordPerformanceReportColumns.add(KeywordPerformanceReportColumn.CLICKS)
    keywordPerformanceReportColumns.getKeywordPerformanceReportColumns.add(KeywordPerformanceReportColumn.IMPRESSIONS)
    keywordPerformanceReportColumns.getKeywordPerformanceReportColumns.add(KeywordPerformanceReportColumn.CTR)
    keywordPerformanceReportColumns.getKeywordPerformanceReportColumns.add(KeywordPerformanceReportColumn.AVERAGE_CPC)
    keywordPerformanceReportColumns.getKeywordPerformanceReportColumns.add(KeywordPerformanceReportColumn.SPEND)
    keywordPerformanceReportColumns.getKeywordPerformanceReportColumns.add(KeywordPerformanceReportColumn.QUALITY_SCORE)

    report.setAggregation(ReportAggregation.DAILY)

    report.setColumns(keywordPerformanceReportColumns)

    report
  }
}
