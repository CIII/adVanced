package models.mongodb.yahoo

import Shared.Shared._
import com.mongodb.casbah.Imports._

object Yahoo {
  def yahooApiAccountCollection = advancedCollection("yahoo_api_account")
  def yahooAdvertiserCollection = advancedCollection("yahoo_advertiser")
  def yahooCampaignCollection = advancedCollection("yahoo_campaign")
  def yahooAdGroupCollection = advancedCollection("yahoo_adgroup")
  def yahooAdExtensionCollection = advancedCollection("yahoo_ad_extension")
  def yahooKeywordCollection = advancedCollection("yahoo_keyword")
  def yahooAdCollection = advancedCollection("yahoo_ad")
  def yahooReportCollection(
    reportType: GeminiReportType.Value
  ) = advancedCollection(
    "yahoo_%s".format(reportType.toString)
  )


  object BudgetType extends Enumeration {
    type BudgetType = Value
    val LIFETIME, DAILY = Value
  }

  object Channel extends Enumeration {
    type Channel = Value
    val NATIVE, SEARCH, SEARCH_AND_NATIVE = Value
  }

  object Status extends Enumeration {
    type Status = Value
    val ACTIVE, PAUSED, DELETED = Value
  }

  object Bool extends Enumeration {
    type Bool = Value
    val TRUE, FALSE = Value
  }

  object AdvancedGeoPos extends Enumeration {
    type AdvancedGeoPos = Value
    val DEFAULT, LOCATION_OF_PRESENCE, LOCATION_OF_INTEREST = Value
  }

  object AdvancedGeoNeg extends Enumeration {
    type AdvancedGeoNeg = Value
    val DEFAULT, LOCATION_OF_PRESENCE = Value
  }

  object BiddingStrategy extends Enumeration {
    type BiddingStrategy = Value
    val OPT_CONVERSION, DEFAULT = Value
  }

  object CallToAction extends Enumeration {
    type CallToAction = Value
    val Install, Download, Play, Buy, Shop, Signup, Register, Book, Try, Launch, Watch, Learn, Read, Get = Value

  }

  object MatchType extends Enumeration {
    type MatchType = Value
    val BROAD, PHRASE, EXACT = Value
  }

  object ParentType extends Enumeration {
    type ParentType = Value
    val ADGROUP, CAMPAIGN = Value
  }

  object PriceType extends Enumeration {
    type PriceType = Value
    val CPC, CPM, CPV = Value
  }

  case class ApiAccount(
    _id: Option[ObjectId],
    name: String,
    clientId: String,
    clientSecret: String,
    refreshToken: String
  )

  def dboToApiAccount(dbo: DBObject): ApiAccount = {
    ApiAccount(
      _id = dbo._id,
      name = dbo.as[String]("name"),
      clientId = dbo.as[String]("clientId"),
      clientSecret = dbo.as[String]("clientSecret"),
      refreshToken = dbo.as[String]("refreshToken")
    )
  }

  def apiAccountToDBObject(aa: ApiAccount): DBObject = {
    DBObject(
      "_id" -> aa._id,
      "name" -> aa.name,
      "clientId" -> aa.clientId,
      "clientSecret" -> aa.clientSecret,
      "refreshToken" -> aa.refreshToken
    )
  }

  case class Advertiser(
    _id: Option[ObjectId],
    var apiId: Long,
    var advertiserName: String,
    var timezone: String,
    var currency: String,
    var status: String,
    var billingCountry: String,
    var webSiteUrl: String,
    var lastUpdateDate: Long=0L,
    var bookingCountry: String,
    var language: String,
    var createdDate: Long=0L
  )

