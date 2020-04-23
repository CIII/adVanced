package models.mongodb.google

import com.mongodb.casbah.Imports._ 
import models.mongodb.performance.PerformanceField
import models.mongodb.performance.PerformanceField.PerformanceFieldType._
import models.mongodb.google.GoogleAdGroupPerformance._
import models.mongodb.performance.HtmlPerformanceField
import models.mongodb.performance.PerformanceField.VisualizationDataType.VisualizationDataType
import models.mongodb.performance.PerformanceField.VisualizationDataType.number
import models.mongodb.google.Google._
import com.google.api.ads.adwords.axis.v201609.cm.AdGroup
import com.google.api.ads.adwords.lib.selectorfields.v201609.cm.AdGroupField
import com.google.api.ads.adwords.axis.v201609.cm.CpcBid
import Shared.Shared._

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
  
  override def toDBO: DBObject = {
    val dbo = super.toDBO
    dbo.putAll(
      DBObject(
        adGroupNameField.fieldName -> adGroupName,
        adGroupIdField.fieldName -> adGroupId,
        adGroupStateField.fieldName -> adGroupState
      )
    )
    
    dbo
  }
  
  override def fromDBO(dbo: DBObject){
    super.fromDBO(dbo)
    this.parseStringField(dbo, adGroupNameField.fieldName, adGroupName)
    this.parseLongField(dbo, adGroupIdField.fieldName, adGroupId)
    this.parseStringField(dbo, adGroupStateField.fieldName, adGroupState)
    this.parseStringField(dbo, adGroupHtmlField.fieldName, adGroupHtml)
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
    
  def getMaxCpcForAdGroup(adGroupId: Long): Long = {
    googleAdGroupCollection.findOne(DBObject("adGroup.object.id" -> adGroupId)) match {
      case Some(adGroupObj) => 
        val adGroup = dboToGoogleEntity[AdGroup](adGroupObj, "adGroup", None)
        adGroup.getBiddingStrategyConfiguration.getBids().filter { 
          bid => bid.getBidsType().equalsIgnoreCase(AdGroupField.CpcBid.toString)
        }.head.asInstanceOf[CpcBid].getBid.getMicroAmount
        
      case _ => -1L
    }
  }
}