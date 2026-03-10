package helpers.google.mcc.account

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import play.api.data.Form
import play.api.data.Forms._

object BudgetControllerHelper {
  case class BudgetForm(
    apiId: Option[Long],
    name: String,
    amount: Option[Long],
    deliveryMethod: Option[String],
    isExplicitlyShared: Option[Boolean],
    status: Option[String]
  )

  def budgetFormToDocument(bf: BudgetForm): Document = {
    Document(
      "apiId" -> bf.apiId,
      "name" -> bf.name,
      "amount" -> bf.amount,
      "deliveryMethod" -> bf.deliveryMethod,
      "isExplicitlyShared" -> bf.isExplicitlyShared,
      "status" -> bf.status
    )
  }

  def documentToBudgetForm(dbo: Document): BudgetForm = {
    BudgetForm(
      apiId = Option(dbo.getLong("apiId")).map(_.toLong),
      name = Option(dbo.getString("name")).getOrElse(""),
      amount = Option(dbo.getLong("periodAmount")).map(_.toLong),
      deliveryMethod = Option(dbo.getString("deliveryMethod")),
      isExplicitlyShared = Option(dbo.getBoolean("isExplicitlyShared")).map(_.booleanValue()),
      status = Option(dbo.getString("status"))
    )
  }

  def budgetForm: Form[BudgetForm] = Form(
    mapping(
      "apiId" -> optional(longNumber),
      "name" -> text,
      "amount" -> optional(longNumber),
      "deliveryMethod" -> optional(text),
      "isExplicitlyShared" -> optional(boolean),
      "status" -> optional(text)
    )(BudgetForm.apply)(BudgetForm.unapply)
  )
}