  case class Campaign(
    _id: Option[ObjectId],
    advertiserObjId: Option[ObjectId],
    advertiserApiId: Option[Long],
    budget: Option[Double],
    budgetType: Option[BudgetType.Value],
    campaignName: String,
    channel: Option[Channel.Value],
    apiId: Option[Long],
    language: Option[String],
    objective: Option[String],
    status: Status.Value,
    isPartnerNetwork: Option[Bool.Value],
    defaultLandingUrl: Option[String],
    trackingPartner: Option[String],
    appLocale: Option[String],
    advancedGeoPos: Option[AdvancedGeoPos.Value],
    advancedGeoNeg: Option[AdvancedGeoNeg.Value]
  )

  case class AdGroup(
    _id: Option[ObjectId],
    advertiserObjId: Option[ObjectId],
    advertiserApiId: Option[Long],
    campaignObjId: Option[ObjectId],
    campaignApiId: Option[Long],
    apiId: Option[Long],
    adGroupName: String,
    bidSet: BidSet,
    status: Status.Value,
    startDateStr: String,
    endDateStr: String,
    advancedGeoPos: Option[AdvancedGeoPos.Value],
    advancedGeoNeg: Option[AdvancedGeoNeg.Value],
    biddingStrategy: Option[BiddingStrategy.Value],
    epcaGoal: Long
  )

  case class Ad(
    _id: Option[ObjectId],
    advertiserObjId: Option[ObjectId],
    advertiserApiId: Option[Long],
    campaignObjId: Option[ObjectId],
    campaignApiId: Option[Long],
    adGroupObjId: Option[ObjectId],
    adGroupApiId: Option[Long],
    apiId: Option[Long],
    description: String,
    displayUrl: String,
    imageUrl: Option[String],
    imageUrlHQ: Option[String],
    landingUrl: String,
    sponsoredBy: String,
    status: Status.Value,
    title: String,
    contentUrl: Option[String],
    videoPrimaryUrl: Option[String],
    impressionTrackingUrls: Option[String],
    callToActionText: Option[CallToAction.Value],
    adName: Option[String]
  )

  case class Bid(
    priceType: PriceType.Value,
    value: Long,
    channel: Channel.Value
  )

  def bidToDBObject(bid: Bid): DBObject = {
    DBObject(
      "priceType" -> bid.priceType,
      "value" -> bid.value,
      "channel" -> bid.channel.toString
    )
  }

  def dboToBid(dbo: DBObject): Bid = {
    Bid(
      priceType = PriceType.withName(dbo.as[String]("priceType")),
      value = dbo.as[Long]("value"),
      channel = Channel.withName(dbo.as[String]("channel"))
    )
  }

  case class AdParamValue(
    paramIndex: Int,
    insertionText: String
  )

  def adParamValueToDBObject(apv: AdParamValue): DBObject = {
    DBObject(
      "paramIndex" -> apv.paramIndex,
      "insertionText" -> apv.insertionText
    )
  }

  def dboToAdParamValue(dbo: DBObject): AdParamValue = {
    AdParamValue(
      paramIndex = dbo.as[Int]("paramIndex"),
      insertionText = dbo.as[String]("insertionText")
    )
  }

  case class BidSet(
    bids: List[Bid]
  )

  def dboToBidSet(dbo: DBObject): BidSet = {
    BidSet(
      bids = dbo.as[List[DBObject]]("bids").map(dboToBid)
    )
  }

  def bidSetToDBObject(bs: BidSet): DBObject = {
    DBObject(
      "bids" -> bs.bids.map(bidToDBObject)
    )
  }

  case class Keyword(
    _id: Option[ObjectId],
    advertiserObjId: Option[ObjectId],
    advertiserApiId: Option[Long],
    campaignObjId: Option[ObjectId],
    campaignApiId: Option[Long],
    adGroupObjId: Option[ObjectId],
    adGroupApiId: Option[Long],
    bidSet: Option[BidSet],
    exclude: Boolean,
    apiId: Long,
    matchType: MatchType.Value,
    parentType: ParentType.Value,
    status: Status.Value,
    value: String,
    adParamValues: Option[List[AdParamValue]],
    landingUrl: Option[String]
  )

