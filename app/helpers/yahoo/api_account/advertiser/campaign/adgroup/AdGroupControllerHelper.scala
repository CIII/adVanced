package helpers.yahoo.api_account.advertiser.campaign.adgroup

import Shared.Shared._
import models.mongodb.yahoo.Yahoo._
import play.api.data.Form
import play.api.data.Forms._

object AdGroupControllerHelper {
  def adGroupForm: Form[AdGroup] = Form(
    mapping(
      "_id" -> optional(text),
      "advertiserObjId" -> optional(text),
      "advertiserApiId" -> optional(longNumber),
      "campaignObjId" -> optional(text),
      "campaignApiId" -> optional(longNumber),
      "apiId" -> optional(longNumber),
      "adGroupName" -> nonEmptyText,
      "bidSet" -> mapping(
        "bids" -> list(
          mapping(
            "priceType" -> text,
            "value" -> longNumber,
            "channel" -> text
          )((priceType, value, channel) => Bid(priceType=PriceType.withName(priceType), value=value, channel=Channel.withName(channel)))
          ((bid: Bid) => Some((bid.priceType.toString, bid.value, bid.channel.toString)))
        )
      )(BidSet.apply)(BidSet.unapply),
      "status" -> text,
      "startDateStr" -> nonEmptyText,
      "endDateStr" -> nonEmptyText,
      "advancedGeoPos" -> optional(text),
      "advancedGeoNeg" -> optional(text),
      "biddingStrategy" -> optional(text),
      "epcaGoal" -> longNumber
    )((
      _id,
      advertiserObjId,
      advertiserApiId,
      campaignObjId,
      campaignApiId,
      apiId,
      adGroupName,
      bidSet,
      status,
      startDateStr,
      endDateStr,
      advancedGeoPos,
      advancedGeoNeg,
      biddingStrategy,
      epcaGoal
      ) => AdGroup(
        _id=formStringToObjectId(_id),
        advertiserObjId=formStringToObjectId(advertiserObjId),
        advertiserApiId=advertiserApiId,
        campaignObjId=formStringToObjectId(campaignObjId),
        campaignApiId=campaignApiId,
        apiId=apiId,
        adGroupName=adGroupName,
        bidSet=bidSet,
        status=models.mongodb.yahoo.Yahoo.Status.withName(status),
        startDateStr=startDateStr,
        endDateStr=endDateStr,
        advancedGeoPos=advancedGeoPos match {case Some(agp) => Some(AdvancedGeoPos.withName(agp)) case None => None},
        advancedGeoNeg=advancedGeoNeg match {case Some(agn) => Some(AdvancedGeoNeg.withName(agn)) case None => None},
        biddingStrategy=biddingStrategy match {case Some(bs) => Some(BiddingStrategy.withName(bs)) case None => None},
        epcaGoal=epcaGoal
    ))((adGroup: AdGroup) => Some((
      objectIdToFormString(adGroup._id),
      objectIdToFormString(adGroup.advertiserObjId),
      adGroup.advertiserApiId,
      objectIdToFormString(adGroup.campaignObjId),
      adGroup.campaignApiId,
      adGroup.apiId,
      adGroup.adGroupName,
      adGroup.bidSet,
      adGroup.status.toString,
      adGroup.startDateStr,
      adGroup.endDateStr,
      adGroup.advancedGeoPos match {case Some(agp) => Some(agp.toString) case None => None},
      adGroup.advancedGeoNeg match {case Some(agn) => Some(agn.toString) case None => None},
      adGroup.biddingStrategy match {case Some(bs) => Some(bs.toString) case None => None},
      adGroup.epcaGoal
    )))
  )
}
