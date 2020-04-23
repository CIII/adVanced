package sync.shared

import java.text.SimpleDateFormat

import Shared.Shared._
import akka.actor.Props
import com.google.api.ads.adwords.axis.v201609.cm._
import com.google.api.ads.adwords.axis.v201609.mcm.{Customer, ManagedCustomer}
import com.google.api.ads.adwords.lib.selectorfields.v201609.cm._
import com.microsoft.bingads.customermanagement.{Account, AccountInfoWithCustomerData}
import com.mongodb.casbah.Imports._
import models.mongodb.facebook.Facebook._
import models.mongodb.google.Google.Mcc
import models.mongodb.yahoo.Yahoo.{Advertiser, ApiAccount}
import org.joda.time.DateTime
import sync.facebook.ads.FacebookMarketingHelper
import sync.facebook.business.ad_study._
import sync.google.adwords.AdWordsHelper
import sync.google.process.management.mcc.account.bidding_strategy.BiddingStrategyActor
import sync.google.process.management.mcc.account.budget.BudgetActor
import sync.google.process.management.mcc.account.campaign.adgroup.ad.AdGroupAdActor
import sync.google.process.management.mcc.account.campaign.adgroup.criterion.keyword.AdGroupCriterionActor
import sync.google.process.management.mcc.account.campaign.adgroup.{AdGroupActor, AdGroupBidModifierActor}
import sync.google.process.management.mcc.account.campaign.criterion.CampaignCriterionActor
import sync.google.process.management.mcc.account.campaign.CampaignActor
import sync.google.process.management.mcc.account.CustomerActor
import sync.msn.bingads.BingAdsHelper
import sync.yahoo.gemini.GeminiHelper
import sync.yahoo.process._
import sync.yahoo.process.advertiser.AdvertiserActor
import com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionDateRangeType

import scala.collection.immutable.List

object Google {

  def googleDateFormatter = new SimpleDateFormat("yyyyMMdd")

  def googleDateToDateTime(dateRangeType: ReportDefinitionDateRangeType): DateTime = DateTime.now.minusDays(dateRangeType match {
    case ReportDefinitionDateRangeType.TODAY => 0
    case ReportDefinitionDateRangeType.YESTERDAY => 1
    case ReportDefinitionDateRangeType.LAST_7_DAYS => 7
    case ReportDefinitionDateRangeType.LAST_14_DAYS => 14
    case ReportDefinitionDateRangeType.LAST_30_DAYS => 30
    case ReportDefinitionDateRangeType.ALL_TIME => 730
  })

  lazy val landingPageTag = "{lpurl}"

  def campaignFields = List(
    CampaignField.AdServingOptimizationStatus,
    CampaignField.AdvertisingChannelType,
    CampaignField.Amount,
    CampaignField.BidCeiling,
    CampaignField.BidType,
    CampaignField.BiddingStrategyId,
    CampaignField.BiddingStrategyName,
    CampaignField.BiddingStrategyType,
    CampaignField.BudgetId,
    CampaignField.BudgetName,
    CampaignField.BudgetReferenceCount,
    CampaignField.BudgetStatus,
    CampaignField.DeliveryMethod,
    CampaignField.Eligible,
    CampaignField.EndDate,
    CampaignField.EnhancedCpcEnabled,
    CampaignField.FrequencyCapMaxImpressions,
    CampaignField.Id,
    CampaignField.IsBudgetExplicitlyShared,
    CampaignField.Labels,
    CampaignField.Level,
    CampaignField.Name,
    CampaignField.PricingMode,
    CampaignField.RejectionReasons,
    CampaignField.ServingStatus,
    CampaignField.Settings,
    CampaignField.StartDate,
    CampaignField.Status,
    CampaignField.TargetContentNetwork,
    CampaignField.TargetGoogleSearch,
    CampaignField.TargetPartnerSearchNetwork,
    CampaignField.TargetSearchNetwork,
    CampaignField.TimeUnit,
    CampaignField.TrackingUrlTemplate,
    CampaignField.UrlCustomParameters
  )

