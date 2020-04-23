package models.mongodb.performance

import com.mongodb.casbah.Imports._
import models.mongodb.performance.PerformanceField.PerformanceFieldType._
import models.mongodb.performance.PerformanceEntity._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormat

abstract class PerformanceEntity {
  val dtf: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
  
  /**
   * Field map that defines the currently present fields in the entity.
   */
  var fieldMap: Map[String, PerformanceValue] = Map()
  
  /**
   * Helper methods for retrieving values from the fieldMap
   */
  def getStringField(field: PerformanceField): String = fieldMap(field.fieldName).value.toString
  def getDoubleField(field: PerformanceField): Double = fieldMap(field.fieldName).value.asInstanceOf[Double]
  def getIntField(field: PerformanceField): Int = fieldMap(field.fieldName).value.asInstanceOf[Int]
  def getLongField(field: PerformanceField): Long = fieldMap(field.fieldName).value.asInstanceOf[Long]
  def getDoubleField(field: PerformanceField, default: Double): Double = {
    if(fieldMap.contains(field.fieldName)){
      getDoubleField(field)
    } else {
      default
    }
  }
  
  def getIntField(field: PerformanceField, default: Int): Int = {
    if(fieldMap.contains(field.fieldName)){
      getIntField(field)
    } else {
      default
    }
  }
  
  /**
   * Helper methods for storing values in the fieldMap
   */
  def setField(field: PerformanceField, value: Any){
    fieldMap += (field.fieldName -> PerformanceValue(field, value))
  }
  
  /**
   * Helper method which returns true if the field is in the field map,
   * and false otherwise
   */
  def fieldExists(field: PerformanceField): Boolean = {
    fieldMap.contains(field.fieldName)
  }
  
  /**
   * Methods requiring implementation that define the dbo IO 
   */
  def toDBO: DBObject = DBObject(
    idField.fieldName -> id,
    dateField.fieldName -> date.toString(dtf)
  )
      
  def fromDBO(dbo: DBObject){
    parseDateTime(dbo, dateField.fieldName, date)
    parseStringField(dbo, idField.fieldName, id)
  }
    
  /**
   * Helper methods for implementing the to/from DBO functionality
   */
  def parseDateTime(dbo: DBObject, field: String, func: (DateTime) => Unit){
    if(!dbo.getAs[String](field).isEmpty){
      func(DateTime.parse(dbo.getAs[String](field).get))
    }
  }
  
  def parseStringField(dbo: DBObject, field: String,  func: (String) => Unit){
    if(!dbo.getAs[String](field).isEmpty){
      func(dbo.getAs[String](field).get)
    }
  }
  
  def parseStringField(dbo: DBObject, field: String,  func: (String) => Unit, default: String){
    if(!dbo.getAs[String](field).isEmpty){
      func(dbo.getAs[String](field).get)
    } else {
      func(default)
    }
  }
  
  def parseDoubleField(dbo: DBObject, field: String, func: (Double) => Unit){
    if(!dbo.getAs[Double](field).isEmpty){
      func(dbo.getAs[Double](field).get)
    } else if(!dbo.getAs[Int](field).isEmpty){
      func(dbo.getAs[Int](field).get.toDouble)
    }
  }
  
  def parseDoubleField(dbo: DBObject, field: String, func: (Double) => Unit, default: Double){
    if(!dbo.getAs[Double](field).isEmpty){
      func(dbo.getAs[Double](field).get)
    } else if(!dbo.getAs[Int](field).isEmpty){
      func(dbo.getAs[Int](field).get.toDouble)
    } else {
      func(default)
    }
  }
  
  def parseLongField(dbo: DBObject, field: String, func: (Long) => Unit){
    if(!dbo.getAs[Long](field).isEmpty){
      func(dbo.getAs[Long](field).get)
    } else if(!dbo.getAs[Int](field).isEmpty){
      func(dbo.getAs[Int](field).get.toLong)
    }
  }
  
  def parseIntField(dbo: DBObject, field: String, func: (Int) => Unit){
    if(!dbo.getAs[Int](field).isEmpty){
      func(dbo.getAs[Int](field).get)
    } 
  }
  
  def toList(fields: List[PerformanceField]): List[Any] = {
    fields.map { field => fieldMap(field.fieldName).value }
  }
  
  /**
   * All performance charts must have an Id & Date field
   */
  def date(): DateTime = DateTime.parse(getStringField(dateField))
  def date(value: DateTime) = setField(dateField, value.toString(dtf))
  
  def id(): String = getStringField(idField)
  def id(value: String) = setField(idField, value)
  
  /**  
   *   Divide, checking to make sure that we do not divide by 0.  in the
   *   case where the divisor is 0 we just return 0
   */
  def safeDivide(numerator: Double, divisor: Double): Double = {
    if(divisor == 0){
      0
    } else {
      numerator / divisor
    }
  }
}

object PerformanceEntity{
  lazy val dateField = new PerformanceField("date", dimension)
  lazy val idField = new PerformanceField("entityId", dimension)
}

