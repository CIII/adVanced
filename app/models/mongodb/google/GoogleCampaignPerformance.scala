package models.mongodb.google

import org.bson._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
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

  override def toDocument: Document = {
    val base = super.toDocument
    base ++ Document(
      campaignIdField.fieldName -> campaignId,
      campaignNameField.fieldName -> campaignName,
      campaignStateField.fieldName -> campaignState
    )
  }

  override def fromDocument(doc: Document){
    super.fromDocument(doc)
    this.parseStringField(doc, campaignStateField.fieldName, campaignState)
    this.parseStringField(doc, campaignNameField.fieldName, campaignName)
    this.parseLongField(doc, campaignIdField.fieldName, campaignId)
    this.parseStringField(doc, campaignHtmlField.fieldName, campaignHtml)
    this.parseDoubleField(doc, campaignBudgetField.fieldName, campaignBudget, 0.0)
    this.parseStringField(doc, campaignBudgetEditHtmlField.fieldName, campaignBudgetEditHtml, "0.0");
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
      controllers.google.mcc.account.campaign.adgroup.routes.AdGroupController.attribution.url,
      controllers.google.mcc.account.campaign.routes.CampaignController.campaigns().url
    )
  )

  /**
   * Convert a mixed-type sequence of values to a BsonArray suitable for use
   * in MongoDB aggregation pipeline operators (e.g. $divide, $concat, $substr).
   *
   * Supported element types:
   *   String    -> BsonString
   *   Int       -> BsonInt32
   *   Long      -> BsonInt64
   *   Double    -> BsonDouble
   *   Boolean   -> BsonBoolean
   *   Document  -> BsonDocument (via toBsonDocument)
   *   BsonValue -> used as-is
   */
  private def bsonArr(elems: Any*): BsonArray = {
    val arr = new BsonArray()
    elems.foreach {
      case s: String    => arr.add(new BsonString(s))
      case i: Int       => arr.add(new BsonInt32(i))
      case l: Long      => arr.add(new BsonInt64(l))
      case d: Double    => arr.add(new BsonDouble(d))
      case b: Boolean   => arr.add(new BsonBoolean(b))
      case doc: Document => arr.add(doc.toBsonDocument)
      case bv: BsonValue => arr.add(bv)
      case other        => arr.add(new BsonString(other.toString))
    }
    arr
  }

  lazy val campaignBudgetField = new PerformanceField("campaignBudget", measure){
    override def dependantFields(): List[PerformanceField] = List(campaignIdField)
    override def projectionQueryObject(): Document = {
      Document(this.fieldName ->
        Document("$divide" ->
          bsonArr(
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
    override def projectionQueryObject(): Document = {
      Document(this.fieldName ->
        Document("$concat" ->
          bsonArr(
            "<div>$",
            Document("$substr" ->
              bsonArr(
                Document("$divide"->
                  bsonArr(
                    Document("$ifNull" ->
                      bsonArr(
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
            Document("$substr" ->
              bsonArr(
                "$_id." + campaignIdField.fieldName,
                0,
                -1
              )
            ),
            ",\"",
            "$_id." + campaignNameField.fieldName,
            "\",",
            Document("$substr" ->
              bsonArr(
                Document("$ifNull" ->
                  bsonArr(
                    "$campaigns.campaign.object.budget.budgetId",
                    0
                  )
                ),
                0,
                -1
              )
            ),
            ",",
            Document("$substr" ->
              bsonArr(
                Document("$divide"->
                  bsonArr(
                    Document("$ifNull" ->
                      bsonArr(
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
            Document("$cond" ->
              bsonArr(
                "$campaigns.campaign.object.budget.isExplicitlyShared",
                "true",
                "false"
              )
            ),
            ",",
            Document("$substr" ->
              bsonArr(
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
