package models.mongodb.lynx

import models.mongodb.performance.PerformanceEntity
import models.mongodb.performance.PerformanceField
import models.mongodb.performance.PerformanceField.PerformanceFieldType._
import models.mongodb.performance.CalculatedPerformanceField
import models.mongodb.performance.CalculatedPerformanceField._
import models.mongodb.lynx.TQReportingPerformance._
import org.joda.time.DateTime
import com.mongodb.casbah.Imports._

class TQReportingPerformance extends PerformanceEntity{
  
  override def date(): DateTime = DateTime.parse(getStringField(createdAtField))
  override def date(value: DateTime) = setField(createdAtField, value.toString(dtf))
  
  override def id(): String = getStringField(sessionIdField)
  override def id(value: String) = setField(sessionIdField, value)
  
  def trafficSource(): String = getStringField(trafficSourceField)
  def trafficSource(value: String) = setField(trafficSourceField, value)
  
  def createdAt(): DateTime = date()
  def createdAt(value: DateTime) = date(value)
  
  def cost(): Double = getDoubleField(costField, 0.0)
  def cost(value: Double) = setField(costField, value)
  
  def clicks(): Int = getIntField(clicksField, 0)
  def clicks(value: Double) = setField(clicksField, value)
  
  def conU(): Int = getIntField(conUField)
  def conU(value: Int) = setField(conUField, value)
  
  def costConU(): Double = getDoubleField(costConUField, safeDivide(cost, conU))
  def costConU(value: Double) = setField(costConUField, value)
  
  def sessionId(value: String) = setField(sessionIdField, value)
  def arrivals(value: Int) = setField(arrivalsField, value)
  def revenue(value: Double) = setField(revenueField, value)
  def pageLoads(value: Int) = setField(pageLoadsField, value)
  def bounces(value: Int) = setField(bouncesField, value)
  def conversions(value: Double) = setField(conversionsField, value)
  def cRate(value: Double) = setField(cRateField, value)
  def lpConvU(value: Double) = setField(lpConvUField, value)
  def lpCRate(value: Double) = setField(lpCRateField, value)
  def vplConU(value: Double) = setField(vplConUField, value)
  
  override def toDBO(): DBObject = {
    throw new Exception("Cannot create DBO object for TQReportingPerformance. TQReporting should be used")
  }
  
  override def fromDBO(dbo: DBObject){
    super.fromDBO(dbo)
    parseDateTime(dbo, createdAtField.fieldName, date)
    parseStringField(dbo, sessionIdField.fieldName, id)
    parseStringField(dbo, trafficSourceField.fieldName, trafficSource, "")
    parseIntField(dbo, arrivalsField.fieldName, arrivals)
    parseDoubleField(dbo, revenueField.fieldName, revenue)
    parseIntField(dbo, pageLoadsField.fieldName, pageLoads)
    parseIntField(dbo, bouncesField.fieldName, bounces)
    parseDoubleField(dbo, conversionsField.fieldName, conversions)
    parseDoubleField(dbo, cRateField.fieldName, cRate)
    parseIntField(dbo, conUField.fieldName, conU)
    parseDoubleField(dbo, lpConvUField.fieldName, lpConvU)
    parseDoubleField(dbo, lpCRateField.fieldName, lpCRate)
    parseDoubleField(dbo, vplConUField.fieldName, vplConU)
  }
}

object TQReportingPerformance{
  lazy val createdAtField = new PerformanceField("created_at", dimension)
  lazy val sessionIdField = new PerformanceField("session_id", dimension)
  lazy val trafficSourceField = new PerformanceField("utm_source", dimension)
  lazy val arrivalsField = new PerformanceField("arrivals", measure)
  lazy val revenueField = new PerformanceField("revenue", measure)
  lazy val pageLoadsField = new PerformanceField("page_loaded", measure)
  lazy val bouncesField = new PerformanceField("bounce", measure)
  lazy val conversionsField = new PerformanceField("conversion", measure)
  lazy val conUField = new PerformanceField("conu", measure)
  lazy val lpConvUField = new PerformanceField("lp_conv_u", measure)
  lazy val lpCRateField = new CalculatedPerformanceField("lpCRate", new Multiply(new Divide(new Variable(lpConvUField), new Variable(arrivalsField)), new Literal(100)))
  lazy val cRateField = new CalculatedPerformanceField("cRate", new Multiply(new Divide(new Variable(conversionsField), new Variable(pageLoadsField)), new Literal(100)))
  lazy val vplConUField = new CalculatedPerformanceField("vplConU", new Divide(new Variable(revenueField), new Variable(conUField)))
  
  // External fields (will always be null or 0 until explicitly set)
  lazy val costField = new PerformanceField("cost", measure)
  lazy val clicksField = new PerformanceField("clicks", measure)
  lazy val costConUField = new PerformanceField("costConU", measure){
    override def dependantFields(): List[PerformanceField] = List(conUField)
  }
}