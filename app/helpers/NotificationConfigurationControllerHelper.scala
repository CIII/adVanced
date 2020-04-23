package helpers

import play.api.data.Form
import play.api.data.Forms._

object NotificationConfigurationControllerHelper {
  case class NotificationForm(
    var userId: String,
    var showAlertsOnDashboard: Boolean,
    var alertTypes: Option[List[String]],
    var alertSeverity: Option[List[String]],
    var enableSmsAlerts: Boolean,
    var sms: Option[String],
    var enableEmailAlerts: Boolean,
    var email: Option[String]
  )

  def notificationForm(user_id: String): Form[NotificationForm] = Form(
    mapping(
      "userId" -> ignored(user_id),
      "showAlertsOnDashboard" -> boolean,
      "alertTypes" -> optional(list(text)),
      "alertSeverity" -> optional(list(text)),
      "enableSmsAlerts" -> boolean,
      "sms" -> optional(text),
      "enableEmailAlerts" -> boolean,
      "email" -> optional(text)
    )(NotificationForm.apply)(NotificationForm.unapply)
  )
}
