package models.mongodb.google

import models.mongodb.performance.PerformanceField
import models.mongodb.performance.PerformanceField.PerformanceFieldType._
import models.mongodb.performance.PerformanceEntity
import models.mongodb.google.GooglePerformance._
import models.mongodb.performance.CalculatedPerformanceField._
import com.mongodb.casbah.Imports._ 
import play.api.Logger
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormat
import models.mongodb.performance.CalculatedPerformanceField

abstract class GooglePerformance extends PerformanceEntity {
  
  def cost(): Double = getDoubleField(costField)
  def cost(value: Double) = setField(costField, value)
  
  def impressions(): Int = getIntField(impField)
  def impressions(value: Int) = setField(impField, value)
  
  def avgPosition(): Double = getDoubleField(avgPosField)
  def avgPosition(value: Double) = setField(avgPosField, value)
  
  def dayOfWeek(): String = getStringField(dowField)
  def dayOfWeek(value: String) = setField(dowField, value)
  
  def accountName(): String = getStringField(accountField)
  def accountName(value: String) = setField(accountField, value)
  
  def interactions(): Int = getIntField(interactionField)
  def interactions(value: Int) = setField(interactionField, value)
  
  def crossDeviceConversions(): Double = getDoubleField(crossDevConvField)
  def crossDeviceConversions(value: Double) = setField(crossDevConvField, value)
  
  def network(): String = getStringField(networkField)
  def network(value: String) = setField(networkField, value)
  
  def gmailClicksToWebsite(): Int = getIntField(gmailClicksField)
  def gmailClicksToWebsite(value: Int) = setField(gmailClicksField, value)
  
  def conversions(): Double = getDoubleField(conversionsField)
  def conversions(value: Double) = setField(conversionsField, value)
  
  def clicks(): Int = getIntField(clicksField)
  def clicks(value: Int) = setField(clicksField, value)
  
  def gmailSaves(): Int = getIntField(gmailSavesField)
  def gmailSaves(value: Int) = setField(gmailSavesField, value)
  
  def gmailForwards(): Int = getIntField(gmailForwardsField)
  def gmailForwards(value: Int) = setField(gmailForwardsField, value)
    
  def device(): String = getStringField(deviceField)
  def device(value: String) = setField(deviceField, value)
  
  def viewThroughConversions(): Double = getDoubleField(viewThroughConvField)
  def viewThroughConversions(value: Double) = setField(viewThroughConvField, value)
  
  def revenue(): Double = getDoubleField(revenueField)
  def revenue(value: Double) = setField(revenueField, value)
  
  def conF(): Int = getIntField(confField)
  def conF(value: Int) = setField(confField, value)
  
  def arrivals(): Int = getIntField(arrivalsField)
  def arrivals(value: Int) = setField(arrivalsField, value)
  
  def conU(): Int = getIntField(conuField)
  def conU(value: Int) = setField(conuField, value)
  
  def duration(): Double = getDoubleField(durationField)
  def duration(value: Double) = setField(durationField, value)
  
  def lpConvU(): Int = getIntField(lpConvUField)
  def lpConvU(value: Int) = setField(lpConvUField, value)
  
  def uFormComplete(): Int = getIntField(uFormCompleteField)
  def uFormComplete(value: Int) = setField(uFormCompleteField, value)
  
  def bounce(): Int = getIntField(bounceField)
  def bounce(value: Int) = setField(bounceField, value)

  /*
   * Calculated Fields
   */
  
  def cpc(): Double = getDoubleField(cpcField, safeDivide(cost, clicks))
  def cpc(value: Double) = setField(cpcField, value)
  
  def cpm(): Double = getDoubleField(cpmField, safeDivide(cost, impressions))
  def cpm(value: Double) = setField(cpmField, value)
  
  def cRate(): Double = getDoubleField(cRateField, 100 * safeDivide(conversions, clicks))
  def cRate(value: Double) = setField(cRateField, value)
  
  def costPerConv(): Double = getDoubleField(costPerConvField, safeDivide(cost, conversions))
  def costPerConv(value: Double) = setField(costPerConvField, value)
  
  def ctr(): Double = getDoubleField(ctrField, 100 * safeDivide(clicks, impressions))
  def ctr(value: Double) = setField(ctrField, value)
  
  def avgDuration(): Double = getDoubleField(avgDurationField, safeDivide(duration, arrivals))
  def avgDuration(value: Double) = setField(avgDurationField, value)
  
