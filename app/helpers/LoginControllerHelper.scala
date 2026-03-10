package helpers

import play.api.data.Forms.mapping
import play.api.data.Form
import play.api.data.Forms._

object LoginControllerHelper {
  case class PasswordChange(
    username: String,
    currentPassword: String,
    newPassword: String,
    confirmNewPassword: String
  )

  def passwordChangeForm: Form[PasswordChange] = Form(
    mapping(
      "username" -> nonEmptyText,
      "currentPassword" -> nonEmptyText,
      "newPassword" -> nonEmptyText,
      "confirmNewPassword" -> nonEmptyText
    )(PasswordChange.apply)(PasswordChange.unapply)
  )
}
