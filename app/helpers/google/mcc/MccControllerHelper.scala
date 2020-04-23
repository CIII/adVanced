package helpers.google.mcc

import Shared.Shared._
import models.mongodb.google.Google.Mcc
import play.api.data.Form
import play.api.data.Forms._

object MccControllerHelper {
  def mccForm: Form[Mcc] = Form(
    mapping(
      "_id" -> optional(text),
      "name" -> nonEmptyText,
      "developerToken" -> nonEmptyText,
      "oAuthClientId" -> nonEmptyText,
      "oAuthClientSecret" -> nonEmptyText,
      "oAuthRefreshToken" -> nonEmptyText
    )((
      _id,
      name,
      developerToken,
      oAuthClientId,
      oAuthClientSecret,
      oAuthRefreshToken
      ) => Mcc(
        _id=formStringToObjectId(_id),
        name=name,
        developerToken=developerToken,
        oAuthClientId=oAuthClientId,
        oAuthClientSecret=oAuthClientSecret,
        oAuthRefreshToken=oAuthRefreshToken
    ))((mcc: Mcc) => Some((
      objectIdToFormString(mcc._id),
      mcc.name,
      mcc.developerToken,
      mcc.oAuthClientId,
      mcc.oAuthClientSecret,
      mcc.oAuthRefreshToken
    )))
  )
}