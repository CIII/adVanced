package helpers.google.mcc.account.campaign.criterion

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._

object CampaignProximityControllerHelper {
  case class CampaignProximityForm(
    parent: controllers.Google.CampaignCriterionParent,
    apiId: Option[Long],
    isNegative: Option[Boolean],
    latitudeInMicroDegrees: Option[Int],
    longitudeInMicroDegrees: Option[Int],
    radiusDistanceUnits: String,
    radiusInUnits: Double,
    streetAddress: Option[String],
    streetAddress2: Option[String],
    cityName: Option[String],
    provinceCode: Option[String],
    provinceName: Option[String],
    postalCode: Option[String],
    countryCode: Option[String],
    bidModifier: Option[Double]
  )

  def documentToCampaignProximityForm(dbo: Document): CampaignProximityForm = {
    CampaignProximityForm(
      parent=controllers.Google.documentToCampaignCriterionParent(Option(dbo.toBsonDocument.get("parent")).filter(_.isDocument).map(v => Document(v.asDocument())).get),
      apiId=Option(dbo.getLong("apiId")).map(_.toLong),
      isNegative=Option(dbo.getBoolean("isNegative")).map(_.booleanValue()),
      latitudeInMicroDegrees=Option(dbo.getInteger("latitudeInMicroDegrees")).map(_.intValue()),
      longitudeInMicroDegrees=Option(dbo.getInteger("longitudeInMicroDegrees")).map(_.intValue()),
      radiusDistanceUnits=Option(dbo.getString("radiusDistanceUnits")).getOrElse(""),
      radiusInUnits=Option(dbo.getDouble("radiusInUnits")).map(_.toDouble).getOrElse(0.0),
      streetAddress=Option(dbo.getString("streetAddress")),
      streetAddress2=Option(dbo.getString("streetAddress2")),
      cityName=Option(dbo.getString("cityName")),
      provinceCode=Option(dbo.getString("provinceCode")),
      provinceName=Option(dbo.getString("provinceName")),
      postalCode=Option(dbo.getString("postalCode")),
      countryCode=Option(dbo.getString("countryCode")),
      bidModifier=Option(dbo.getDouble("bidModifier")).map(_.toDouble)
    )
  }

  def campaignProximityFormToDocument(cpf: CampaignProximityForm): Document = {
    Document(
      "parent" -> controllers.Google.campaignCriterionParentToDocument(cpf.parent),
      "apiId" -> cpf.apiId,
      "isNegative" -> cpf.isNegative,
      "latitudeInMicroDegrees" -> cpf.latitudeInMicroDegrees,
      "longitudeInMicroDegrees" -> cpf.longitudeInMicroDegrees,
      "radiusDistanceUnits" -> cpf.radiusDistanceUnits,
      "radiusInUnits" -> cpf.radiusInUnits,
      "streetAddress" -> cpf.streetAddress,
      "streetAddress2" -> cpf.streetAddress2,
      "cityName" -> cpf.cityName,
      "provinceCode" -> cpf.provinceCode,
      "provinceName" -> cpf.provinceName,
      "postalCode" -> cpf.postalCode,
      "countryCode" -> cpf.countryCode,
      "bidModifier" -> cpf.bidModifier
    )
  }

  def campaignProximityForm: Form[CampaignProximityForm] = Form(
    mapping(
      "parent" -> mapping(
        "mccObjId" -> optional(text),
        "customerApiId" -> optional(longNumber),
        "campaignApiId" -> optional(longNumber)
      )(controllers.Google.CampaignCriterionParent.apply)(controllers.Google.CampaignCriterionParent.unapply),
      "apiId" -> optional(longNumber),
      "isNegative" -> optional(boolean),
      "latitudeInMicroDegrees" -> optional(number),
      "longitudeInMicroDegrees" -> optional(number),
      "radiusDistanceUnits" -> text,
      "radiusInUnits" -> of[Double],
      "streetAddress" -> optional(text),
      "streetAddress2" -> optional(text),
      "cityName" -> optional(text),
      "provinceCode" -> optional(text),
      "provinceName" -> optional(text),
      "postalCode" -> optional(text),
      "countryCode" -> optional(text),
      "bidModifier" -> optional(of[Double])
    )(CampaignProximityForm.apply)(CampaignProximityForm.unapply)
  )
}
