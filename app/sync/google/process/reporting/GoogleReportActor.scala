package sync.google.process.reporting

import Shared.Shared.GoogleReportRequest
import org.apache.pekko.actor.Actor
import org.apache.pekko.event.Logging
import models.mongodb.Utilities._
import sync.google.adwords.ReportHelper.getDateString
import util.charts.ChartCache

class GoogleReportActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case reportRequest: GoogleReportRequest =>
      try {
        log.info("Processing %s -> Account: %s -> Date Range: %s -> Download Format: %s".format(
          reportRequest.reportType,
          reportRequest.accountName,
          reportRequest.dateRange,
          reportRequest.downloadFormat
        ))

        // TODO: Not yet migrated to Google Ads API v18
        // The old AdWords API v201609 ReportHelper and AdWordsHelper are no longer available.
        // Implement report downloading using the Google Ads API v18 ReportService.
        log.info("Not yet migrated to Google Ads API v18 - report downloading is stubbed out")

        log.info(s"FINISHED PROCESSING GOOGLE REPORT - $getDateString")
      } catch {
        case e: Exception =>
          log.error(s"Error processing Google report: ${e.getMessage}")
          log.info("Error Processing Report (%s) - %s".format(
            reportRequest.reportType,
            e.toString
          ))
      } finally {
        ChartCache.clearCache
        log.info("Finished processing report")
        context.stop(self)
      }
  }
}