  def campaignCriterionFields = List(
    CampaignCriterionField.Address,
    CampaignCriterionField.AgeRangeType,
    CampaignCriterionField.BidModifier,
    CampaignCriterionField.CampaignId,
    CampaignCriterionField.CarrierCountryCode,
    CampaignCriterionField.CarrierName,
    CampaignCriterionField.ContentLabelType,
    CampaignCriterionField.CriteriaType,
    CampaignCriterionField.DayOfWeek,
    CampaignCriterionField.DeviceName,
    CampaignCriterionField.DeviceType,
    CampaignCriterionField.Dimensions,
    CampaignCriterionField.DisplayName,
    CampaignCriterionField.DisplayType,
    CampaignCriterionField.EndHour,
    CampaignCriterionField.EndMinute,
    CampaignCriterionField.GenderType,
    CampaignCriterionField.GeoPoint,
    CampaignCriterionField.Id,
    CampaignCriterionField.IsNegative,
    CampaignCriterionField.KeywordMatchType,
    CampaignCriterionField.LanguageCode,
    CampaignCriterionField.LanguageName,
    CampaignCriterionField.LocationName,
    CampaignCriterionField.ManufacturerName,
    CampaignCriterionField.MatchingFunction,
    CampaignCriterionField.MobileAppCategoryId,
    CampaignCriterionField.OperatingSystemName,
    CampaignCriterionField.OperatorType,
    CampaignCriterionField.OsMajorVersion,
    CampaignCriterionField.OsMinorVersion,
    CampaignCriterionField.Path,
    CampaignCriterionField.PlacementUrl,
    CampaignCriterionField.PlatformName,
    CampaignCriterionField.RadiusDistanceUnits,
    CampaignCriterionField.RadiusInUnits,
    CampaignCriterionField.StartHour,
    CampaignCriterionField.StartHour,
    CampaignCriterionField.TargetingStatus,
    CampaignCriterionField.KeywordText,
    CampaignCriterionField.UserInterestId,
    CampaignCriterionField.UserInterestName,
    CampaignCriterionField.UserListId,
    CampaignCriterionField.UserListMembershipStatus,
    CampaignCriterionField.UserListName,
    CampaignCriterionField.VerticalId,
    CampaignCriterionField.VerticalParentId
  )

  def adGroupBidModifierFields = List(
    AdGroupBidModifierField.AdGroupId,
    AdGroupBidModifierField.BidModifier,
    AdGroupBidModifierField.BidModifierSource,
    AdGroupBidModifierField.CampaignId,
    AdGroupBidModifierField.CriteriaType,
    AdGroupBidModifierField.Id,
    AdGroupBidModifierField.PlatformName
  )

  def adGroupCriterionFields = List(
    AdGroupCriterionField.AdGroupId,
    AdGroupCriterionField.AgeRangeType,
    AdGroupCriterionField.ApprovalStatus,
    AdGroupCriterionField.BidModifier,
    AdGroupCriterionField.BidType,
    AdGroupCriterionField.BiddingStrategyId,
    AdGroupCriterionField.BiddingStrategyName,
    AdGroupCriterionField.BiddingStrategySource,
    AdGroupCriterionField.BiddingStrategyType,
    AdGroupCriterionField.CaseValue,
    AdGroupCriterionField.CpcBid,
    AdGroupCriterionField.CpcBidSource,
    AdGroupCriterionField.CpmBid,
    AdGroupCriterionField.CpmBidSource,
    AdGroupCriterionField.CriteriaCoverage,
    AdGroupCriterionField.CriteriaSamples,
    AdGroupCriterionField.CriteriaType,
    AdGroupCriterionField.CriterionUse,
    AdGroupCriterionField.DestinationUrl,
    AdGroupCriterionField.DisapprovalReasons,
    AdGroupCriterionField.DisplayName,
    AdGroupCriterionField.EnhancedCpcEnabled,
    AdGroupCriterionField.FinalAppUrls,
    AdGroupCriterionField.FinalMobileUrls,
    AdGroupCriterionField.FinalUrls,
    AdGroupCriterionField.FirstPageCpc,
    AdGroupCriterionField.GenderType,
    AdGroupCriterionField.Id,
    AdGroupCriterionField.KeywordMatchType,
    AdGroupCriterionField.Labels,
    AdGroupCriterionField.MobileAppCategoryId,
    AdGroupCriterionField.Parameter,
    AdGroupCriterionField.ParentCriterionId,
    AdGroupCriterionField.PartitionType,
    AdGroupCriterionField.Path,
    AdGroupCriterionField.PlacementUrl,
    AdGroupCriterionField.QualityScore,
    AdGroupCriterionField.Status,
    AdGroupCriterionField.SystemServingStatus,
    AdGroupCriterionField.TopOfPageCpc,
    AdGroupCriterionField.TrackingUrlTemplate,
    AdGroupCriterionField.UrlCustomParameters,
    AdGroupCriterionField.UserInterestId,
    AdGroupCriterionField.UserInterestName,
    AdGroupCriterionField.UserListId,
    AdGroupCriterionField.UserListMembershipStatus,
    AdGroupCriterionField.UserListName,
    AdGroupCriterionField.VerticalId,
    AdGroupCriterionField.VerticalParentId
  )

