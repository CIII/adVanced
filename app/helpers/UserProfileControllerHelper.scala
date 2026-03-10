package helpers

import play.api.data.Form
import play.api.data.Forms._

import scala.collection.immutable.List

/**
 * Created by clarencewilliams on 11/30/15.
 */
object UserProfileControllerHelper {
  case class UserProfileForm(
    _id: Option[String],
    username: String,
    password: String,
    email: String,
    advertiserIds: Option[String],
    security_roles: List[String],
    security_roles_str: Option[String]
  )
  
  def userProfileForm: Form[UserProfileForm] = Form(
    mapping(
      "_id" -> optional(text),
      "username" -> nonEmptyText,
      "password" -> text,
      "email" -> email,
      "advertiserIds" -> optional(text),
      "security_roles" -> list(text),
      "security_roles_str" -> optional(text)
    )(UserProfileForm.apply)(UserProfileForm.unapply)
  )
  
}
