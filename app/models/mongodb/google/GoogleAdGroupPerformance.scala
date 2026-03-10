package models.mongodb.google

import org.mongodb.scala.bson.Document
import org.mongodb.scala.model.Filters._
import models.mongodb.MongoExtensions._
import models.mongodb.performance.PerformanceField
import models.mongodb.performance.PerformanceField.PerformanceFieldType._
import models.mongodb.google.GoogleAdGroupPerformance._
import models.mongodb.performance.HtmlPerformanceField
import models.mongodb.performance.PerformanceField.VisualizationDataType.VisualizationDataType
import models.mongodb.performance.PerformanceField.VisualizationDataType.number
import models.mongodb.google.Google._
import Shared.Shared._

import scala.concurrent.Await
import scala.concurrent.duration._

class GoogleAdGroupPerformance extends GoogleCampaignPerformance{

  def adGroupName(): String = getStringField(adGroupNameField)
  def adGroupName(value: String) = setField(adGroupNameField, value)

  def adGroupId(): Long = getLongField(adGroupIdField)
  def adGroupId(value: Long) = setField(adGroupIdField, value)

  def adGroupState(): String = getStringField(adGroupStateField)
  def adGroupState(value: String) = setField(adGroupStateField, value)

  def adGroupHtml(): String = getStringField(adGroupHtmlField)
  def adGroupHtml(value: String) = setField(adGroupHtmlField, value)

  def maxCpc(): Double = getDoubleField(maxCpcField)
  def maxCpc(value: Double) = setField(maxCpcField, value)

  def maxCpcHtml(): String = getStringField(maxCpcHtmlField)
  def maxCpcHtml(value: String) = setField(maxCpcHtmlField, value)

  override def toDocument: Document = {
    val base = super.toDocument
    base ++ Document(
      adGroupNameField.fieldName -> adGroupName,
      adGroupIdField.fieldName -> adGroupId,
      adGroupStateField.fieldName -> adGroupState
    )
  }

  override def fromDocument(doc: Document){
    super.fromDocument(doc)
    this.parseStringField(doc, adGroupNameField.fieldName, adGroupName)
    this.parseLongField(doc, adGroupIdField.fieldName, adGroupId)
    this.parseStringField(doc, adGroupStateField.fieldName, adGroupState)
    this.parseStringField(doc, adGroupHtmlField.fieldName, adGroupHtml)
    if(fieldExists(adGroupIdField)){
      maxCpc(microToDollars(getMaxCpcForAdGroup(adGroupId)))
      maxCpcHtml(buildMaxCpcHtml)
    }
  }

  def buildMaxCpcHtml(): String = {
    if(maxCpc < 0){
      " -- "
    } else {
      ("<div>$%f<span class=\"pull-right\">" +
	    "<a href=\"#\" onClick=\'showMaxCpcEditModal(%d,\"%s\",%f)\'>" +
	    "<i class=\"fa fa-edit\"></i></a>" +
	    "</span></div>").format(maxCpc, adGroupId, adGroupName, maxCpc)
    }
  }
}

object GoogleAdGroupPerformance {
  // NOTE: googleAdGroupCollection must be provided via dependency injection (MongoService)
  var googleAdGroupCollection: org.mongodb.scala.MongoCollection[Document] = _

  lazy val adGroupNameField = new PerformanceField("adGroupName", dimension)
  lazy val adGroupIdField = new PerformanceField("adGroupId", dimension){
    override lazy val visualizationDataType = number
  }
  lazy val adGroupStateField = new PerformanceField("adGroupState", dimension)
  lazy val adGroupHtmlField = new HtmlPerformanceField(
    "adGroupHtml",
    HtmlPerformanceField.clickthroughAndEditBuilder(
      adGroupNameField,
      controllers.google.mcc.account.campaign.adgroup.ad.routes.AdController.attribution.url,
      controllers.google.mcc.account.campaign.adgroup.routes.AdGroupController.adGroups.url
    )
  )

  lazy val maxCpcField = new PerformanceField("maxCpc", dimension){
    override lazy val visualizationDataType = number
    override def dependantFields(): List[PerformanceField] = List(adGroupIdField)
  }

  lazy val maxCpcHtmlField = new PerformanceField("maxCpcHtml", dimension){
    override def dependantFields(): List[PerformanceField] = List(adGroupIdField, adGroupNameField)
  }

  /**
   * Retrieve the max CPC bid for an ad group from the stored Document.
   *
   * TODO: Replace with Google Ads API v18 ad group bid retrieval.
   * The old AdWords API v201609 AdGroup, AdGroupField, and CpcBid types are no longer available.
   * This now reads the CPC bid directly from the stored Document structure.
   */
  def getMaxCpcForAdGroup(adGroupId: Long): Long = {
    val result = Await.result(
      googleAdGroupCollection.find(equal("adGroup.object.id", adGroupId)).first().toFuture(),
      10.seconds
    )
    if (result != null) {
      // TODO: Not yet migrated to Google Ads API v18
      // Previously used documentToGoogleEntity[AdGroup] and AdWords CpcBid type.
      // Now reading directly from stored Document structure.
      try {
        val adGroupDoc = Option(result.toBsonDocument.get("adGroup")).map(v => Document(v.asDocument())).flatMap(d => Option(d.toBsonDocument.get("object")).map(v => Document(v.asDocument())))
        adGroupDoc.flatMap { doc =>
          val biddingConfig = Option(doc.toBsonDocument.get("biddingStrategyConfiguration")).map(v => Document(v.asDocument()))
          biddingConfig.flatMap { config =>
            val bids = config.getList("bids", classOf[Document])
            import scala.jdk.CollectionConverters._
            bids.asScala.find { bid =>
              Option(bid.getString("bidsType")).exists(_.equalsIgnoreCase("CpcBid"))
            }.flatMap { cpcBid =>
              Option(cpcBid.toBsonDocument.get("bid")).map(v => Document(v.asDocument())).flatMap { bidDoc =>
                Option(bidDoc.getLong("microAmount")).map(_.longValue())
              }
            }
          }
        }.getOrElse(-1L)
      } catch {
        case _: Exception => -1L
      }
    } else {
      -1L
    }
  }
}