  def keywordToDBObject(keyword: Keyword) = DBObject(
    "_id" -> keyword._id,
    "advertiserObjId" -> keyword.advertiserObjId,
    "advertiserApiId" -> keyword.advertiserApiId,
    "campaignObjId" -> keyword.campaignObjId,
    "campaignApiId" -> keyword.campaignApiId,
    "adGroupObjId" -> keyword.adGroupObjId,
    "adGroupApiId" -> keyword.adGroupApiId,
    "bidSet" -> keyword.bidSet,
    "exclude" -> keyword.exclude,
    "apiId" -> keyword.apiId,
    "matchType" -> keyword.matchType,
    "parentType" -> keyword.parentType,
    "status" -> keyword.status,
    "value" -> keyword.value,
    "adParamValues" -> keyword.adParamValues,
    "landingUrl" -> keyword.landingUrl
  )

  def dboToKeyword(dbo: DBObject): Keyword = {
    Keyword(
      _id = dbo._id,
      advertiserObjId = dbo.as[Option[ObjectId]]("advertiserObjId"),
      advertiserApiId = dbo.as[Option[Long]]("advertiserApiId"),
      campaignObjId = dbo.as[Option[ObjectId]]("campaignObjId"),
      campaignApiId = dbo.as[Option[Long]]("campaignApiId"),
      adGroupObjId = dbo.as[Option[ObjectId]]("adGroupObjId"),
      adGroupApiId = dbo.as[Option[Long]]("adGroupApiId"),
      bidSet = dbo.as[Option[DBObject]]("bidSet") match {
        case Some(x) =>
          Some(dboToBidSet(x))
        case _ =>
          None
      },
      exclude = dbo.as[Boolean]("exclude"),
      apiId = dbo.as[Long]("apiId"),
      matchType = MatchType.withName(dbo.as[String]("matchType")),
      parentType = ParentType.withName(dbo.as[String]("parentType")),
      status = Status.withName(dbo.as[String]("status")),
      value = dbo.as[String]("value"),
      adParamValues = dbo.as[Option[List[DBObject]]]("adParamValues") match {
        case Some(x) =>
          Some(x.map(dboToAdParamValue))
        case _ =>
          None
      },
      landingUrl = dbo.as[Option[String]]("landingUrl")
    )
  }

  def dboToAd(dbo: DBObject): Ad = {
    Ad(
      _id = dbo._id,
      advertiserObjId = dbo.as[Option[ObjectId]]("advertiserObjId"),
      advertiserApiId = dbo.as[Option[Long]]("advertiserApiId"),
      campaignObjId = dbo.as[Option[ObjectId]]("campaignObjId"),
      campaignApiId = dbo.as[Option[Long]]("campaignApiId"),
      adGroupObjId = dbo.as[Option[ObjectId]]("adGroupObjId"),
      adGroupApiId = dbo.as[Option[Long]]("adGroupApiId"),
      apiId = dbo.as[Option[Long]]("apiId"),
      description = dbo.as[String]("description"),
      displayUrl = dbo.as[String]("displayUrl"),
      imageUrl = dbo.as[Option[String]]("imageUrl"),
      imageUrlHQ = dbo.as[Option[String]]("imageUrlHQ"),
      landingUrl = dbo.as[String]("landingUrl"),
      sponsoredBy = dbo.as[String]("sponsoredBy"),
      status = Status.withName(dbo.as[String]("status")),
      title = dbo.as[String]("title"),
      contentUrl = dbo.as[Option[String]]("contentUrl"),
      videoPrimaryUrl = dbo.as[Option[String]]("videoPrimaryUrl"),
      impressionTrackingUrls = dbo.as[Option[String]]("impressionTrackingUrls"),
      callToActionText = dbo.as[Option[String]]("callToActionText") match {
        case Some(cta) =>
          Some(CallToAction.withName(cta))
        case _ =>
          None
      },
      adName = dbo.as[Option[String]]("adName")
    )
  }

