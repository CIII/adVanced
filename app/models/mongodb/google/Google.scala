package models.mongodb.google

import Shared.Shared._
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON

import scala.reflect.internal.util.ScalaClassLoader
import scala.reflect.{ClassTag, classTag}

object Google {
  def googleMccCollection = advancedCollection("google_mcc")
  def googleCustomerCollection = advancedCollection("google_customer")
  def googleCampaignCollection = advancedCollection("google_campaign")
  def googleAdGroupCollection = advancedCollection("google_adgroup")
  def googleAdCollection  = advancedCollection("google_ad")
  def googleCriterionCollection = advancedCollection("google_criterion")
  def googleBudgetCollection = advancedCollection("google_budget")
  def googleBiddingStrategyCollection = advancedCollection("google_bidding_strategy")
  def googleBidModifierCollection = advancedCollection("google_bid_modifier")
  def googleReportCollection(
    reportType: com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType
  ) = advancedCollection(
    "google_%s".format(reportType.value.toLowerCase)
  )

  def dboToGoogleEntity[T: ClassTag](dbo: DBObject, listKey: String, objectKey: Option[String]): T = {
    gson.fromJson(
      JSON.serialize(
        objectKey match {
          case Some(key) =>
            dbo.as[DBObject](listKey).as[DBObject]("object").expand[DBObject](key)
          case None =>
            dbo.as[DBObject](listKey).as[DBObject]("object")
        }
      ),
      classTag[T].runtimeClass.asInstanceOf[Class[T]]
    )
  }

  def mccToDBObject(mcc: Mcc): DBObject = {
    DBObject(
      "_id" -> mcc._id,
      "developerToken" -> mcc.developerToken,
      "name" -> mcc.name,
      "oAuthClientId" -> mcc.oAuthClientId,
      "oAuthClientSecret" -> mcc.oAuthClientSecret,
      "oAuthRefreshToken" -> mcc.oAuthRefreshToken
    )
  }

  def dboToMcc(dbo: DBObject): Mcc = {
    Mcc(
      _id = dbo._id,
      name = dbo.as[String]("name"),
      developerToken = dbo.as[String]("developerToken"),
      oAuthClientId = dbo.as[String]("oAuthClientId"),
      oAuthClientSecret = dbo.as[String]("oAuthClientSecret"),
      oAuthRefreshToken = dbo.as[String]("oAuthRefreshToken")
    )
  }

  case class Mcc (
    _id: Option[ObjectId],
    var name: String,
    var developerToken: String,
    var oAuthClientId: String,
    var oAuthClientSecret: String,
    var oAuthRefreshToken: String
  )
}