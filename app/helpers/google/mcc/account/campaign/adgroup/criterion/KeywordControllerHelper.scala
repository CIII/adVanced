package helpers.google.mcc.account.campaign.adgroup.criterion

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.{Form, Forms}

import scala.collection.immutable.List
import scala.jdk.CollectionConverters._

object KeywordControllerHelper {
  case class KeywordForm(
    parent: controllers.Google.AdGroupCriterionParent,
    apiId: Option[Long],
    criterionUse: String,
    text: String,
    matchType: String,
    userStatus: Option[String],
    systemServingStatus: Option[String],
    approvalStatus: Option[String],
    disapprovalReasons: Option[List[String]],
    destinationUrl: Option[String],
    finalUrl: Option[String],
    finalMobileUrl: Option[String],
    customParameters: Option[List[controllers.Google.CustomParameter]],
    firstPageCpcAmount: Option[Long],
    topOfPageCpcAmount: Option[Long],
    bidModifier: Option[Double]
  )

  def documentToAdGroupKeywordForm(dbo: Document) = KeywordForm(
    parent=controllers.Google.documentToAdGroupCriterionParent(Option(dbo.toBsonDocument.get("parent")).filter(_.isDocument).map(v => Document(v.asDocument())).get),
    apiId=Option(dbo.getLong("apiId")).map(_.toLong),
    criterionUse=Option(dbo.getString("criterionUse")).getOrElse(""),
    text=Option(dbo.getString("text")).getOrElse(""),
    matchType=Option(dbo.getString("matchType")).getOrElse(""),
    userStatus=Option(dbo.getString("userStatus")),
    systemServingStatus=Option(dbo.getString("systemServingStatus")),
    approvalStatus=Option(dbo.getString("approvalStatus")),
    disapprovalReasons=Option(dbo.getList("disapprovalReasons", classOf[String])).map(_.asScala.toList),
    destinationUrl=Option(dbo.getString("destinationUrl")),
    finalUrl=Option(dbo.getString("finalUrl")),
    finalMobileUrl=Option(dbo.getString("finalMobileUrl")),
    customParameters=Some(Option(dbo.getList("customParameters", classOf[Document])).map(_.asScala.toList).getOrElse(List()).map(x =>
      controllers.Google.documentToCustomParameter(x))
    ),
    firstPageCpcAmount=Option(dbo.getLong("firstPageCpcAmount")).map(_.toLong),
    topOfPageCpcAmount=Option(dbo.getLong("topOfPageCpcAmount")).map(_.toLong),
    bidModifier=Option(dbo.getDouble("bidModifier")).map(_.toDouble)
  )

  def adGroupKeywordFormToDocument(kf: KeywordForm) = Document(
    "parent" -> controllers.Google.adGroupCriterionParentToDocument(kf.parent),
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
    "customParameters" -> kf.customParameters.getOrElse(List()).map(x => controllers.Google.customParameterToDocument(x)),
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
