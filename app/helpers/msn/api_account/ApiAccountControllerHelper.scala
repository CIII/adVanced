package helpers.msn.api_account

import Shared.Shared._
import models.mongodb.msn.Msn.ApiAccount
import play.api.data.Form
import play.api.data.Forms._

object ApiAccountControllerHelper {
  def apiAccountForm: Form[ApiAccount] = Form(
    mapping(
      "_id" -> optional(text),
      "name" -> nonEmptyText,
      "userName" -> nonEmptyText,
      "password" -> nonEmptyText,
      "developerToken" -> nonEmptyText
    )((
        _id,
        name,
        userName,
        password,
        developerToken
        ) => ApiAccount(
      _id=formStringToObjectId(_id),
      name=name,
      userName=userName,
      password=password,
      developerToken=developerToken
    ))((apiAccount: ApiAccount) => Some((
      objectIdToFormString(apiAccount._id),
      apiAccount.name,
      apiAccount.userName,
      apiAccount.password,
      apiAccount.developerToken
      )))
  )
}
