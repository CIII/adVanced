package sync.google.process.reporting

import Shared.Shared.GoogleReportRequest
import akka.actor.Actor
import akka.event.Logging
import com.mongodb.casbah.commons.MongoDBObject
import models.mongodb.Utilities._
import models.mongodb.google.Google._
import models.mongodb.performance.PerformanceEntity
import sync.google.adwords.AdWordsHelper
import sync.google.adwords.account.CustomerHelper
import util.MongoJson
import util.charts.ChartCache
import play.api.libs.json._
import sync.google.adwords.ReportHelper._
import sync.shared.Google._

class GoogleReportActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case reportRequest: GoogleReportRequest =>
      try {
        log.info("Processing %s -> Account: %s -> Date Range: %s -> Download Format: %s".format(
          reportRequest.reportType.value,
          reportRequest.accountName,
          reportRequest.dateRange.value,
          reportRequest.downloadFormat.value
        ))
        log.info(s"FINISHED PROCESSING GOOGLE REPORT - $getDateString")

        val adWordsHelper = new AdWordsHelper(
          clientId = reportRequest.oAuthData.oAuthClientId,
          clientSecret = reportRequest.oAuthData.oAuthClientSecret,
          refreshToken = reportRequest.oAuthData.oAuthRefreshToken,
          developerToken = reportRequest.oAuthData.developerToken,
          customerId = reportRequest.customerId,
          enablePartialFailure = false
        )

        val accountHelper = new CustomerHelper(adWordsHelper, log)

        // Wipe the collection for the day which we are adding records.  Because we can't really
        // dedup based on report criteria, this will ensure we don't have any duplicate records
        val wipeDay = googleDateToDateTime(reportRequest.dateRange).toString("yyyy-MM-dd")
        log.info("Removing report records from " + reportRequest.reportType.toString + " on or after " + wipeDay)
        googleReportCollection(reportRequest.reportType).remove(
          com.mongodb.casbah.Imports.DBObject(PerformanceEntity.dateField.fieldName -> MongoDBObject("$gte" -> wipeDay))
        )

        accountHelper.downloadReport(
          reportRequest.reportType,
          reportRequest.fields,
          reportRequest.predicates,
          reportRequest.dateRange,
          reportRequest.downloadFormat,
          reportRequest.enableZeroImpressions
        ) match {
          case None =>
          case Some(report_csv) =>
            for (item <- csvToJson(
              report_csv
                .split("\n")
                .drop(1)
                .dropRight(1)
                .mkString("\n")
            )) {
              var sanitizedItem = item
              sanitizedItem.keys.foreach { k =>
                var sanitizedKey: String = k
                val sanitizedValue: JsValue = sanitizedItem.\(k).get
                if (k.contains(".")) {
                  sanitizedKey = sanitizedKey.replace(".", "")
                }

                sanitizedItem = sanitizedItem - k
                sanitizedItem = sanitizedItem + (sanitizedKey -> sanitizedValue)
              }

              sanitizedItem = sanitizedItem + ("accountName" -> Json.toJson(reportRequest.accountName))
              sanitizedItem = attributeLynxSessionData(log, sanitizedItem, reportRequest.reportType)

              reportRequest.reportType match {
                case com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType.CAMPAIGN_PERFORMANCE_REPORT =>
                  val performance = parseGoogleCampaignPerformance(sanitizedItem)
                  googleReportCollection(reportRequest.reportType).insert(performance.toDBO)
                case com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType.ADGROUP_PERFORMANCE_REPORT =>
                  val performance = parseGoogleAdGroupPerformance(sanitizedItem)
                  googleReportCollection(reportRequest.reportType).insert(performance.toDBO)
                case com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType.AD_PERFORMANCE_REPORT =>
                  val performance = parseGoogleAdPerformance(sanitizedItem)
                  googleReportCollection(reportRequest.reportType).insert(performance.toDBO)
                case com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType.GEO_PERFORMANCE_REPORT =>
                  val performance = parseGoogleGeoPerformance(sanitizedItem)
                  googleReportCollection(reportRequest.reportType).insert(performance.toDBO)
                case _ =>
                  googleReportCollection(reportRequest.reportType).insert(MongoJson.fromJson(sanitizedItem))
              }
            }
        }

        log.info(s"FINISHED PROCESSING GOOGLE REPORT - $getDateString")
      } catch {
        case e: Exception =>
          e.printStackTrace()
          log.info("Error Processing Report (%s) - %s".format(
            reportRequest.reportType.value,
            e.toString
          ))
      } finally {
        ChartCache.clearCache
        log.info("Finished processing report")
        context.stop(self)
      }
  }
}
