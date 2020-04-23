package helpers.google.mcc.account.campaign.adgroup.criterion

import com.mongodb.casbah.Imports._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.{Form, Forms}

import scala.collection.immutable.List

object KeywordControllerHelper {
  case class KeywordForm(
    var parent: controllers.Google.AdGroupCriterionParent,
    var apiId: Option[Long],
    var criterionUse: String,
    var text: String,
    var matchType: String,
    var userStatus: Option[String],
    var systemServingStatus: Option[String],
    var approvalStatus: Option[String],
    var disapprovalReasons: Option[List[String]],
    var destinationUrl: Option[String],
    var finalUrl: Option[String],
    var finalMobileUrl: Option[String],
    var customParameters: Option[List[controllers.Google.CustomParameter]],
    var firstPageCpcAmount: Option[Long],
    var topOfPageCpcAmount: Option[Long],
    var bidModifier: Option[Double]
  )

  def dboToAdGroupKeywordForm(dbo: DBObject) = KeywordForm(
    parent=controllers.Google.dboToAdGroupCriterionParent(dbo.getAs[DBObject]("parent").get),
    apiId=dbo.getAsOrElse[Option[Long]]("apiId", None),
    criterionUse=dbo.getAsOrElse[String]("criterionUse", ""),
    text=dbo.getAsOrElse[String]("text", ""),
    matchType=dbo.getAsOrElse[String]("matchType", ""),
    userStatus=dbo.getAsOrElse[Option[String]]("userStatus", None),
    systemServingStatus=dbo.getAsOrElse[Option[String]]("systemServingStatus", None),
    approvalStatus=dbo.getAsOrElse[Option[String]]("approvalStatus", None),
    disapprovalReasons=dbo.getAsOrElse[Option[List[String]]]("disapprovalReasons", None),
    destinationUrl=dbo.getAsOrElse[Option[String]]("destinationUrl", None),
    finalUrl=dbo.getAsOrElse[Option[String]]("finalUrl", None),
    finalMobileUrl=dbo.getAsOrElse[Option[String]]("finalMobileUrl", None),
    customParameters=Some(dbo.getAsOrElse[List[DBObject]]("customParameters", List()).map(x =>
      controllers.Google.dboToCustomParameter(x))
    ),
    firstPageCpcAmount=dbo.getAsOrElse[Option[Long]]("firstPageCpcAmount", None),
    topOfPageCpcAmount=dbo.getAsOrElse[Option[Long]]("topOfPageCpcAmount", None),
    bidModifier=dbo.getAsOrElse[Option[Double]]("bidModifier", None)
  )

  def adGroupKeywordFormToDbo(kf: KeywordForm) = DBObject(
    "parent" -> controllers.Google.adGroupCriterionParentToDbo(kf.parent),
    "apiId" -> kf.apiId,
    "criterionUse" -> kf.criterionUse,
    "text" -> kf.text,
    "matchType" -> kf.matchType,
    "userStatus" -> kf.userStatus,
    "systemServingStatus" -> kf.systemServingStatus,
    "approvalStatus" -> kf.approvalStatus,
    "disapprovalReasons" -> kf.disapprovalReasons,
    "destinationUrl" -> kf.destinationUrl,
    "finalUrl" -> kf.finalUrl,
    "finalMobileUrl" -> kf.finalMobileUrl,
    "customParameters" -> kf.customParameters.getOrElse(List()).map(x => controllers.Google.customParameterToDbo(x)),
    "firstPageCpcAmount" -> kf.firstPageCpcAmount,
    "topOfPageCpcAmount" -> kf.topOfPageCpcAmount,
    "bidModifier" -> kf.bidModifier
  )

  def keywordForm: Form[KeywordForm] = Form(
    mapping(
      "parent" -> mapping(
        "mccObjId" -> optional(text),
        "customerApiId" -> optional(longNumber),
        "campaignApiId" -> optional(longNumber),
        "adGroupApiId" -> optional(longNumber)
      )(controllers.Google.AdGroupCriterionParent.apply)(controllers.Google.AdGroupCriterionParent.unapply),
      "apiId" -> optional(longNumber),
      "criterionUse" -> text,
      "text" -> nonEmptyText,
      "matchType" -> nonEmptyText,
      "user_status" -> optional(text),
      "systemServingStatus" -> optional(text),
      "approvalStatus" -> optional(text),
      "disapprovalReasons" -> optional(list(text)),
      "destinationUrl" -> optional(text),
      "finalUrl" -> optional(text),
      "finalMobileUrl" -> optional(text),
      "customParameters" -> optional(
        list(
          mapping(
            "key" -> text,
            "value" -> optional(text)
          )(controllers.Google.CustomParameter.apply)(controllers.Google.CustomParameter.unapply)
        )
      ),
      "firstPageCpcAmount" -> optional(longNumber),
      "topOfPageCpcAmount" -> optional(longNumber),
      "bidModifier" -> optional(Forms.of[Double])
    )(KeywordForm.apply)(KeywordForm.unapply)
  )
}
