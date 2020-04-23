package helpers.msn.api_account.customer.account.campaign

import com.mongodb.casbah.Imports._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._

object CampaignControllerHelper {
  case class CampaignForm(
    name: String,
    description: Option[String],
    budgetType: Option[String],
    dailyBudget: Option[Double],
    status: String,
    timeZone: String
  )


  def dboToCampaignForm(dbo: DBObject) = CampaignForm(
    name=dbo.getAsOrElse[String]("name", ""),
    description=dbo.getAsOrElse[Option[String]]("description", None),
    budgetType=dbo.getAsOrElse[Option[String]]("budgetType", None),
    dailyBudget=dbo.getAsOrElse[Option[Double]]("dailyBudget", None),
    status=dbo.getAsOrElse[String]("status", ""),
    timeZone=dbo.getAsOrElse[String]("timeZone", "")
  )

  def campaignFormToDbo(cf: CampaignForm) = DBObject(
    "name" -> cf.name,
    "description" -> cf.description,
    "budgetType" -> cf.budgetType,
    "dailyBudget" -> cf.dailyBudget,
    "status" -> cf.status,
    "timeZone" -> cf.timeZone
  )


  def campaignForm: Form[CampaignForm] = Form(
    mapping(
      "name" -> nonEmptyText,
      "description" -> optional(text),
      "budgetType" -> optional(text),
      "dailyBudget" -> optional(of[Double]),
      "status" -> nonEmptyText,
      "timeZone" -> nonEmptyText
    )(CampaignForm.apply)(CampaignForm.unapply)
  )
}
