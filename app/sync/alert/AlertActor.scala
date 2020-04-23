package sync.alert

import Shared.Shared.AlertConfigurationMessage
import akka.actor.Actor
import akka.event.Logging
class AlertActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case alert_msg: AlertConfigurationMessage =>

    case _ =>

  }
}