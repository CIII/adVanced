package sync.scripts.yahoo

import java.io.{BufferedWriter, File, FileWriter}

import Shared.Shared._
import akka.actor.Actor
import akka.event.Logging
import helpers.yahoo.reporting.ReportingControllerHelper._
import models.mongodb.yahoo.Yahoo._
import org.joda.time.DateTime
import com.mongodb.casbah.Imports._

class ExportKeywordStatsCsvActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case dateRange: (Option[String], Option[String]) =>
      try {
        val six_months_ago = DateTime.now.minusMonths(6)
        val file_prefix = "yahoo_keyword_stats"
        deleteFiles("/tmp", file_prefix)
        val filename = "/tmp/%s_%s.csv".format(
          file_prefix,
          dateRange._1.getOrElse(yahooReportDateFormatter.print(six_months_ago))
        )
        val file = new File(filename)
        val bw = new BufferedWriter(new FileWriter(file))
        bw.write(mongoToCsv(
          yahooReportCollection(GeminiReportType.keyword_stats)
            .find(buildQry("", "", "", "", "", dateRange._1.getOrElse(""), dateRange._2.getOrElse(""))).toList.distinct
            .sortBy{r => DateTime.parse(r.as[String]("day")).toDate.getTime}.map{ row =>
            DBObject(
              "Date" -> row.as[String]("day"),
              "Source" -> "yahoo",
              "KeywordId" -> row.as[String]("keyword id"),
              "Cost" -> row.as[Double]("spend")
            )
          }
        ))
        bw.close()
      } catch {
        case e: Exception =>
          log.error("Error exporting yahoo keyword stats report CSV - %s".format(e.toString))
      }
  }
}
