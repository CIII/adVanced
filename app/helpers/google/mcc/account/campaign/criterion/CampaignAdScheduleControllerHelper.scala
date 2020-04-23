package helpers.google.mcc.account.campaign.criterion

import com.mongodb.casbah.Imports._
import models.mongodb.Utilities
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._

object CampaignAdScheduleControllerHelper {

  case class CampaignAdScheduleForm(
    var parent: controllers.Google.CampaignCriterionParent,
    var apiId: Option[Long],
    var isNegative: Option[Boolean],
    var dayOfWeek: String,
    var startHour: Int,
    var startMinute: String,
    var endHour: Int,
    var endMinute: String,
    var bidModifier: Double
  )

  def dboToCampaignAdScheduleForm(dbo: DBObject): CampaignAdScheduleForm = {
    CampaignAdScheduleForm(
      parent=controllers.Google.dboToCampaignCriterionParent(dbo.getAs[DBObject]("parent").get),
      apiId=dbo.getAsOrElse[Option[Long]]("ApiId", None),
      isNegative=dbo.getAsOrElse[Option[Boolean]]("isNegative", None),
      dayOfWeek=dbo.getAs[String]("dayOfWeek").get,
      startHour=dbo.getAs[Int]("startHour").get,
      startMinute=dbo.getAs[String]("startMinute").get,
      endHour=dbo.getAs[Int]("endHour").get,
      endMinute=dbo.getAs[String]("endMinute").get,
      bidModifier=dbo.getAs[Double]("bidModifier").get
    )
  }

  def campaignAdScheduleFormToDbo(casf: CampaignAdScheduleForm): DBObject = {
    var dbo = DBObject.newBuilder
    for((name, idx) <- Utilities.getCaseClassParameter[CampaignAdScheduleForm].zipWithIndex) {
      dbo += (Utilities.getMethodName(name) -> casf.productElement(idx))
    }
    dbo.result
  }

  def campaignAdScheduleForm: Form[CampaignAdScheduleForm] = Form(
    mapping(
      "parent" -> mapping(
        "mccObjId" -> optional(text),
        "customerApiId" -> optional(longNumber),
        "campaignApiId" -> optional(longNumber)
      )(controllers.Google.CampaignCriterionParent.apply)(controllers.Google.CampaignCriterionParent.unapply),
      "apiId" -> optional(longNumber),
      "isNegative" -> optional(boolean),
      "dayOfWeek" -> text,
      "startHour" -> number,
      "startMinute" -> text,
      "endHour" -> number,
      "endMinute" -> text,
      "bidModifier" -> of[Double]
    )(CampaignAdScheduleForm.apply)(CampaignAdScheduleForm.unapply)
  )
}
