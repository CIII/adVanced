package models.mongodb.google

import models.mongodb.performance.PerformanceField
import models.mongodb.performance.PerformanceField.PerformanceFieldType._
import models.mongodb.google.GoogleAccountPerformance._
import models.mongodb.performance.HtmlPerformanceField
import com.mongodb.casbah.Imports._

class GoogleAccountPerformance extends GoogleCampaignPerformance{
  
  def accountHtml(value: String) = setField(accountHtmlField, value)
  
  override def fromDBO(dbo: DBObject){
    super.fromDBO(dbo)
    this.parseStringField(dbo, accountHtmlField.fieldName, accountHtml)
  }
}

object GoogleAccountPerformance{
  lazy val accountHtmlField = new HtmlPerformanceField(
    "accountHtml",
    HtmlPerformanceField.clickthroughBuilder(
      GooglePerformance.accountField,
      GooglePerformance.accountField,
      controllers.google.mcc.account.campaign.routes.CampaignController.attribution().url
    )
  )
}