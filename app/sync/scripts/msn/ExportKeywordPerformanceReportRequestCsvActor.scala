package sync.scripts.msn

import java.io.{FileWriter, BufferedWriter, File}

import Shared.Shared._
import org.apache.pekko.actor.Actor
import org.apache.pekko.event.Logging
import helpers.msn.reporting.ReportingControllerHelper._
import helpers.google.reporting.ReportingControllerHelper.googleReportDateFormatter
import models.mongodb.MongoExtensions._
import models.mongodb.msn.Msn._
import models.mongodb.facebook.Facebook.mongoToCsv
import org.joda.time.DateTime
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import org.joda.time.format.DateTimeFormat

class ExportKeywordPerformanceReportRequestCsvActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case dateRange: (Option[String], Option[String]) =>
      try {
        val six_months_ago = DateTime.now.minusMonths(6)
        val file_prefix = "msn_keyword_performance"
        deleteFiles("/tmp", file_prefix)
        val filename = "/tmp/%s_%s.csv".format(
          file_prefix,
          dateRange._1.getOrElse(googleReportDateFormatter.print(six_months_ago))
        )
        val file = new File(filename)
        val bw = new BufferedWriter(new FileWriter(file))
        bw.write(mongoToCsv(
          msnReportCollection
            .find(buildQry("", "", "", "", dateRange._1.getOrElse(""), dateRange._2.getOrElse(""))).toList.distinct
            .sortBy{r => DateTime.parse(r.getString("gregoriandate"), DateTimeFormat.forPattern("M/d/yyyy")).toDate.getTime}.map{ row =>
            Document(
              "Date" -> googleReportDateFormatter.print(DateTime.parse(row.getString("gregoriandate"), DateTimeFormat.forPattern("M/d/yyyy"))),
              "Source" -> "msn",
              "KeywordId" -> row.getLong("keywordid").longValue(),
              "Cost" -> row.getDouble("spend").doubleValue()
            )
          }
        ))
        bw.close()
      } catch {
        case e: Exception =>
          log.error("Error exporting msn keyword performance report CSV - %s".format(e.toString))
      }
  }
}