  def adGroupFields = List(
    AdGroupField.BidType,
    AdGroupField.BiddingStrategyId,
    AdGroupField.BiddingStrategyName,
    AdGroupField.BiddingStrategySource,
    AdGroupField.BiddingStrategyType,
    AdGroupField.CampaignId,
    AdGroupField.CampaignName,
    AdGroupField.ContentBidCriterionTypeGroup,
    AdGroupField.CpcBid,
    AdGroupField.CpmBid,
    AdGroupField.EnhancedCpcEnabled,
    AdGroupField.Id,
    AdGroupField.Labels,
    AdGroupField.Name,
    AdGroupField.Settings,
    AdGroupField.Status,
    AdGroupField.TargetCpa,
    AdGroupField.TargetCpaBid,
    AdGroupField.TrackingUrlTemplate,
    AdGroupField.UrlCustomParameters
  )

  def sharedBiddingStrategyFields = List(
    BiddingStrategyField.BiddingScheme,
    BiddingStrategyField.Id,
    BiddingStrategyField.Name,
    BiddingStrategyField.Status,
    BiddingStrategyField.Type
  )

  def budgetFields = List(
    BudgetField.Amount,
    BudgetField.BudgetId,
    BudgetField.BudgetName,
    BudgetField.BudgetReferenceCount,
    BudgetField.BudgetStatus,
    BudgetField.DeliveryMethod,
    BudgetField.IsBudgetExplicitlyShared
  )

  def managedCustomerFields = List(
    ManagedCustomerField.CanManageClients,
    ManagedCustomerField.CurrencyCode,
    ManagedCustomerField.CustomerId,
    ManagedCustomerField.DateTimeZone,
    ManagedCustomerField.Name,
    ManagedCustomerField.TestAccount
  )

  def adGroupAdFields = List(
    AdGroupAdField.AdGroupAdDisapprovalReasons,
    AdGroupAdField.AdGroupAdTrademarkDisapproved,
    AdGroupAdField.AdGroupCreativeApprovalStatus,
    AdGroupAdField.AdGroupId,
    AdGroupAdField.AdvertisingId,
    AdGroupAdField.CreationTime,
    AdGroupAdField.CreativeFinalMobileUrls,
    AdGroupAdField.CreativeFinalUrls,
    AdGroupAdField.CreativeTrackingUrlTemplate,
    AdGroupAdField.CreativeUrlCustomParameters,
    AdGroupAdField.Description1,
    AdGroupAdField.Description1,
    AdGroupAdField.Description2,
    AdGroupAdField.Description2,
    AdGroupAdField.DevicePreference,
    AdGroupAdField.Dimensions,
    AdGroupAdField.DisplayUrl,
    AdGroupAdField.ExpandingDirections,
    AdGroupAdField.FileSize,
    AdGroupAdField.Headline,
    AdGroupAdField.Height,
    AdGroupAdField.Id,
    AdGroupAdField.ImageCreativeName,
    AdGroupAdField.IndustryStandardCommercialIdentifier,
    AdGroupAdField.IsCookieTargeted,
    AdGroupAdField.IsTagged,
    AdGroupAdField.IsUserInterestTargeted,
    AdGroupAdField.Labels,
    AdGroupAdField.MediaId,
    AdGroupAdField.MimeType,
    AdGroupAdField.ReadyToPlayOnTheWeb,
    AdGroupAdField.ReadyToPlayOnTheWeb,
    AdGroupAdField.ReferenceId,
    AdGroupAdField.RichMediaAdCertifiedVendorFormatId,
    AdGroupAdField.RichMediaAdDuration,
    AdGroupAdField.RichMediaAdImpressionBeaconUrl,
    AdGroupAdField.RichMediaAdName,
    AdGroupAdField.RichMediaAdSnippet,
    AdGroupAdField.RichMediaAdSourceUrl,
    AdGroupAdField.RichMediaAdType,
    AdGroupAdField.SourceUrl,
    AdGroupAdField.Status,
    AdGroupAdField.TemplateAdDuration,
    AdGroupAdField.TemplateAdName,
    AdGroupAdField.TemplateAdUnionId,
    AdGroupAdField.TemplateElementFieldName,
    AdGroupAdField.TemplateElementFieldText,
    AdGroupAdField.TemplateElementFieldType,
    AdGroupAdField.TemplateId,
    AdGroupAdField.TemplateOriginAdId,
    AdGroupAdField.UniqueName,
    AdGroupAdField.Url,
    AdGroupAdField.Urls,
    AdGroupAdField.VideoTypes,
    AdGroupAdField.Width,
    AdGroupAdField.YouTubeVideoIdString
  )

