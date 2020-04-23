package helpers.yahoo.api_account.advertiser.campaign.adgroup.keyword

import Shared.Shared._
import models.mongodb.yahoo.Yahoo._
import play.api.data.Form
import play.api.data.Forms._

object KeywordControllerHelper {
  def adGroupKeywordForm: Form[Keyword] = Form(
    mapping(
      "_id" -> optional(text),
      "advertiserObjId" -> optional(text),
      "advertiserApiId" -> optional(longNumber),
      "campaignObjId" -> optional(text),
      "campaignApiId" -> optional(longNumber),
      "adGroupObjId" -> optional(text),
      "adGroupApiId" -> optional(longNumber),
      "bidSet" -> optional(
        mapping(
          "bids" -> list(
            mapping(
              "priceType" -> text,
              "value" -> longNumber,
              "channel" -> text
            )((priceType, value, channel) => Bid(priceType=PriceType.withName(priceType), value=value, channel=Channel.withName(channel)))
            ((bid: Bid) => Some((bid.priceType.toString, bid.value, bid.channel.toString)))
          )
        )(BidSet.apply)(BidSet.unapply)
      ),
      "exclude" -> boolean,
      "apiId" -> longNumber,
      "matchType" -> text,
      "parentType" -> text,
      "status" -> text,
      "value" -> nonEmptyText,
      "adParamValues" -> optional(
        list(
          mapping(
            "paramIndex" -> number,
            "insertionText" -> nonEmptyText
          )(AdParamValue.apply)(AdParamValue.unapply)
        )
      ),
      "landingUrl" -> optional(text)
    )((
      _id,
      advertiserObjId,
      advertiserApiId,
      campaignObjId,
      campaignApiId,
      adGroupObjId,
      adGroupApiId,
      bidSet,
      exclude,
      apiId,
      matchType,
      parentType,
      status,
      value,
      adParamValues,
      landingUrl
    ) => Keyword(
      _id=formStringToObjectId(_id),
      advertiserObjId=formStringToObjectId(advertiserObjId),
      advertiserApiId=advertiserApiId,
      campaignObjId=formStringToObjectId(campaignObjId),
      campaignApiId=campaignApiId,
      adGroupObjId=formStringToObjectId(adGroupObjId),
      adGroupApiId=adGroupApiId,
      bidSet=bidSet,
      exclude=exclude,
      apiId=apiId,
      matchType=MatchType.withName(matchType),
      parentType=ParentType.withName(parentType),
      status=models.mongodb.yahoo.Yahoo.Status.withName(status),
      value=value,
      adParamValues=adParamValues,
      landingUrl=landingUrl
    )
  )((keyword: Keyword) => Some((
      objectIdToFormString(keyword._id),
      objectIdToFormString(keyword.advertiserObjId),
      keyword.advertiserApiId,
      objectIdToFormString(keyword.campaignObjId),
      keyword.campaignApiId,
      objectIdToFormString(keyword.adGroupObjId),
      keyword.adGroupApiId,
      keyword.bidSet,
      keyword.exclude,
      keyword.apiId,
      keyword.matchType.toString,
      keyword.parentType.toString,
      keyword.status.toString,
      keyword.value,
      keyword.adParamValues,
      keyword.landingUrl
    ))
  ))
}
