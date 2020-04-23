package helpers.yahoo.api_account.advertiser

import Shared.Shared._
import models.mongodb.yahoo.Yahoo.Advertiser
import play.api.data.Form
import play.api.data.Forms._

object AdvertiserControllerHelper {
  def advertiserForm: Form[Advertiser] = Form(
    mapping(
      "_id" -> optional(text),
      "apiId" -> longNumber,
      "advertiserName" -> nonEmptyText,
      "timeZone" -> nonEmptyText,
      "currency" -> nonEmptyText,
      "status" -> nonEmptyText,
      "billingCountry" -> nonEmptyText,
      "webSiteUrl" -> nonEmptyText,
      "lastUpdateDate" -> longNumber,
      "bookingCountry" -> nonEmptyText,
      "language" -> nonEmptyText,
      "createdDate" -> longNumber
    )((
      _id,
      apiId,
      advertiserName,
      timezone,
      currency,
      status,
      billingCountry,
      webSiteUrl,
      lastUpdateDate,
      bookingCountry,
      language,
      createdDate
    ) => Advertiser(
      _id=formStringToObjectId(_id),
      apiId=apiId,
      advertiserName=advertiserName,
      timezone=timezone,
      currency=currency,
      status=status,
      billingCountry=billingCountry,
      webSiteUrl=webSiteUrl,
      lastUpdateDate=lastUpdateDate,
      bookingCountry=bookingCountry,
      language=language,
      createdDate=createdDate
    ))((advertiser: Advertiser) => Some((
      objectIdToFormString(advertiser._id),
      advertiser.apiId,
      advertiser.advertiserName,
      advertiser.timezone,
      advertiser.currency,
      advertiser.status,
      advertiser.billingCountry,
      advertiser.webSiteUrl,
      advertiser.lastUpdateDate,
      advertiser.bookingCountry,
      advertiser.language,
      advertiser.createdDate
    )))
  )
}
