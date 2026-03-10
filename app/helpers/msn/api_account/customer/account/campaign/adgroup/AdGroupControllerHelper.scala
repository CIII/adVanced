package helpers.msn.api_account.customer.account.campaign.adgroup

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._

import scala.collection.immutable.List
import scala.jdk.CollectionConverters._
import Shared.Shared.jodaDate

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

  def documentToAdGroupForm(dbo: Document) = AdGroupForm(
    apiId=Option(dbo.getLong("apiId")).map(_.toLong),
    name=dbo.getString("name"),
    startDate=Some(new org.joda.time.DateTime(Option(dbo.getLong("startDate")).map(_.toLong).getOrElse(0L))),
    endDate=Some(new org.joda.time.DateTime(Option(dbo.getLong("endDate")).map(_.toLong).getOrElse(0L))),
    adDistribution=Option(dbo.getList("adDistribution", classOf[String])).map(_.asScala.toList).getOrElse(List()),
    adRotationType=dbo.getString("adRotationType"),
    biddingScheme=dbo.getString("biddingScheme"),
    contentMatchBid=Option(dbo.getDouble("contentMatchBid")).map(_.toDouble),
    network=Option(dbo.getString("network")),
    pricingModel=Option(dbo.getString("pricingModel")),
    language=Option(dbo.getString("language")).getOrElse(""),
    status=Option(dbo.getString("status")).getOrElse("")
  )

  def adGroupFormToDocument(agf: AdGroupForm) = Document(
    "apiId" -> agf.apiId,
    "name" -> agf.name,
    "startDate" -> agf.startDate.getOrElse(new org.joda.time.DateTime()).getMillis,
    "endDate" -> agf.endDate.getOrElse(new org.joda.time.DateTime()).getMillis,
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
    )(AdGroupForm.apply)(AdGroupForm.unapply _)
  )
}
