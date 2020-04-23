package models.mongodb.google

import com.mongodb.casbah.Imports._ 
import models.mongodb.google.GoogleCampaignPerformance._
import models.mongodb.performance.PerformanceField
import models.mongodb.performance.PerformanceField.PerformanceFieldType._
import models.mongodb.performance.PerformanceField.VisualizationDataType.VisualizationDataType
import models.mongodb.performance.PerformanceField.VisualizationDataType.number
import models.mongodb.performance.HtmlPerformanceField
import models.mongodb.performance.HtmlPerformanceField._

class GoogleCampaignPerformance extends GooglePerformance {
 
  def campaignState(): String = getStringField(campaignStateField)
  def campaignState(value: String) = setField(campaignStateField, value)
  
  def campaignName(): String = getStringField(campaignNameField)
  def campaignName(value: String) = setField(campaignNameField, value)
  
  def campaignId(): Long = getLongField(campaignIdField)
  def campaignId(value: Long) = setField(campaignIdField, value)
  
  def campaignHtml(): String = getStringField(campaignHtmlField)
  def campaignHtml(value: String) = setField(campaignHtmlField, value)
  
  def campaignBudget(): Double = getDoubleField(campaignBudgetField)
  def campaignBudget(value: Double) = setField(campaignBudgetField, value)
  
  def campaignBudgetEditHtml(value: String) = setField(campaignBudgetEditHtmlField, value)
    
  override def toDBO: DBObject = {
    val dbo = super.toDBO
    dbo.putAll(
      DBObject(
        campaignIdField.fieldName -> campaignId,
        campaignNameField.fieldName -> campaignName,
        campaignStateField.fieldName -> campaignState
      )
    )
    
    dbo
  }
  
  override def fromDBO(dbo: DBObject){
    super.fromDBO(dbo)
    this.parseStringField(dbo, campaignStateField.fieldName, campaignState)
    this.parseStringField(dbo, campaignNameField.fieldName, campaignName)
    this.parseLongField(dbo, campaignIdField.fieldName, campaignId)
    this.parseStringField(dbo, campaignHtmlField.fieldName, campaignHtml)
    this.parseDoubleField(dbo, campaignBudgetField.fieldName, campaignBudget, 0.0)
    this.parseStringField(dbo, campaignBudgetEditHtmlField.fieldName, campaignBudgetEditHtml, "0.0");
  }
}

object GoogleCampaignPerformance{
  lazy val campaignStateField = new PerformanceField("campaignState", dimension)
  lazy val campaignNameField = new PerformanceField("campaign", dimension)
  lazy val campaignIdField = new PerformanceField("campaignId", dimension){
    override lazy val visualizationDataType = number
  }
  
  lazy val campaignHtmlField = new HtmlPerformanceField(
    "campaignHtml", 
    HtmlPerformanceField.clickthroughAndEditBuilder(
      campaignNameField, 
      controllers.google.mcc.account.campaign.adgroup.routes.AdGroupController.attribution().url, 
      controllers.google.mcc.account.campaign.routes.CampaignController.campaigns().url
    )
  )
  
  lazy val campaignBudgetField = new PerformanceField("campaignBudget", measure){
    override def dependantFields(): List[PerformanceField] = List(campaignIdField)
    override def projectionQueryObject(): DBObject = {
      MongoDBObject(this.fieldName -> 
        MongoDBObject("$divide" -> 
          MongoDBList(
              "$campaigns.campaign.object.budget.amount.microAmount",
              1000000
          )
        )
      )
    }
  }
  
  /**
   * The campaign budget edit html field is really an html concatenation, but the sub-objects within the
   * concat are complex enough that it wasn't achieveable using an HtmlPerformanceField.  Because this
   * is a rare circumstance it is built out using db objects.
   */
  lazy val campaignBudgetEditHtmlField = new PerformanceField("campaignBudgetEditHtml", dimension){
    override def dependantFields(): List[PerformanceField] = List(campaignIdField, campaignNameField)
    override def projectionQueryObject(): DBObject = {
      MongoDBObject(this.fieldName -> 
        MongoDBObject("$concat" ->
          MongoDBList(
            "<div>$",
            MongoDBObject("$substr" ->
              MongoDBList(
                MongoDBObject("$divide"->
                  MongoDBList(
                    MongoDBObject("$ifNull" ->
                      MongoDBList(
                        "$campaigns.campaign.object.budget.amount.microAmount",
                        0.0
                      )
                    ),
                    1000000
                  )
                ),
                0,
                -1
              )
            ),
            "<span class=\"pull-right\">",
            "<a href=\"#\" onClick=\'showBudgetEditModal(",
            MongoDBObject("$substr" ->
              MongoDBList(
                "$_id." + campaignIdField.fieldName,
                0,
                -1
              )
            ),
            ",\"",
            "$_id." + campaignNameField.fieldName,
            "\",",
            MongoDBObject("$substr" ->
              MongoDBList(
                MongoDBObject("$ifNull" ->
                  MongoDBList(
                    "$campaigns.campaign.object.budget.budgetId",
                    0
                  )
                ),
                0,
                -1
              )
            ),
            ",",
            MongoDBObject("$substr" ->
              MongoDBList(
                MongoDBObject("$divide"->
                  MongoDBList(
                    MongoDBObject("$ifNull" ->
                      MongoDBList(
                        "$campaigns.campaign.object.budget.amount.microAmount",
                        0.0
                      )
                    ),
                    1000000
                  )
                ),
                0,
                -1
              )
            ),
            ",",
            MongoDBObject("$cond" ->
              MongoDBList(
                "$campaigns.campaign.object.budget.isExplicitlyShared",
                "true",
                "false"
              )
            ),
            ",",
            MongoDBObject("$substr" ->
              MongoDBList(
                "$campaigns.campaign.object.budget.referenceCount",
                0,
                -1
              )
            ),
            ")\'><i class=\"fa fa-edit\"></i></a>",
            "</span>",
            "</div>"
          )
        )
      )               
    }
  }
}