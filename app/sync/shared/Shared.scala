package sync.shared

import java.text.SimpleDateFormat

import org.bson.types.ObjectId
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import models.mongodb.facebook.Facebook._
import models.mongodb.google.Google.Mcc
import org.joda.time.DateTime
import sync.facebook.ads.FacebookMarketingHelper
import sync.facebook.business.ad_study._
import sync.google.adwords.GoogleAdsService
import sync.msn.bingads.BingAdsHelper

/**
 * Shared data structures for sync operations.
 *
 * TODO: The Google-related case classes need to be updated when the Google Ads API v18
 * migration is complete. The old AdWords API types (Campaign, AdGroup, etc.) should be
 * replaced with Google Ads API resource types or Document-based representations.
 */
object Google {

  def googleDateFormatter = new SimpleDateFormat("yyyyMMdd")

  /**
   * TODO: Replace with Google Ads API v18 date range handling.
   * The old ReportDefinitionDateRangeType enum has been removed.
   */
  def googleDateToDateTime(dateRangeType: String): DateTime = DateTime.now.minusDays(dateRangeType match {
    case "TODAY" => 0
    case "YESTERDAY" => 1
    case "LAST_7_DAYS" => 7
    case "LAST_14_DAYS" => 14
    case "LAST_30_DAYS" => 30
    case "ALL_TIME" => 730
  })

  lazy val landingPageTag = "{lpurl}"

  /**
   * TODO: Replace these case classes with Google Ads API v18 resource representations.
   * The old AdWords API types (Campaign, AdGroup, etc.) are no longer available.
   * Consider using Document-based representations until the Google Ads API is integrated.
   */
  case class MccObject(
    mccObjId: ObjectId,
    mcc: Mcc
  )

  case class CustomerObject(
    mccObject: MccObject,
    customerObjId: Option[ObjectId],
    customerDoc: Option[Document]
  )

  case class CampaignObject(
    customerObject: CustomerObject,
    campaignObjId: Option[ObjectId],
    campaignDoc: Document
  )

  case class AdGroupObject(
    campaignObject: CampaignObject,
    adGroupObjId: Option[ObjectId],
    adGroupDoc: Document
  )

  case class SharedBiddingStrategyObject(
    customerObject: CustomerObject,
    sharedBiddingStrategyObjId: Option[ObjectId],
    sharedBiddingStrategyDoc: Document
  )

  case class AdGroupBidModifierObject(
    campaignObject: CampaignObject,
    adGroupBidModifierObjId: Option[ObjectId],
    adGroupBidModifierDoc: Document
  )

  case class CampaignCriterionObject(
    campaignObject: CampaignObject,
    campaignCriterionObjId: Option[ObjectId],
    campaignCriterionDoc: Document
  )

  case class AdGroupAdObject(
    adGroupObject: AdGroupObject,
    adGroupAdObjId: Option[ObjectId],
    adGroupAdDoc: Document
  )

  case class BudgetObject(
    customerObject: CustomerObject,
    budgetObjId: Option[ObjectId],
    budgetDoc: Document
  )

  case class AdGroupCriterionObject(
    adGroupObject: AdGroupObject,
    adGroupCriterionObjId: Option[ObjectId],
    adGroupCriterionDoc: Document
  )

  abstract class GoogleDataPullRequest {
    val pushToExternal: Boolean
  }

  case class GoogleCustomerDataPullRequest(
    customerObject: CustomerObject,
    recursivePull: Boolean,
    pushToExternal: Boolean
  ) extends GoogleDataPullRequest

  case class GoogleCampaignDataPullRequest(
    campaignObject: CampaignObject,
    recursivePull: Boolean,
    pushToExternal: Boolean
  ) extends GoogleDataPullRequest

  case class GoogleAdGroupDataPullRequest(
    adGroupObject: AdGroupObject,
    recursivePull: Boolean,
    pushToExternal: Boolean
  ) extends GoogleDataPullRequest

