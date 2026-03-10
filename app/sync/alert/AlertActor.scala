package sync.alert

import Shared.Shared.AlertConfigurationMessage
import org.apache.pekko.actor.Actor
import org.apache.pekko.event.Logging
class AlertActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case alert_msg: AlertConfigurationMessage =>
      // TODO: Implement alert processing
    case _ =>
      // TODO: Log unexpected message
  }
}