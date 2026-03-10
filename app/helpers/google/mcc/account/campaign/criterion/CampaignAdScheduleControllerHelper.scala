package helpers.google.mcc.account.campaign.criterion

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import models.mongodb.Utilities
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._

object CampaignAdScheduleControllerHelper {

  case class CampaignAdScheduleForm(
    parent: controllers.Google.CampaignCriterionParent,
    apiId: Option[Long],
    isNegative: Option[Boolean],
    dayOfWeek: String,
    startHour: Int,
    startMinute: String,
    endHour: Int,
    endMinute: String,
    bidModifier: Double
  )

  def documentToCampaignAdScheduleForm(dbo: Document): CampaignAdScheduleForm = {
    CampaignAdScheduleForm(
      parent=controllers.Google.documentToCampaignCriterionParent(Option(dbo.toBsonDocument.get("parent")).filter(_.isDocument).map(v => Document(v.asDocument())).get),
      apiId=Option(dbo.getLong("ApiId")).map(_.toLong),
      isNegative=Option(dbo.getBoolean("isNegative")).map(_.booleanValue()),
      dayOfWeek=dbo.getString("dayOfWeek"),
      startHour=dbo.getInteger("startHour"),
      startMinute=dbo.getString("startMinute"),
      endHour=dbo.getInteger("endHour"),
      endMinute=dbo.getString("endMinute"),
      bidModifier=dbo.getDouble("bidModifier")
    )
  }

  def campaignAdScheduleFormToDocument(casf: CampaignAdScheduleForm): Document = {
    var dbo = Document()
    for((name, idx) <- Utilities.getCaseClassParameter[CampaignAdScheduleForm].zipWithIndex) {
      dbo = dbo ++ Document(Utilities.getMethodName(name) -> casf.productElement(idx).toString)
    }
    dbo
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
