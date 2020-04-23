package helpers.yahoo.api_account.advertiser.campaign

import Shared.Shared._
import models.mongodb.yahoo.Yahoo._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._

object CampaignControllerHelper {
  def campaignForm: Form[Campaign] = Form(
    mapping(
      "_id" -> optional(text),
      "advertiserObjId" -> optional(text),
      "advertiserApiId" -> optional(longNumber),
      "budget" -> optional(of[Double]),
      "budgetType" -> optional(text),
      "campaignName" -> text,
      "channel" -> optional(text),
      "apiId" -> optional(longNumber),
      "language" -> optional(text),
      "objective" -> optional(text),
      "status" -> text,
      "isPartnerNetwork" -> optional(text),
      "defaultLandingUrl" -> optional(text),
      "trackingPartner" -> optional(text),
      "appLocale" -> optional(text),
      "advancedGeoPos" -> optional(text),
      "advancedGeoNeg" -> optional(text)
    )((
      _id,
      advertiserObjId,
      advertiserApiId,
      budget,
      budgetType,
      campaignName,
      channel,
      apiId,
      language,
      objective,
      status,
      isPartnerNetwork,
      defaultLandingUrl,
      trackingPartner,
      appLocale,
      advancedGeoPos,
      advancedGeoNeg
      ) => Campaign(
        _id=formStringToObjectId(_id),
        advertiserObjId=formStringToObjectId(advertiserObjId),
        advertiserApiId=advertiserApiId,
        budget=budget,
        budgetType=budgetType match {case Some(bt) => Some(BudgetType.withName(bt)) case None => None},
        campaignName=campaignName,
        channel=channel match {case Some(c) => Some(Channel.withName(c)) case None => None},
        apiId=apiId,
        language=language,
        objective=objective,
        status=models.mongodb.yahoo.Yahoo.Status.withName(status),
        isPartnerNetwork=isPartnerNetwork match {case Some(ipn) => Some(Bool.withName(ipn)) case None => None},
        defaultLandingUrl=defaultLandingUrl,
        trackingPartner=trackingPartner,
        appLocale=appLocale,
        advancedGeoPos=advancedGeoPos match {case Some(agp) => Some(AdvancedGeoPos.withName(agp)) case None => None},
        advancedGeoNeg=advancedGeoNeg match {case Some(agn) => Some(AdvancedGeoNeg.withName(agn)) case None => None}
    ))((campaign: Campaign) => Some((
      objectIdToFormString(campaign._id),
      objectIdToFormString(campaign.advertiserObjId),
      campaign.advertiserApiId,
      campaign.budget,
      campaign.budgetType match {case Some(bt) => Some(bt.toString) case None => None},
      campaign.campaignName,
      campaign.channel match {case Some(c) => Some(c.toString) case None => None},
      campaign.apiId,
      campaign.language,
      campaign.objective,
      campaign.status.toString,
      campaign.isPartnerNetwork match {case Some(ipn) => Some(ipn.toString) case None => None},
      campaign.defaultLandingUrl,
      campaign.trackingPartner,
      campaign.appLocale,
      campaign.advancedGeoPos match {case Some(agp) => Some(agp.toString) case None => None},
      campaign.advancedGeoNeg match {case Some(agn) => Some(agn.toString) case None => None}
    )))
  )
}