  def lpcRate(): Double = getDoubleField(lpcRateField, 100 * safeDivide(lpConvU, arrivals))
  def lpcRate(value: Double) = setField(lpcRateField, value)
  
  def formArrivals(): Double = getDoubleField(formArrField, 100 * safeDivide(uFormComplete, arrivals))
  def formArrivals(value: Double) = setField(formArrField, value)
  
  def formCRate(): Double = getDoubleField(formCRateField, 100 * safeDivide(uFormComplete, lpConvU))
  def formCRate(value: Double) = setField(formCRateField, value)
  
  def vplConU(): Double = getDoubleField(vplConUField, safeDivide(revenue, conU))
  def vplConU(value: Double) = setField(vplConUField, value)
  
  def costConU(): Double = getDoubleField(costConUField, safeDivide(cost, conU))
  def costConU(value: Double) = setField(costConUField, value)
  
  def bounceRate(): Double = getDoubleField(bounceRateField, safeDivide(bounce, arrivals))
  def bounceRate(value: Double) = setField(bounceRateField, value)
  
  def costLPConv(): Double = getDoubleField(costLPConvField, safeDivide(cost, safeDivide(lpConvU, arrivals)))
  def costLPConv(value: Double) = setField(costLPConvField, value)
  
  override def toDBO: DBObject = {
    val dbo = DBObject(
      costField.fieldName -> cost,
      impField.fieldName -> impressions,
      avgPosField.fieldName -> avgPosition,
      dowField.fieldName -> dayOfWeek,
      accountField.fieldName -> accountName,
      interactionField.fieldName -> interactions,
      crossDevConvField.fieldName -> crossDeviceConversions,
      networkField.fieldName -> network,
      gmailClicksField.fieldName -> gmailClicksToWebsite,
      conversionsField.fieldName -> conversions,
      clicksField.fieldName -> clicks,
      gmailSavesField.fieldName -> gmailSaves,
      gmailForwardsField.fieldName -> gmailForwards,
      deviceField.fieldName -> device,
      viewThroughConvField.fieldName -> viewThroughConversions,
      revenueField.fieldName -> revenue,
      confField.fieldName -> conF,
      arrivalsField.fieldName -> arrivals,
      conuField.fieldName -> conU,
      durationField.fieldName -> duration,
      lpConvUField.fieldName -> lpConvU,
      uFormCompleteField.fieldName -> uFormComplete,
      bounceField.fieldName -> bounce
    )
    
    dbo.putAll(super.toDBO)
    dbo
  }
  
  override def fromDBO(dbo: DBObject){
    super.fromDBO(dbo)
    parseDoubleField(dbo, costField.fieldName, cost)
    parseIntField(dbo, impField.fieldName, impressions)
    parseDoubleField(dbo, avgPosField.fieldName, avgPosition)
    parseStringField(dbo, dowField.fieldName, dayOfWeek)
    parseStringField(dbo, accountField.fieldName, accountName)
    parseIntField(dbo, interactionField.fieldName, interactions)
    parseDoubleField(dbo, crossDevConvField.fieldName, crossDeviceConversions)
    parseStringField(dbo, networkField.fieldName, network)
    parseIntField(dbo, gmailClicksField.fieldName, gmailClicksToWebsite)
    parseDoubleField(dbo, conversionsField.fieldName, conversions)
    parseIntField(dbo, clicksField.fieldName, clicks)
    parseIntField(dbo, gmailSavesField.fieldName, gmailSaves)
    parseIntField(dbo, gmailForwardsField.fieldName, gmailForwards)
    parseStringField(dbo, deviceField.fieldName, device)
    parseDoubleField(dbo, viewThroughConvField.fieldName, viewThroughConversions)
    parseDoubleField(dbo, revenueField.fieldName, revenue)
    parseIntField(dbo, confField.fieldName, conF)
    parseIntField(dbo, arrivalsField.fieldName, arrivals)
    parseIntField(dbo, conuField.fieldName, conU)
    parseDoubleField(dbo, durationField.fieldName, duration)
    parseIntField(dbo, lpConvUField.fieldName, lpConvU)
    parseIntField(dbo, uFormCompleteField.fieldName, uFormComplete)
    parseIntField(dbo, bounceField.fieldName, bounce)
    parseDoubleField(dbo, cpcField.fieldName, cpc)
    parseDoubleField(dbo, cpmField.fieldName, cpm)
    parseDoubleField(dbo, cRateField.fieldName, cRate)
    parseDoubleField(dbo, costPerConvField.fieldName, costPerConv)
    parseDoubleField(dbo, avgDurationField.fieldName, avgDuration)
    parseDoubleField(dbo, lpcRateField.fieldName, lpcRate)
    parseDoubleField(dbo, formArrField.fieldName, formArrivals)
    parseDoubleField(dbo, formCRateField.fieldName, formCRate)
    parseDoubleField(dbo, vplConUField.fieldName, vplConU)
    parseDoubleField(dbo, costConUField.fieldName, costConU)
    parseDoubleField(dbo, bounceRateField.fieldName, bounceRate)
    parseDoubleField(dbo, costLPConvField.fieldName, costLPConv)
    parseDoubleField(dbo, ctrField.fieldName, ctr)
  }
}

