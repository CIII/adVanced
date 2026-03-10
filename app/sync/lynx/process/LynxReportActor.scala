package sync.lynx.process

import Shared.Shared._
import org.apache.pekko.actor.Actor
import org.apache.pekko.event.Logging
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import com.mongodb.client.model.{FindOneAndReplaceOptions, ReturnDocument}
import dao.ArrivalFactDAO
import models.mongodb.lynx.TQReporting
import org.joda.time.{DateTime, DateTimeZone}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

// TODO: Convert to injectable class with DI for DatabaseConfigProvider
class LynxReportActor(dbConfigProvider: DatabaseConfigProvider)(implicit ec: scala.concurrent.ExecutionContext) extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case reportRequest: LynxReportRequest =>
      try {
        log.info("Lynx Report Data Pull Started")
        reportRequest.reportType match {
          case _ if reportRequest.reportType == LynxReportType.TQReportingArrivalFactsReportRequest =>
            val dbConfig = dbConfigProvider.get[JdbcProfile]
            val arrivalFactDAO = new ArrivalFactDAO(dbConfig)
            arrivalFactDAO.all().map { facts =>
              facts.foreach { factRaw =>
                // Convert the timestamps in the session from UTC to EST to match
                // the performance reports we pull in.  Have to specify that they are UTC first
                // because we don't store the tz information in tq_reporting.
                val updatedSession = factRaw.session.copy(
                  created_at = if (factRaw.session.created_at.isDefined) {
                    Some(
                      new DateTime(factRaw.session.created_at.get)
                        .withZoneRetainFields(DateTimeZone.UTC)
                        .withZone(DateTimeZone.forID("America/New_York"))
                    )
                  } else factRaw.session.created_at,
                  last_activity = if (factRaw.session.last_activity.isDefined) {
                    Some(
                      new DateTime(factRaw.session.last_activity.get)
                        .withZoneRetainFields(DateTimeZone.UTC)
                        .withZone(DateTimeZone.forID("America/New_York"))
                    )
                  } else factRaw.session.last_activity
                )
                val fact = factRaw.copy(session = updatedSession)

                TQReporting.arrivalFactCollection.findOneAndReplace(
                  Document("session_id" -> fact.session.session_id),
                  TQReporting.arrivalFactToDocument(fact),
                  new FindOneAndReplaceOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
                )
              }
              log.info("Lynx Report Data Pull Complete")
            }
        }
      } catch {
        case e: Exception =>
          log.info(s"ERROR PROCESSING LYNX REPORT -> $e")
          log.error(s"Error processing Lynx report", e)
      } finally { context.stop(self) }
  }
}
