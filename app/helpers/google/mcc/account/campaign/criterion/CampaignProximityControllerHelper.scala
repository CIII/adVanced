package helpers.google.mcc.account.campaign.criterion

import com.mongodb.casbah.Imports._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._

object CampaignProximityControllerHelper {
  case class CampaignProximityForm(
    var parent: controllers.Google.CampaignCriterionParent,
    var apiId: Option[Long],
    var isNegative: Option[Boolean],
    var latitudeInMicroDegrees: Option[Int],
    var longitudeInMicroDegrees: Option[Int],
    var radiusDistanceUnits: String,
    var radiusInUnits: Double,
    var streetAddress: Option[String],
    var streetAddress2: Option[String],
    var cityName: Option[String],
    var provinceCode: Option[String],
    var provinceName: Option[String],
    var postalCode: Option[String],
    var countryCode: Option[String],
    var bidModifier: Option[Double]
  )

  def dboToCampaignProximityForm(dbo: DBObject): CampaignProximityForm = {
    CampaignProximityForm(
      parent=controllers.Google.dboToCampaignCriterionParent(dbo.getAs[DBObject]("parent").get),
      apiId=dbo.getAsOrElse[Option[Long]]("apiId", None),
      isNegative=dbo.getAsOrElse[Option[Boolean]]("isNegative", None),
      latitudeInMicroDegrees=dbo.getAsOrElse[Option[Int]]("latitudeInMicroDegrees", None),
      longitudeInMicroDegrees=dbo.getAsOrElse[Option[Int]]("longitudeInMicroDegrees", None),
      radiusDistanceUnits=dbo.getAsOrElse[String]("radiusDistanceUnits", ""),
      radiusInUnits=dbo.getAsOrElse[Double]("radiusInUnits", 0.0),
      streetAddress=dbo.getAsOrElse[Option[String]]("streetAddress", None),
      streetAddress2=dbo.getAsOrElse[Option[String]]("streetAddress2", None),
      cityName=dbo.getAsOrElse[Option[String]]("cityName", None),
      provinceCode=dbo.getAsOrElse[Option[String]]("provinceCode", None),
      provinceName=dbo.getAsOrElse[Option[String]]("provinceName", None),
      postalCode=dbo.getAsOrElse[Option[String]]("postalCode", None),
      countryCode=dbo.getAsOrElse[Option[String]]("countryCode", None),
      bidModifier=dbo.getAsOrElse[Option[Double]]("bidModifier", None)
    )
  }

  def campaignProximityFormToDbo(cpf: CampaignProximityForm): DBObject = {
    DBObject(
      "parent" -> controllers.Google.campaignCriterionParentToDbo(cpf.parent),
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
