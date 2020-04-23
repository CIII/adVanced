package sync.msn.process.reporting

import Shared.Shared.{MsnReportRequest, MsnReportType}
import akka.actor.Actor
import akka.event.Logging
import com.microsoft.bingads.reporting._
import models.mongodb.Utilities._
import models.mongodb.msn.Msn._
import sync.msn.bingads.BingAdsHelper
import util.MongoJson
import sync.msn.bingads.ReportHelper._

import scala.io.Source
import scala.util.control.Breaks._

class MsnReportActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case msnReportRequest: MsnReportRequest =>
      try {
        log.info("Processing %s -> Date Range: %s to %s -> Download Format: %s".format(
          msnReportRequest.reportType,
          msnReportRequest.startDate,
          msnReportRequest.endDate,
          msnReportRequest.reportFormat
        ))

        val bingAdsHelper = new BingAdsHelper(
          msnReportRequest.apiAccount.userName,
          msnReportRequest.apiAccount.password,
          msnReportRequest.apiAccount.developerToken,
          Some(msnReportRequest.customerId),
          Some(msnReportRequest.customerAccountId)
        )

        val reportingServiceManager = new ReportingServiceManager(bingAdsHelper.authData)

        var reportRequest: Option[ReportRequest] = None

        if(msnReportRequest.reportType == MsnReportType.KeywordPerformanceReportRequest) {
          reportRequest = Some(getKeywordPerformanceReportRequest(msnReportRequest, bingAdsHelper))
        }

        val reportingDownloadOperation = reportingServiceManager.submitDownloadAsync(
          reportRequest.get,
          null
        ).get

        var reportingOperationStatus: Option[ReportingOperationStatus] = None
        breakable {
          for (i <- 0 to 30) {
            Thread.sleep(5000)
            reportingOperationStatus = Some(reportingDownloadOperation.getStatusAsync(null).get())
            if (reportingOperationStatus.get.getStatus == ReportRequestStatusType.SUCCESS) {
              break()
            }
          }
        }

        val resultFile = reportingDownloadOperation.downloadResultFileAsync(null, "msn_tmp.csv", true, true, null).get()
        val lines = Source.fromFile(resultFile).getLines().toList
        if (lines.length > 13) {
          for (item <- csvToJson(lines.slice(10, lines.length - 1).mkString("\n"))) {
            var sanitized_item = item
            sanitized_item.keys.foreach { k =>
              if (k.contains(".")) {
                val value = sanitized_item.\(k).get
                sanitized_item = sanitized_item.-(k)
                sanitized_item.+(k.replace(".", "") -> value)
              }
            }
            msnReportCollection(msnReportRequest.reportType).insert(MongoJson.fromJson(sanitized_item))
          }
        }
        resultFile.delete()
      } catch {
        case e: Exception =>
          e.printStackTrace()
          log.info("Error Processing Report (%s) - %s".format(
            msnReportRequest.reportType,
            e.toString
          ))
      } finally { context.stop(self) }
  }
}