  def AdToDBObject(ad: Ad): DBObject = {
    DBObject(
      "_id" -> ad._id,
      "advertiserObjId" -> ad.advertiserObjId,
      "advertiserApiId" -> ad.advertiserApiId,
      "campaignObjId" -> ad.campaignObjId,
      "campaignApiId" -> ad.campaignApiId,
      "adGroupObjId" -> ad.adGroupObjId,
      "adGroupApiId" -> ad.adGroupApiId,
      "apiId" -> ad.apiId,
      "description" -> ad.description,
      "displayUrl" -> ad.displayUrl,
      "imageUrl" -> ad.imageUrl,
      "imageUrlHQ" -> ad.imageUrlHQ,
      "landingUrl" -> ad.landingUrl,
      "sponsoredBy" -> ad.sponsoredBy,
      "status" -> ad.status.toString,
      "title" -> ad.title,
      "contentUrl" -> ad.contentUrl,
      "videoPrimaryUrl" -> ad.videoPrimaryUrl,
      "impressionTrackingUrls" -> ad.impressionTrackingUrls,
      "callToActionText" -> (ad.callToActionText match {
        case Some(cta) =>
          Some(cta.toString)
        case _ =>
          None
      }),
      "adName" -> ad.adName
    )
  }

  def adGroupToDBObject(adGroup: AdGroup): DBObject = {
    DBObject(
      "_id" -> adGroup._id,
      "advertiserObjId" -> adGroup.advertiserObjId,
      "advertiserApiId" -> adGroup.advertiserApiId,
      "campaignObjId" -> adGroup.campaignObjId,
      "campaignApiId" -> adGroup.campaignApiId,
      "adGroupName" -> adGroup.adGroupName,
      "bidSet" -> bidSetToDBObject(adGroup.bidSet),
      "apiId" -> adGroup.apiId,
      "status" -> adGroup.status.toString,
      "startDateStr" -> adGroup.startDateStr,
      "endDateStr" -> adGroup.endDateStr,
      "advancedGeoPos" -> adGroup.advancedGeoPos.toString,
      "advancedGeoNeg" -> adGroup.advancedGeoNeg.toString,
      "biddingStrategy" -> (adGroup.biddingStrategy match {
        case Some(bs) =>
          Some(bs.toString)
        case _ =>
          None
      }),
      "epcaGoal" -> adGroup.epcaGoal
    )
  }

  def campaignToDBObject(campaign: Campaign): DBObject = {
    DBObject(
      "_id" -> campaign._id,
      "advertiserObjId" -> campaign.advertiserObjId,
      "advertiserApiId" -> campaign.advertiserApiId,
      "budget" -> campaign.budget,
      "budgetType" -> (campaign.budgetType match {
        case Some(bt) =>
          Some(bt.toString)
        case _ =>
          None
      }),
      "campaignName" -> campaign.campaignName,
      "channel" -> campaign.channel.toString,
      "apiId" -> campaign.apiId,
      "language" -> campaign.language,
      "objective" -> campaign.objective,
      "status" -> campaign.status.toString,
      "isPartnerNetwork" -> campaign.isPartnerNetwork.toString,
      "defaultLandingUrl" -> campaign.defaultLandingUrl,
      "trackingPartner" -> campaign.trackingPartner,
      "advancedGeoPos" -> campaign.advancedGeoPos.toString,
      "advancedGeoNeg" -> campaign.advancedGeoNeg.toString
    )
  }

  def advertiserToDBObject(advertiser: Advertiser): DBObject = {
    DBObject(
      "_id" -> advertiser._id,
      "apiId" -> advertiser.apiId,
      "advertiserName" -> advertiser.advertiserName,
      "timezone" -> advertiser.timezone,
      "currency" -> advertiser.currency,
      "status" -> advertiser.status,
      "billingCountry" -> advertiser.billingCountry,
      "webSiteUrl" -> advertiser.webSiteUrl,
      "lastUpdateDate" -> advertiser.lastUpdateDate,
      "bookingCountry" -> advertiser.bookingCountry,
      "language" -> advertiser.language,
      "createdDate" -> advertiser.createdDate
    )
  }