  case class MccObject(
    mccObjId: ObjectId,
    mcc: Mcc
  )

  case class CustomerObject(
    mccObject: MccObject,
    var customerObjId: Option[ObjectId],
    managedCustomer: ManagedCustomer,
    var customer: Option[Customer]
  )

  case class CampaignObject(
    customerObject: CustomerObject,
    var campaignObjId: Option[ObjectId],
    campaign: Campaign
  )

  case class AdGroupObject(
    campaignObject: CampaignObject,
    var adGroupObjId: Option[ObjectId],
    adGroup: AdGroup
  )

  case class SharedBiddingStrategyObject(
    customerObject: CustomerObject,
    var sharedBiddingStrategyObjId: Option[ObjectId],
    sharedBiddingStrategy: SharedBiddingStrategy
  )

  case class AdGroupBidModifierObject(
    campaignObject: CampaignObject,
    var adGroupBidModifierObjId: Option[ObjectId],
    adGroupBidModifier: AdGroupBidModifier
  )

  case class CampaignCriterionObject(
    campaignObject: CampaignObject,
    var campaignCriterionObjId: Option[ObjectId],
    campaignCriterion: CampaignCriterion
  )

  case class AdGroupAdObject(
    adGroupObject: AdGroupObject,
    var adGroupAdObjId: Option[ObjectId],
    adGroupAd: AdGroupAd
  )

  case class BudgetObject(
    customerObject: CustomerObject,
    var budgetObjId: Option[ObjectId],
    budget: Budget
  )

  case class AdGroupCriterionObject(
    adGroupObject: AdGroupObject,
    var adGroupCriterionObjId: Option[ObjectId],
    adGroupCriterion: AdGroupCriterion
  )

  abstract class GoogleDataPullRequest {
    val pushToExternal: Boolean
  }

  case class GoogleCustomerDataPullRequest(
    var adWordsHelper: Option[AdWordsHelper],
    customerObject: CustomerObject,
    recursivePull: Boolean,
    pushToExternal: Boolean
  ) extends GoogleDataPullRequest

  case class GoogleCampaignDataPullRequest(
    adWordsHelper: AdWordsHelper,
    campaignObject: CampaignObject,
    recursivePull: Boolean,
    pushToExternal: Boolean
  ) extends GoogleDataPullRequest

  case class GoogleAdGroupDataPullRequest(
    adWordsHelper: AdWordsHelper,
    adGroupObject: AdGroupObject,
    recursivePull: Boolean,
    pushToExternal: Boolean
  ) extends GoogleDataPullRequest

  case class GoogleAdGroupBidModifierDataPullRequest(
    adWordsHelper: AdWordsHelper,
    adGroupBidModifierObject: AdGroupBidModifierObject,
    pushToExternal: Boolean
  ) extends GoogleDataPullRequest

