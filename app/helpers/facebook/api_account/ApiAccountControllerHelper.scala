package helpers.facebook.api_account

import Shared.Shared._
import models.mongodb.facebook.Facebook.FacebookApiAccount
import play.api.data.Form
import play.api.data.Forms._

object ApiAccountControllerHelper {
  def facebookApiAccountForm: Form[FacebookApiAccount] = Form(
    mapping(
      "_id" -> optional(text),
      "accountId" -> nonEmptyText,
      "applicationSecret" -> nonEmptyText,
      "accessToken" -> nonEmptyText
    )((
        _id,
        accountId,
        applicationSecret,
        accessToken
        ) => FacebookApiAccount(
      _id=formStringToObjectId(_id),
      accountId=accountId,
      applicationSecret=applicationSecret,
      accessToken=accessToken
    ))((apiAccount: FacebookApiAccount) => Some((
      objectIdToFormString(apiAccount._id),
      apiAccount.accountId,
      apiAccount.applicationSecret,
      apiAccount.accessToken
    )))
  )
}
