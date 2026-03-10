package helpers.facebook.api_account.campaign.ad_set.ad

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import play.api.data.{Form, Forms}
import play.api.data.Forms._
import scala.jdk.CollectionConverters._

object AdControllerHelper {

  case class AdParent(
    apiAccountObjId: Option[String],
    campaignApiId: Option[String],
    adSetApiId: Option[String]
  )

  def adParentToDocument(ap: AdParent): Document = Document(
    "apiAccountObjId" -> ap.apiAccountObjId,
    "campaignApiId" -> ap.campaignApiId,
    "adSetApiId" -> ap.adSetApiId
  )

  def documentToAdParent(dbo: Document): AdParent = AdParent(
    apiAccountObjId = Option(dbo.getString("apiAccountObjId")),
    campaignApiId = Option(dbo.getString("campaignApiId")),
    adSetApiId = Option(dbo.getString("adSetApiId"))
  )

  case class AdCreative(
    adImage: Option[String],
    adLink: String,
    adMessage: Option[String]
  )

  case class AdForm(
    parent: AdParent,
    apiId: Option[String],
    accountId: String,
    adSetId: String,
    campaignId: String,
    adLabels: Option[List[String]],
    adCreatives: Option[List[AdCreative]],
    bidAmount: Option[Long],
    createdTime: Option[String],
    effectiveStatus: Option[String],
    name: String,
    status: Option[String],
    updatedTime: Option[String]
  )

  def adFormToDocument(asf: AdForm): Document = {
    Document(
      "parent" -> adParentToDocument(asf.parent),
      "apiId" -> asf.apiId,
      "accountId" -> asf.accountId,
      "adSetId" -> asf.adSetId,
      "campaignId" -> asf.campaignId,
      "adLabels" -> asf.adLabels,
      "adCreatives" -> asf.adCreatives.map(_.map(ac => Document(
        "adImage" -> ac.adImage,
        "adLink" -> ac.adLink,
        "adMessage" -> ac.adMessage
      ))),
      "bidAmount" -> asf.bidAmount,
      "createdTime" -> asf.createdTime,
      "effectiveStatus" -> asf.effectiveStatus,
      "name" -> asf.name,
      "status" -> asf.status,
      "updatedTime" -> asf.updatedTime
    )
  }

  def documentToAdForm(dbo: Document): AdForm = {
    AdForm(
      parent=documentToAdParent(Option(dbo.toBsonDocument.get("parent")).map(v => Document(v.asDocument())).get),
      apiId = Option(dbo.getString("apiId")),
      accountId = Option(dbo.getString("accountId")).getOrElse(""),
      adSetId = Option(dbo.getString("adSetId")).getOrElse(""),
      campaignId = Option(dbo.getString("campaignId")).getOrElse(""),
      adLabels = Option(dbo.getList("adLabels", classOf[String])).map(_.asScala.toList),
      adCreatives = Option(dbo.getList("adCreatives", classOf[Document])).map(_.asScala.toList.map(d =>
        AdCreative(
          adImage = Option(d.getString("adImage")),
          adLink = d.getString("adLink"),
          adMessage = Option(d.getString("adMessage"))
        )
      )),
      bidAmount = Option(dbo.getLong("bidAmount")).map(_.toLong),
      createdTime = Option(dbo.getString("createdTime")),
      effectiveStatus = Option(dbo.getString("effectiveStatus")),
      name = Option(dbo.getString("name")).getOrElse(""),
      status = Option(dbo.getString("status")),
      updatedTime = Option(dbo.getString("updatedTime"))
    )
  }

  def adForm: Form[AdForm] = Form(
    mapping(
      "parent" -> mapping(
        "apiAccountObjId" -> optional(text),
        "campaignApiId" -> optional(text),
        "adSetApiId" -> optional(text)
      )(AdParent.apply)(AdParent.unapply),
      "apiId" -> optional(text),
      "accountId" -> nonEmptyText,
      "adSetId" -> nonEmptyText,
      "campaignId" -> nonEmptyText,
      "adLabels" -> optional(Forms.list(text)),
      "adCreatives" -> optional(
        Forms.list(
          mapping(
            "adImage" -> optional(text),
            "adLink" -> nonEmptyText,
            "adMessage" -> optional(text)
          )(AdCreative.apply)(AdCreative.unapply)
        )
      ),
      "bidAmount" -> optional(longNumber),
      "createdTime" -> optional(text),
      "effectiveStatus" -> optional(text),
      "name" -> text,
      "status" -> optional(text),
      "updatedTime" -> optional(text)
    )(AdForm.apply)(AdForm.unapply)
  )
}