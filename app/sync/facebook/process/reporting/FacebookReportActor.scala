package sync.facebook.process.reporting

import Shared.Shared._
import org.apache.pekko.actor.Actor
import org.apache.pekko.event.Logging
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.facebook.Facebook._

class FacebookReportActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case _ => //todo: unimplemented
  }
}