  case class GoogleAdGroupBidModifierDataPullRequest(
    adGroupBidModifierObject: AdGroupBidModifierObject,
    pushToExternal: Boolean
  ) extends GoogleDataPullRequest

  case class GoogleCampaignCriterionDataPullRequest(
    campaignCriterionObject: CampaignCriterionObject,
    pushToExternal: Boolean
  ) extends GoogleDataPullRequest

  case class GoogleAdGroupCriterionDataPullRequest(
    adGroupCriterionObject: AdGroupCriterionObject,
    pushToExternal: Boolean
  ) extends GoogleDataPullRequest

  case class GoogleAdGroupAdDataPullRequest(
    adGroupAdObject: AdGroupAdObject,
    pushToExternal: Boolean
  ) extends GoogleDataPullRequest

  case class GoogleBudgetDataPullRequest(
    budgetObject: BudgetObject,
    pushToExternal: Boolean
  ) extends GoogleDataPullRequest

  case class GoogleBiddingStrategyDataPullRequest(
    sharedBiddingStrategyObject: SharedBiddingStrategyObject,
    pushToExternal: Boolean
  ) extends GoogleDataPullRequest
}

object Facebook {
  abstract class FacebookPullRequest {
    val pushToExternal: Boolean
  }

  case class FacebookSplitTestDataPullRequest(
    fbSplitTest: FacebookSplitTest,
    fbBusinessHelper: FacebookAdStudyHelper,
    pushToExternal: Boolean
  ) extends FacebookPullRequest

  case class ApiAccountObject(
    apiAccountObjId: ObjectId,
    apiAccount: models.mongodb.facebook.Facebook.FacebookApiAccount
  )

  case class CampaignObject(
    apiAccountObject: ApiAccountObject,
    campaignObjId: Option[ObjectId],
    campaign: com.facebook.ads.sdk.Campaign
  )

  case class AdSetObject(
    campaignObject: CampaignObject,
    adSetObjId: Option[ObjectId],
    adSet: com.facebook.ads.sdk.AdSet
  )

  case class AdObject(
    adSetObject: AdSetObject,
    adObjId: Option[ObjectId],
    ad: com.facebook.ads.sdk.Ad
  )

  abstract class DataPullRequest {
    val pushToExternal: Boolean
  }

  case class FacebookCampaignDataPullRequest(
    marketinghelper: FacebookMarketingHelper,
    apiAccountObject: ApiAccountObject,
    recursivePull: Boolean,
    pushToExternal: Boolean
  ) extends DataPullRequest

  case class FacebookAdSetDataPullRequest(
    marketinghelper: FacebookMarketingHelper,
    campaignObject: CampaignObject,
    recursivePull: Boolean,
    pushToExternal: Boolean
  ) extends DataPullRequest

  case class FacebookAdDataPullRequest(
    marketinghelper: FacebookMarketingHelper,
    adSetObject: AdSetObject,
    recursivePull: Boolean,
    pushToExternal: Boolean
  ) extends DataPullRequest
}

object Msn {
  val addAdGroupsLimit = 1000
  val getAdsByIdsLimit = 20
  val addCampaignsLimit = 1000
  val addAdsLimit = 50
  val addKeywordsLimit = 1000

  case class Bid(
    amount: Double
  )

  def bidToDocument(b: Bid): Document = Document(
    "amount" -> b.amount
  )

  def documentToBid(doc: Document): Bid = Bid(
    amount = Option(doc.getDouble("amount")).map(_.doubleValue()).getOrElse(0.0)
  )

  case class MsnAccountInfoDataPullRequest(
    bingAdsHelper: BingAdsHelper,
    apiAccountObjId: ObjectId,
    account: com.microsoft.bingads.v13.customermanagement.AdvertiserAccount,
    accountInfoWithCustomerData: com.microsoft.bingads.v13.customermanagement.AccountInfoWithCustomerData,
    pushToExternal: Boolean
  )
}
