package helpers

import play.api.data.Form
import play.api.data.Forms._

object NotificationConfigurationControllerHelper {
  case class NotificationForm(
    userId: String,
    showAlertsOnDashboard: Boolean,
    alertTypes: Option[List[String]],
    alertSeverity: Option[List[String]],
    enableSmsAlerts: Boolean,
    sms: Option[String],
    enableEmailAlerts: Boolean,
    email: Option[String]
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
