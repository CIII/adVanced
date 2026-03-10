package models.mongodb.google

import models.mongodb.MongoExtensions._
import models.mongodb.performance.PerformanceField
import models.mongodb.performance.PerformanceField.PerformanceFieldType._
import models.mongodb.google.GoogleAccountPerformance._
import models.mongodb.performance.HtmlPerformanceField
import org.mongodb.scala.bson.Document

class GoogleAccountPerformance extends GoogleCampaignPerformance{

  def accountHtml(value: String) = setField(accountHtmlField, value)

  override def fromDocument(doc: Document){
    super.fromDocument(doc)
    this.parseStringField(doc, accountHtmlField.fieldName, accountHtml)
  }
}

object GoogleAccountPerformance{
  lazy val accountHtmlField = new HtmlPerformanceField(
    "accountHtml",
    HtmlPerformanceField.clickthroughBuilder(
      GooglePerformance.accountField,
      GooglePerformance.accountField,
      controllers.google.mcc.account.campaign.routes.CampaignController.attribution.url
    )
  )
}
