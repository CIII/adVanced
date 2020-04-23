package helpers.msn.api_account.customer

import com.mongodb.casbah.Imports._
import play.api.data.Form
import play.api.data.Forms._

/**
 * Created by clarencewilliams on 11/30/15.
 */
object CustomerControllerHelper {
  case class CustomerAddress(
    line1: Option[String],
    line2: Option[String],
    line3: Option[String],
    line4: Option[String],
    city: Option[String],
    stateOrProvince: Option[String],
    postalCode: Option[String],
    countryCode: Option[String]
  )

  def dboToCustomerAddress(dbo: DBObject) = CustomerAddress(
    line1=dbo.getAsOrElse[Option[String]]("line1", None),
    line2=dbo.getAsOrElse[Option[String]]("line2", None),
    line3=dbo.getAsOrElse[Option[String]]("line3", None),
    line4=dbo.getAsOrElse[Option[String]]("line4", None),
    city=dbo.getAsOrElse[Option[String]]("city", None),
    stateOrProvince=dbo.getAsOrElse[Option[String]]("stateOrProvince", None),
    postalCode=dbo.getAsOrElse[Option[String]]("postalCode", None),
    countryCode=dbo.getAsOrElse[Option[String]]("countryCode", None)
  )

  def customerAddressToDbo(ca: CustomerAddress) = DBObject(
    "line1" -> ca.line1,
    "line2" -> ca.line2,
    "line3" -> ca.line3,
    "line4" -> ca.line4,
    "city" -> ca.city,
    "stateOrProvince" -> ca.stateOrProvince,
    "postalCode" -> ca.postalCode,
    "countryCode" -> ca.countryCode
  )

  case class CustomerForm(
    customerApiId: Option[Long],
    customerNumber: Option[String],
    name: String,
    customerAddress: CustomerAddress
  )

  def dboToCustomerForm(dbo: DBObject) = CustomerForm(
    customerApiId = dbo.getAsOrElse[Option[Long]]("customerApiId", None),
    customerNumber = dbo.getAsOrElse[Option[String]]("customerNumber", None),
    name = dbo.getAsOrElse[String]("name", ""),
    customerAddress = dboToCustomerAddress(dbo.getAs[DBObject]("customerAddress").get)
  )

  def customerFormToDbo(cf: CustomerForm) = DBObject(
    "customerApiId" -> cf.customerApiId,
    "customerNumber" -> cf.customerNumber,
    "name" -> cf.name,
    "customerAddress" -> customerAddressToDbo(cf.customerAddress)
  )

  def customerForm: Form[CustomerForm] = Form(
    mapping(
      "customerApiId" -> optional(longNumber),
      "customerNumber" -> optional(text),
      "name" -> nonEmptyText,
      "customerAddress" -> mapping(
        "line1" -> optional(text),
        "line2" -> optional(text),
        "line3" -> optional(text),
        "line4" -> optional(text),
        "city" -> optional(text),
        "stateOrProvince" -> optional(text),
        "postalCode" -> optional(text),
        "countryCode" -> optional(text)
      )(CustomerAddress.apply)(CustomerAddress.unapply)
    )(CustomerForm.apply)(CustomerForm.unapply)
  )
}
