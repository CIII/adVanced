package sync.yahoo.reporting

import Shared.Shared._
import akka.actor.Actor
import akka.event.Logging
import models.mongodb.Utilities._
import models.mongodb.yahoo.Yahoo._
import play.api.libs.json.Json
import sync.yahoo.gemini.GeminiHelper
import com.mongodb.casbah.Imports._

class YahooReportActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case reportRequest: GeminiReportRequest =>
      try {
        log.info("Processing %s -> Download Format: %s".format(
          reportRequest.reportType.toString,
          reportRequest.reportFormat.toString
        ))

        val geminiHelper = new GeminiHelper(
          reportRequest.oAuthData.clientId,
          reportRequest.oAuthData.clientSecret,
          reportRequest.oAuthData.refreshToken,
          log
        )

        yahooAdvertiserCollection.find(DBObject("apiAccountObjId" -> reportRequest.oAuthData._id.get)).foreach { adv =>
          reportRequest.filters = Some(GeminiReportFilter(
            field=GeminiReportField.Advertiser_ID,
            operator=GeminiReportFilterOperation.`=`,
            value=Some(adv.as[Long]("apiId").toString)
          ) :: reportRequest.filters.getOrElse(List()).filter(_.field != GeminiReportField.Advertiser_ID))
          geminiHelper.reportRequest(reportRequest) match {
            case None =>
            case Some(report_csv) =>
              val jsonObjs = csvToJson(report_csv)
              if(jsonObjs.nonEmpty) {
                jsonObjs.foreach { json =>
                  yahooReportCollection(reportRequest.reportType).insert(
                    com.mongodb.util.JSON.parse(Json.stringify(json)).asInstanceOf[DBObject]
                  )
                }
              } else {
                log.info("Empty Report (%s) - Advertiser ID (%s)..Skipping".format(reportRequest.reportType.toString, adv.as[Long]("apiId")))
              }
          }
        }
      } catch {
        case e: Exception =>
          log.info("Error Processing Report (%s) - %s".format(
            reportRequest.reportType.toString,
            e.toString
          ))
          e.printStackTrace()
      }
  }
}