package helpers.msn.api_account.customer.account.campaign.adgroup.site_placement

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._
import sync.shared.Msn._

/**
 * Created by clarencewilliams on 11/30/15.
 */
object SitePlacementControllerHelper {
  case class SitePlacementForm(
    apiId: Option[Long],
    sitePlacementApiId: Option[Long],
    bid: Option[Bid],
    status: Option[String],
    url: String
  )

  def documentToSitePlacementForm(dbo: Document) = SitePlacementForm(
    apiId=Option(dbo.getLong("apiId")).map(_.toLong),
    sitePlacementApiId=Option(dbo.getLong("sitePLacementApiId")).map(_.toLong),
    bid=Some(documentToBid(Option(dbo.toBsonDocument.get("bid")).filter(_.isDocument).map(v => Document(v.asDocument())).get)),
    status=Option(dbo.getString("status")),
    url=Option(dbo.getString("url")).getOrElse("")
  )

  def sitePlacementFormToDocument(spf: SitePlacementForm) = Document(
    "apiId" -> spf.apiId,
    "sitePlacementApiId" -> spf.sitePlacementApiId,
    "bid" -> bidToDocument(spf.bid.get),
    "status" -> spf.status,
    "url" -> spf.url
  )


  def sitePlacementForm: Form[SitePlacementForm] = Form(
    mapping(
      "apiId" -> optional(longNumber),
      "sitePlacementId" -> optional(longNumber),
      "bid" -> optional(mapping(
        "amount" -> of(doubleFormat)
      )(Bid.apply)(Bid.unapply)),
      "status" -> optional(text),
      "url" -> text
    )(SitePlacementForm.apply)(SitePlacementForm.unapply)
  )
}