  def dboToAdvertiser(dbo: DBObject): Advertiser = {
    Advertiser(
      _id = dbo._id,
      apiId = dbo.as[Long]("apiId"),
      advertiserName = dbo.as[String]("advertiserName"),
      timezone = dbo.as[String]("timezone"),
      currency = dbo.as[String]("currency"),
      status = dbo.as[String]("status"),
      billingCountry = dbo.as[String]("billingCountry"),
      webSiteUrl = dbo.as[String]("webSiteUrl"),
      lastUpdateDate = dbo.as[Long]("lastUpdateDate"),
      bookingCountry = dbo.as[String]("bookingCountry"),
      language = dbo.as[String]("language"),
      createdDate = dbo.as[Long]("createdDate")
    )
  }

  def dboToCampaign(dbo: DBObject): Campaign = {
    Campaign(
      _id = dbo._id,
      advertiserObjId = dbo.as[Option[ObjectId]]("advertiserObjId"),
      advertiserApiId = dbo.as[Option[Long]]("advertiserApiId"),
      budget = dbo.as[Option[Double]]("budget"),
      budgetType = dbo.as[Option[String]]("budgetType") match {
        case Some(bt) =>
          Some(BudgetType.withName(bt))
        case _ =>
          None
      },
      campaignName = dbo.as[String]("campaignName"),
      channel = Some(Channel.withName(dbo.as[String]("channel"))),
      apiId = Some(dbo.as[Long]("apiId")),
      language = Some(dbo.as[String]("language")),
      objective = Some(dbo.as[String]("objective")),
      status = Status.withName(dbo.as[String]("status")),
      isPartnerNetwork = Some(Bool.withName(dbo.as[String]("isPartnerNetwork"))),
      defaultLandingUrl = Some(dbo.as[String]("defaultLandingUrl")),
      trackingPartner = Some(dbo.as[String]("trackingPartner")),
      appLocale = Some(dbo.as[String]("appLocale")),
      advancedGeoPos = Some(AdvancedGeoPos.withName(dbo.as[String]("advancedGeoPos"))),
      advancedGeoNeg = Some(AdvancedGeoNeg.withName(dbo.as[String]("advancedGeoNeg")))
    )
  }

  def dboToAdGroup(dbo: DBObject): AdGroup = {
    AdGroup(
      _id = dbo._id,
      advertiserObjId = Some(dbo.as[ObjectId]("advertiserObjId")),
      advertiserApiId = Some(dbo.as[Long]("advertiserApiId")),
      campaignObjId = Some(dbo.as[ObjectId]("campaignObjId")),
      campaignApiId = Some(dbo.as[Long]("campaignApiId")),
      adGroupName = dbo.as[String]("adGroupName"),
      bidSet = dboToBidSet(dbo.as[DBObject]("bidSet")),
      apiId = Some(dbo.as[Long]("apiId")),
      status = Status.withName(dbo.as[String]("status")),
      startDateStr = dbo.as[String]("startDateStr"),
      endDateStr = dbo.as[String]("endDateStr"),
      advancedGeoPos = Some(AdvancedGeoPos.withName(dbo.as[String]("advancedGeoPos"))),
      advancedGeoNeg = Some(AdvancedGeoNeg.withName(dbo.as[String]("advancedGeoNeg"))),
      biddingStrategy = dbo.as[Option[String]]("biddingStrategy") match {
        case Some(bs) =>
          Some(BiddingStrategy.withName(bs))
        case _ =>
          None
      },
      epcaGoal = dbo.as[Long]("epcaGoal")
    )
  }
}