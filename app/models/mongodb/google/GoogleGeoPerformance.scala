package models.mongodb.google

import models.mongodb.performance.PerformanceField
import models.mongodb.performance.PerformanceField.PerformanceFieldType._
import models.mongodb.performance.PerformanceField.VisualizationDataType.number
import models.mongodb.google.GoogleGeoPerformance._
import org.mongodb.scala.bson.Document

class GoogleGeoPerformance extends GoogleAdGroupPerformance{

  def region(): String = getStringField(regionField)
  def region(value: String) = setField(regionField, value)

  def city(): String = getStringField(cityField)
  def city(value: String) = setField(cityField, value)

  def mostSpecificLocation(): String = getStringField(mostSpecificLocationField)
  def mostSpecificLocation(value: String) = setField(mostSpecificLocationField, value)

  def isTargetable(): String = getStringField(targetableField)
  def isTargetable(value: String) = setField(targetableField, value)

  def clientName(): String = getStringField(clientNameField)
  def clientName(value: String) = setField(clientNameField, value)

  def currency(): String = getStringField(currencyField)
  def currency(value: String) = setField(currencyField, value)

  def countryTerritory(): String = getStringField(countryTerritoryField)
  def countryTerritory(value: String) = setField(countryTerritoryField, value)

  def metroArea(): String = getStringField(metroAreaField)
  def metroArea(value: String) = setField(metroAreaField, value)

  def customerId(): String = getStringField(customerIdField)
  def customerId(value: String) = setField(customerIdField, value)

  override def toDocument: Document = {
    val base = super.toDocument
    base ++ Document(
      regionField.fieldName -> region,
      cityField.fieldName -> city,
      mostSpecificLocationField.fieldName -> mostSpecificLocation,
      targetableField.fieldName -> isTargetable,
      clientNameField.fieldName -> clientName,
      currencyField.fieldName -> currency,
      countryTerritoryField.fieldName -> countryTerritory,
      metroAreaField.fieldName -> metroArea,
      customerIdField.fieldName -> customerId
    )
  }

  override def fromDocument(doc: Document){
    super.fromDocument(doc)
    this.parseStringField(doc, regionField.fieldName, region)
    this.parseStringField(doc, cityField.fieldName, city)
    this.parseStringField(doc, mostSpecificLocationField.fieldName, mostSpecificLocation)
    this.parseStringField(doc, targetableField.fieldName, isTargetable)
    this.parseStringField(doc, clientNameField.fieldName, clientName)
    this.parseStringField(doc, currencyField.fieldName, currency)
    this.parseStringField(doc, countryTerritoryField.fieldName, countryTerritory)
    this.parseStringField(doc, metroAreaField.fieldName, metroArea)
    this.parseStringField(doc, customerIdField.fieldName, customerId)
  }
}

object GoogleGeoPerformance {
  lazy val regionField = new PerformanceField("region", dimension){
    override lazy val visualizationDataType = number
  }

  lazy val cityField = new PerformanceField("city", dimension)
  lazy val mostSpecificLocationField = new PerformanceField("mostSpecificLocation", dimension)
  lazy val targetableField = new PerformanceField("targetable", dimension)
  lazy val clientNameField = new PerformanceField("clientName", dimension)
  lazy val currencyField = new PerformanceField("currency", dimension)
  lazy val countryTerritoryField = new PerformanceField("countryTerritory", dimension)
  lazy val metroAreaField = new PerformanceField("metroArea", dimension)
  lazy val customerIdField = new PerformanceField("customerId", dimension)
}
