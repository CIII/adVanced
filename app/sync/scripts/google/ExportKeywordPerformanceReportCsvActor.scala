package sync.scripts.google

import Shared.Shared._
import org.apache.pekko.actor.Actor
import org.apache.pekko.event.Logging
import org.joda.time.DateTime

/**
 * TODO: Re-implement with Google Ads API v18 reporting.
 * The old ReportDefinitionReportType/ReportDownloader have been removed.
 */
class ExportKeywordPerformanceReportCsvActor extends Actor {
  val log = Logging(context.system, this)

  def receive: Receive = {
    case dateRange: (Option[String], Option[String]) =>
      try {
        val sixMonthsAgo = DateTime.now.minusMonths(6)
        val filePrefix = "google_keyword_performance"
        deleteFiles("/tmp", filePrefix)
        // TODO: Re-implement when Google Ads API v18 reporting is integrated
        log.info("ExportKeywordPerformanceReportCsvActor not yet migrated to Google Ads API v18")
      } catch {
        case e: Exception =>
          log.error("Error exporting google keyword performance report CSV - {}", e.toString)
      }
  }
}