object GooglePerformance{
  lazy val avgPosField = new PerformanceField("avgPos", dimension)
  lazy val costField = new PerformanceField("cost", measure)
  lazy val impField = new PerformanceField("impressions", measure)
  lazy val dowField = new PerformanceField("dayOfWeek", dimension)
  lazy val bounceField = new PerformanceField("bounce", measure)
  lazy val uFormCompleteField = new PerformanceField("uFormComplete", measure)
  lazy val lpConvUField = new PerformanceField("lpConvU", measure)
  lazy val durationField = new PerformanceField("duration", measure)
  lazy val arrivalsField = new PerformanceField("arrivals", measure)
  lazy val confField = new PerformanceField("conF", measure)
  lazy val revenueField = new PerformanceField("revenue", measure)
  lazy val viewThroughConvField = new PerformanceField("viewThroughConv", measure)
  lazy val deviceField = new PerformanceField("device", dimension)
  lazy val gmailForwardsField = new PerformanceField("gmailForwards", measure)
  lazy val gmailSavesField = new PerformanceField("gmailSaves", measure)
  lazy val clicksField = new PerformanceField("clicks", measure)
  lazy val conversionsField = new PerformanceField("conversions", measure)
  lazy val gmailClicksField = new PerformanceField("gmailClicksToWebsite", measure)
  lazy val networkField = new PerformanceField("network", dimension)
  lazy val crossDevConvField = new PerformanceField("crossDeviceConversions", measure)
  lazy val interactionField = new PerformanceField("interactions", measure)
  lazy val accountField = new PerformanceField("accountName", dimension)
  lazy val conuField = new PerformanceField("conU", measure)
  
  /*
   * Calculated fields
   */
  lazy val cpcField = new CalculatedPerformanceField("cpc", new Divide(new Variable(costField), new Variable(clicksField)))
  lazy val cpmField = new CalculatedPerformanceField("cpm", new Divide(new Variable(costField), new Variable(impField)))
  lazy val cRateField = new CalculatedPerformanceField("cRate", new Multiply(new Divide(new Variable(conversionsField), new Variable(clicksField)), new Literal(100)))
  lazy val costPerConvField = new CalculatedPerformanceField("costPerConv", new Divide(new Variable(costField), new Variable(conversionsField)))
  lazy val ctrField = new CalculatedPerformanceField("ctr", new Multiply(new Divide(new Variable(clicksField), new Variable(impField)), new Literal(100)))
  lazy val avgDurationField = new CalculatedPerformanceField("avgDuration", new Divide(new Variable(durationField), new Variable(arrivalsField)))
  lazy val lpcRateField = new CalculatedPerformanceField("lpcRate", new Multiply(new Divide(new Variable(lpConvUField), new Variable(arrivalsField)), new Literal(100)))
  lazy val formArrField = new CalculatedPerformanceField("formArr", new Multiply(new Divide(new Variable(uFormCompleteField), new Variable(arrivalsField)), new Literal(100)))
  lazy val formCRateField = new CalculatedPerformanceField("formCRate", new Multiply(new Divide(new Variable(uFormCompleteField), new Variable(lpConvUField)), new Literal(100)))
  lazy val vplConUField = new CalculatedPerformanceField("vplConU", new Divide(new Variable(revenueField), new Variable(conuField)))
  lazy val costConUField = new CalculatedPerformanceField("costConu", new Divide(new Variable(costField), new Variable(conuField)))
  lazy val bounceRateField = new CalculatedPerformanceField("bounceRate", new Multiply(new Divide(new Variable(bounceField), new Variable(arrivalsField)), new Literal(100)))
  lazy val costLPConvField = new CalculatedPerformanceField("costLPConv", new Divide(new Variable(costField), new Divide(new Variable(lpConvUField), new Variable(arrivalsField))))
}