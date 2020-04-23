package helpers.yahoo.api_account

import Shared.Shared._
import models.mongodb.yahoo.Yahoo.ApiAccount
import play.api.data.Form
import play.api.data.Forms._

object ApiAccountControllerHelper {
  def apiAccountForm: Form[ApiAccount] = Form(
    mapping(
      "_id" -> optional(text),
      "name" -> nonEmptyText,
      "clientId" -> nonEmptyText,
      "clientSecret" -> nonEmptyText,
      "refreshToken" -> nonEmptyText
    )((_id, name, clientId, clientSecret, refreshToken) => ApiAccount(
      _id=formStringToObjectId(_id),
      name=name,
      clientId=clientId,
      clientSecret=clientSecret,
      refreshToken=refreshToken
    ))((apiAccount: ApiAccount) => Some((
      objectIdToFormString(apiAccount._id),
      apiAccount.name,
      apiAccount.clientId,
      apiAccount.clientSecret,
      apiAccount.refreshToken
    )))
  )
}
