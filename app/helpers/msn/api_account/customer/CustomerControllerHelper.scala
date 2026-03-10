package helpers.msn.api_account.customer

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
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

  def documentToCustomerAddress(dbo: Document) = CustomerAddress(
    line1=Option(dbo.getString("line1")),
    line2=Option(dbo.getString("line2")),
    line3=Option(dbo.getString("line3")),
    line4=Option(dbo.getString("line4")),
    city=Option(dbo.getString("city")),
    stateOrProvince=Option(dbo.getString("stateOrProvince")),
    postalCode=Option(dbo.getString("postalCode")),
    countryCode=Option(dbo.getString("countryCode"))
  )

  def customerAddressToDocument(ca: CustomerAddress) = Document(
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

  def documentToCustomerForm(dbo: Document) = CustomerForm(
    customerApiId = Option(dbo.getLong("customerApiId")).map(_.toLong),
    customerNumber = Option(dbo.getString("customerNumber")),
    name = Option(dbo.getString("name")).getOrElse(""),
    customerAddress = documentToCustomerAddress(Option(dbo.toBsonDocument.get("customerAddress")).filter(_.isDocument).map(v => Document(v.asDocument())).get)
  )

  def customerFormToDocument(cf: CustomerForm) = Document(
    "customerApiId" -> cf.customerApiId,
    "customerNumber" -> cf.customerNumber,
    "name" -> cf.name,
    "customerAddress" -> customerAddressToDocument(cf.customerAddress)
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
