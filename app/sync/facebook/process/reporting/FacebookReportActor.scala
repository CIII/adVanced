package sync.facebook.process.reporting

import Shared.Shared._
import akka.actor.Actor
import akka.event.Logging
import com.mongodb.casbah.Imports._
import models.mongodb.facebook.Facebook._

class FacebookReportActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case _ => //todo: unimplemented
  }
}