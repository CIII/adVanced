package helpers.facebook.api_account.campaign.ad_set.ad

import com.mongodb.casbah.Imports._
import play.api.data.{Form, Forms}
import play.api.data.Forms._

object AdControllerHelper {

  case class AdCreative(
    var adImage: Option[String],
    var adLink: String,
    var adMessage: Option[String]
  )

  case class AdForm(
    var parent: AdParent,
    var apiId: Option[String],
    var accountId: String,
    var adSetId: String,
    var campaignId: String,
    var adLabels: Option[List[String]],
    var adCreatives: Option[List[AdCreative]],
    var bidAmount: Option[Long],
    var createdTime: Option[String],
    var effectiveStatus: Option[String],
    var name: String,
    var status: Option[String],
    var updatedTime: Option[String]
  )

  def adFormToDbo(asf: AdForm): DBObject = {
    DBObject(
      "parent" -> asf.parent,
      "apiId" -> asf.apiId,
      "accountId" -> asf.accountId,
      "adSetId" -> asf.adSetId,
      "campaignId" -> asf.campaignId,
      "adLabels" -> asf.adLabels,
      "adCreatives" -> asf.adCreatives,
      "bidAmount" -> asf.bidAmount,
      "createdTime" -> asf.createdTime,
      "effectiveStatus" -> asf.effectiveStatus,
      "name" -> asf.name,
      "status" -> asf.status,
      "updatedTime" -> asf.updatedTime
    )
  }

  def dboToAdForm(dbo: DBObject): AdForm = {
    AdForm(
      parent=dboToAdParent(dbo.as[DBObject]("parent")),
      apiId = dbo.getAs[String]("apiId"),
      accountId = dbo.getAsOrElse[String]("accountId", ""),
      adSetId = dbo.getAsOrElse[String]("adSetId", ""),
      campaignId = dbo.getAsOrElse[String]("campaignId", ""),
      adLabels = dbo.getAs[List[String]]("adLabels"),
      adCreatives = dbo.getAs[List[AdCreative]]("adCreatives"),
      bidAmount = dbo.getAs[Long]("bidAmount"),
      createdTime = dbo.getAs[String]("createdTime"),
      effectiveStatus = dbo.getAs[String]("effectiveStatus"),
      name = dbo.getAsOrElse[String]("name", ""),
      status = dbo.getAs[String]("status"),
      updatedTime = dbo.getAs[String]("updatedTime")
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