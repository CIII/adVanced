package models.mongodb.google

import models.mongodb.performance.PerformanceField
import models.mongodb.performance.PerformanceField.PerformanceFieldType._
import models.mongodb.performance.PerformanceField.VisualizationDataType.VisualizationDataType
import models.mongodb.performance.PerformanceField.VisualizationDataType.number
import models.mongodb.performance.HtmlPerformanceField
import models.mongodb.google.GoogleAdPerformance._
import com.mongodb.casbah.Imports._ 

class GoogleAdPerformance extends GoogleAdGroupPerformance{

  def adName(): String = getStringField(adNameField)
  def adName(value: String) = setField(adNameField, value)
  
  def adId(): Long = getLongField(adIdField)
  def adId(value: Long) = setField(adIdField, value)
  
  def adState(): String = getStringField(adStateField)
  def adState(value: String) = setField(adStateField, value)
  
  def adType(): String = getStringField(adTypeField)
  def adType(value: String) = setField(adTypeField, value)
  
  def adDescription(): String = getStringField(adDescriptionField)
  def adDescription(value: String) = setField(adDescriptionField, value)
  
  def adDescriptionLine1(): String = getStringField(adDescriptionLine1Field)
  def adDescriptionLine1(value: String) = setField(adDescriptionLine1Field, value)
  
  def headline1():String = getStringField(headline1Field)
  def headline1(value: String) = setField(headline1Field, value)
  
  def headline2(): String = getStringField(headline2Field)
  def headline2(value: String) = setField(headline2Field, value)
  
  def longHeadline(): String = getStringField(longHeadlineField)
  def longHeadline(value: String) = setField(longHeadlineField, value)
  
  def imageAdName(): String = getStringField(imageAdNameField)
  def imageAdName(value: String) = setField(imageAdNameField, value)
  
  def imageWidth(): String = getStringField(imageWidthField)
  def imageWidth(value: String) = setField(imageWidthField, value)
  
  def imageHeight(): String = getStringField(imageHeightField)
  def imageHeight(value: String) = setField(imageHeightField, value)
  
  def path1(): String = getStringField(path1Field)
  def path1(value: String) = setField(path1Field, value)
  
  def path2(): String = getStringField(path2Field)
  def path2(value: String) = setField(path2Field, value)
  
  def adHtml(): String = getStringField(adHtmlField)
  def adHtml(value: String) = setField(adHtmlField, value)
  
  override def toDBO: DBObject = {
    val dbo = super.toDBO
    dbo.putAll(
      DBObject(
        adNameField.fieldName -> adName,
        adIdField.fieldName -> adId,
        adStateField.fieldName -> adState,
        adTypeField.fieldName -> adType,
        adDescriptionField.fieldName -> adDescription,
        adDescriptionLine1Field.fieldName -> adDescriptionLine1,
        headline1Field.fieldName -> headline1,
        headline2Field.fieldName -> headline2,
        longHeadlineField.fieldName -> longHeadline,
        imageAdNameField.fieldName -> imageAdName,
        imageWidthField.fieldName -> imageWidth,
        imageHeightField.fieldName -> imageHeight,
        path1Field.fieldName -> path1,
        path2Field.fieldName -> path2
      )
    )
    
    dbo
  }
  
  override def fromDBO(dbo: DBObject){
    super.fromDBO(dbo)
    this.parseStringField(dbo, adNameField.fieldName, adName)
    this.parseLongField(dbo, adIdField.fieldName, adId)
    this.parseStringField(dbo, adStateField.fieldName, adState)
    this.parseStringField(dbo, adTypeField.fieldName, adType)
    this.parseStringField(dbo, adDescriptionField.fieldName, adDescription)
    this.parseStringField(dbo, adDescriptionLine1Field.fieldName, adDescriptionLine1)
    this.parseStringField(dbo, headline1Field.fieldName, headline1)
    this.parseStringField(dbo, headline2Field.fieldName, headline2)
    this.parseStringField(dbo, longHeadlineField.fieldName, longHeadline)
    this.parseStringField(dbo, imageAdNameField.fieldName, imageAdName)
    this.parseStringField(dbo, imageWidthField.fieldName, imageWidth)
    this.parseStringField(dbo, imageHeightField.fieldName, imageHeight)
    this.parseStringField(dbo, path1Field.fieldName, path1)
    this.parseStringField(dbo, path2Field.fieldName, path2)
    this.parseStringField(dbo, adHtmlField.fieldName, adHtml)
  }
}

object GoogleAdPerformance {
  lazy val adNameField = new PerformanceField("ad", dimension)
  lazy val adIdField = new PerformanceField("adId", dimension){
    override lazy val visualizationDataType = number
  }
  lazy val adStateField = new PerformanceField("adState", dimension)
  lazy val adTypeField = new PerformanceField("adTypeField", dimension)
  lazy val adDescriptionField = new PerformanceField("adDescription", dimension)
  lazy val adDescriptionLine1Field = new PerformanceField("adDescriptionLine1", dimension)
  lazy val headline1Field = new PerformanceField("headline1", dimension)
  lazy val headline2Field = new PerformanceField("headline2", dimension)
  lazy val longHeadlineField = new PerformanceField("longHeadline", dimension)
  lazy val imageAdNameField = new PerformanceField("imageAdName", dimension)
  lazy val imageWidthField = new PerformanceField("imageWidth", dimension)
  lazy val imageHeightField = new PerformanceField("imageHeight", dimension)
  lazy val path1Field = new PerformanceField("path1", dimension)
  lazy val path2Field = new PerformanceField("path2", dimension)
  
  lazy val adHtmlField = new HtmlPerformanceField(
    "adHtml",
    HtmlPerformanceField.editBuilder(
      adNameField, 
      controllers.google.mcc.account.campaign.adgroup.ad.routes.TextAdController.textAds.url
    )
  )
}