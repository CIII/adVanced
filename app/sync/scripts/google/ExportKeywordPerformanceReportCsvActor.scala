package sync.scripts.google

import java.io.{BufferedWriter, File, FileWriter}

import Shared.Shared._
import akka.actor.Actor
import akka.event.Logging
import com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType
import helpers.google.reporting.ReportingControllerHelper._
import models.mongodb.google.Google._
import org.joda.time.DateTime
import com.mongodb.casbah.Imports._
import sync.scripts.shared.Google._

class ExportKeywordPerformanceReportCsvActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case dateRange: (Option[String], Option[String]) =>
      try {
        val six_months_ago = DateTime.now.minusMonths(6)
        val file_prefix = "google_keyword_performance"
        deleteFiles("/tmp", file_prefix)
        val filename = "/tmp/%s_%s.csv".format(
          file_prefix,
          dateRange._1.getOrElse(googleReportDateFormatter.print(six_months_ago))
        )
        val file = new File(filename)
        val bw = new BufferedWriter(new FileWriter(file))
        bw.write(mongoToCsv(
          googleReportCollection(ReportDefinitionReportType.KEYWORDS_PERFORMANCE_REPORT)
            .find(buildQry("", "", "", "", dateRange._1.getOrElse(""), dateRange._2.getOrElse(""))).toList.distinct
            .sortBy{r => DateTime.parse(r.as[String]("day")).toDate.getTime}.map{ row =>
            DBObject(
              "Date" -> row.as[String]("day"),
              "Source" -> "google",
              "KeywordId" -> row.as[Long]("keyword id"),
              "Cost" -> (if(row.as[String]("cost") == "0") {
                "0.00"
              } else {
                BigDecimal(row.as[String]("cost").toLong / microAmountMultiplier).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
              })
            )
          }
        ))
        bw.close()
      } catch {
        case e: Exception =>
          log.error("Error exporting google keyword performance report CSV - %s".format(e.toString))
      }
  }
}
