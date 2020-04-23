package sync.lynx.process

import Shared.Shared._
import akka.actor.Actor
import akka.event.Logging
import com.mongodb.casbah.Imports._
import dao.ArrivalFactDAO
import models.mongodb.lynx.TQReporting
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc.Controller
import slick.driver.JdbcProfile
import scala.concurrent.ExecutionContext.Implicits.global

class LynxReportActor extends Actor with Controller {
  val log = Logging(context.system, this)

  def receive = {
    case reportRequest: LynxReportRequest =>
      try {
        log.info("Lynx Report Data Pull Started")
        reportRequest.reportType match {
          case _ if reportRequest.reportType == LynxReportType.TQReportingArrivalFactsReportRequest =>
            val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
            val arrivalFactDAO = new ArrivalFactDAO(dbConfig)
            arrivalFactDAO.all().map { facts =>
              facts.foreach { fact =>
                // Convert the timestamps in the session from UTC to EST to match
                // the performance reports we pull in.  Have to specify that they are UTC first
                // because we don't store the tz information in tq_reporting.
                if (fact.session.created_at.isDefined) {
                  fact.session.created_at = Some(
                    new DateTime(fact.session.created_at.get)
                      .withZoneRetainFields(DateTimeZone.UTC)
                      .withZone(DateTimeZone.forID("America/New_York"))
                  )
                }

                if (fact.session.last_activity.isDefined) {
                  fact.session.last_activity = Some(
                    new DateTime(fact.session.last_activity.get)
                      .withZoneRetainFields(DateTimeZone.UTC)
                      .withZone(DateTimeZone.forID("America/New_York"))
                  )
                }

                TQReporting.arrivalFactCollection.findAndModify(
                  DBObject("session_id" -> fact.session.session_id),
                  null,
                  null,
                  false,
                  TQReporting.arrivalFactToDBObject(fact),
                  true,
                  true
                )
              }
              log.info("Lynx Report Data Pull Complete")
            }
        }
      } catch {
        case e: Exception =>
          log.info(s"ERROR PROCESSING LYNX REPORT -> $e")
          e.printStackTrace()
      } finally { context.stop(self) }
  }
}