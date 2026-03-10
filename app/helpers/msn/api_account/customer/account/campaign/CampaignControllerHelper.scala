package helpers.msn.api_account.customer.account.campaign

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
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


  def documentToCampaignForm(dbo: Document) = CampaignForm(
    name=Option(dbo.getString("name")).getOrElse(""),
    description=Option(dbo.getString("description")),
    budgetType=Option(dbo.getString("budgetType")),
    dailyBudget=Option(dbo.getDouble("dailyBudget")).map(_.toDouble),
    status=Option(dbo.getString("status")).getOrElse(""),
    timeZone=Option(dbo.getString("timeZone")).getOrElse("")
  )

  def campaignFormToDocument(cf: CampaignForm) = Document(
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
