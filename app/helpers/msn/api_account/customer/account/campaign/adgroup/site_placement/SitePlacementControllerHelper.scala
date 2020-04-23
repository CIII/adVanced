package helpers.msn.api_account.customer.account.campaign.adgroup.site_placement

import com.mongodb.casbah.Imports._
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

  def dboToSitePlacementForm(dbo: DBObject) = SitePlacementForm(
    apiId=dbo.getAsOrElse[Option[Long]]("apiId", None),
    sitePlacementApiId=dbo.getAsOrElse[Option[Long]]("sitePLacementApiId", None),
    bid=Some(dboToBid(dbo.getAs[DBObject]("bid").get)),
    status=dbo.getAsOrElse[Option[String]]("status", None),
    url=dbo.getAsOrElse[String]("url", "")
  )

  def sitePlacementFormToDbo(spf: SitePlacementForm) = DBObject(
    "apiId" -> spf.apiId,
    "sitePlacementApiId" -> spf.sitePlacementApiId,
    "bid" -> bidToDbo(spf.bid.get),
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
