package helpers.google.mcc.account

import com.mongodb.casbah.Imports._
import play.api.data.Form
import play.api.data.Forms._

object BudgetControllerHelper {
  case class BudgetForm(
    var apiId: Option[Long],
    var name: String,
    var amount: Option[Long],
    var deliveryMethod: Option[String],
    var isExplicitlyShared: Option[Boolean],
    var status: Option[String]
  )

  def budgetFormToDbo(bf: BudgetForm): DBObject = {
    DBObject(
      "apiId" -> bf.apiId,
      "name" -> bf.name,
      "amount" -> bf.amount,
      "deliveryMethod" -> bf.deliveryMethod,
      "isExplicitlyShared" -> bf.isExplicitlyShared,
      "status" -> bf.status
    )
  }

  def dboToBudgetForm(dbo: DBObject): BudgetForm = {
    BudgetForm(
      apiId = dbo.getAsOrElse[Option[Long]]("apiId", None),
      name = dbo.getAsOrElse[String]("name", ""),
      amount = dbo.getAsOrElse[Option[Long]]("periodAmount", None),
      deliveryMethod = dbo.getAsOrElse[Option[String]]("deliveryMethod", None),
      isExplicitlyShared = dbo.getAsOrElse[Option[Boolean]]("isExplicitlyShared", None),
      status = dbo.getAsOrElse[Option[String]]("status", None)
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