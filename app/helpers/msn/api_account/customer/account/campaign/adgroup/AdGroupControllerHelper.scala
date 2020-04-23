package helpers.msn.api_account.customer.account.campaign.adgroup

import com.mongodb.casbah.Imports._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._

import scala.collection.immutable.List

object AdGroupControllerHelper {
  case class AdGroupForm(
    apiId: Option[Long],
    name: String,
    startDate: Option[org.joda.time.DateTime],
    endDate: Option[org.joda.time.DateTime],
    adDistribution: List[String],
    adRotationType: String,
    biddingScheme: String,
    contentMatchBid: Option[Double],
    network: Option[String],
    pricingModel: Option[String],
    language: String,
    status: String
  )

  def dboToAdGroupForm(dbo: DBObject) = AdGroupForm(
    apiId=dbo.getAsOrElse[Option[Long]]("apiId", None),
    name=dbo.getAs[String]("name").get,
    startDate=Some(new org.joda.time.DateTime(dbo.getAsOrElse[Option[Long]]("startDate", None).getOrElse(0))),
    endDate=Some(new org.joda.time.DateTime(dbo.getAsOrElse[Option[Long]]("endDate", None).getOrElse(0))),
    adDistribution=dbo.getAsOrElse[List[String]]("adDistribution", List()),
    adRotationType=dbo.getAs[String]("adRotationType").get,
    biddingScheme=dbo.getAs[String]("biddingScheme").get,
    contentMatchBid=dbo.getAsOrElse[Option[Double]]("contentMatchBid", None),
    network=dbo.getAsOrElse[Option[String]]("network", None),
    pricingModel=dbo.getAs[String]("pricingModel"),
    language=dbo.getAsOrElse[String]("language", ""),
    status=dbo.getAsOrElse[String]("status", "")
  )

  def adGroupFormToDbo(agf: AdGroupForm) = DBObject(
    "apiId" -> agf.apiId,
    "name" -> agf.name,
    "startDate" -> agf.startDate.getOrElse(new org.joda.time.DateTime()),
    "endDate" -> agf.endDate.getOrElse(new org.joda.time.DateTime()),
    "adDistribution" -> agf.adDistribution,
    "adRotationType" -> agf.adRotationType,
    "biddingScheme" -> agf.biddingScheme,
    "contentMatchBid" -> agf.contentMatchBid,
    "network" -> agf.network,
    "pricingModel" -> agf.pricingModel,
    "language" -> agf.language,
    "status" -> agf.status
  )

  def adGroupForm: Form[AdGroupForm] = Form(
    mapping(
      "id" -> optional(longNumber),
      "name" -> nonEmptyText,
      "startDate" -> optional(jodaDate),
      "endDate" -> optional(jodaDate),
      "ad_distribution" -> list(text),
      "ad_rotation_type" -> text,
      "bidding_scheme" -> text,
      "content_match_bid" -> optional(of[Double]),
      "network" -> optional(text),
      "pricing_model" -> optional(text),
      "language" -> nonEmptyText,
      "status" -> nonEmptyText
    )(AdGroupForm.apply)(AdGroupForm.unapply)
  )
}