  case class GoogleCampaignCriterionDataPullRequest(
    adWordsHelper: AdWordsHelper,
    campaignCriterionObject: CampaignCriterionObject,
    pushToExternal: Boolean
  ) extends GoogleDataPullRequest

  case class GoogleAdGroupCriterionDataPullRequest(
    adWordsHelper: AdWordsHelper,
    adGroupCriterionObject: AdGroupCriterionObject,
    pushToExternal: Boolean
  ) extends GoogleDataPullRequest

  case class GoogleAdGroupAdDataPullRequest(
    adWordsHelper: AdWordsHelper,
    adGroupAdObject: AdGroupAdObject,
    pushToExternal: Boolean
  ) extends GoogleDataPullRequest

  case class GoogleBudgetDataPullRequest(
    adWordsHelper: AdWordsHelper,
    budgetObject: BudgetObject,
    pushToExternal: Boolean
  ) extends GoogleDataPullRequest

  case class GoogleBiddingStrategyDataPullRequest(
    adWordsHelper: AdWordsHelper,
    sharedBiddingStrategyObject: SharedBiddingStrategyObject,
    pushToExternal: Boolean
  ) extends GoogleDataPullRequest
}

object Facebook {
  abstract class FacebookPullRequest{
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
    var campaignObjId: Option[ObjectId],
    campaign: com.facebook.ads.sdk.Campaign
  )

  case class AdSetObject(
    campaignObject: CampaignObject,
    var adSetObjId: Option[ObjectId],
    adSet: com.facebook.ads.sdk.AdSet
  )

  case class AdObject(
    adSetObject: AdSetObject,
    var adObjId: Option[ObjectId],
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

object Yahoo {
  case class ApiAccountObject(
    apiAccountObjId: ObjectId,
    apiAccount: ApiAccount
  )

  case class AdvertiserObject(
    apiAccountObject: ApiAccountObject,
    var advertiserObjId: Option[ObjectId],
    advertiser: Advertiser
  )

  case class AdGroupObject(
    campaignObject: CampaignObject,
    var adGroupObjId: Option[ObjectId],
    adGroup: models.mongodb.yahoo.Yahoo.AdGroup
  )

  case class AdObject(
    adGroupObject: AdGroupObject,
    var adObjId: Option[ObjectId],
    ad: models.mongodb.yahoo.Yahoo.Ad
  )

  abstract class YahooDataPullRequest{
    val pushToExternal: Boolean
  }

  case class YahooAdvertiserDataPullRequest(
    var geminiHelper: Option[GeminiHelper],
    advertiserObject: AdvertiserObject,
    recursivePull: Boolean,
    pushToExternal: Boolean
  ) extends YahooDataPullRequest

  case class CampaignObject(
    advertiserObject: AdvertiserObject,
    var campaignObjId: Option[ObjectId],
    campaign: models.mongodb.yahoo.Yahoo.Campaign
  )

  case class YahooCampaignDataPullRequest(
    geminiHelper: GeminiHelper,
    campaignObject: CampaignObject,
    recursivePull: Boolean,
    pushToExternal: Boolean
  ) extends YahooDataPullRequest

  case class YahooAdGroupDataPullRequest(
    geminiHelper: GeminiHelper,
    adGroupObject: AdGroupObject,
    recursivePull: Boolean,
    pushToExternal: Boolean
  ) extends YahooDataPullRequest

  case class YahooAdDataPullRequest(
    adWordsHelper: AdWordsHelper,
    adObject: AdObject,
    pushToExternal: Boolean
  ) extends YahooDataPullRequest
}

object Msn {
  val addAdGroupsLimit = 1000
  val getAdsByIdsLimit = 20
  val addCampaignsLimit = 1000
  val addAdsLimit = 50
  val addKeywordsLimit = 1000

  case class Bid(
    var amount: Double
  )

  def bidToDbo(b: Bid) = DBObject(
    "amount" -> b.amount
  )

  def dboToBid(dbo: DBObject) = Bid(
    amount=dbo.getAsOrElse[Double]("amount", 0.0)
  )

  case class MsnAccountInfoDataPullRequest(
    bingAdsHelper: BingAdsHelper,
    apiAccountObjId: ObjectId,
    account: Account,
    accountInfoWithCustomerData: AccountInfoWithCustomerData,
    pushToExternal: Boolean
  )
}