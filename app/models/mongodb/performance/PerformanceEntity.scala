package models.mongodb.performance

import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
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
   * Methods requiring implementation that define the Document IO
   */
  def toDocument: Document = Document(
    idField.fieldName -> id,
    dateField.fieldName -> date.toString(dtf)
  )

  def fromDocument(doc: Document){
    parseDateTime(doc, dateField.fieldName, date)
    parseStringField(doc, idField.fieldName, id)
  }

  /**
   * Helper methods for implementing the to/from Document functionality
   */
  def parseDateTime(doc: Document, field: String, func: (DateTime) => Unit){
    if(Option(doc.getString(field)).nonEmpty){
      func(DateTime.parse(doc.getString(field)))
    }
  }

  def parseStringField(doc: Document, field: String, func: (String) => Unit){
    if(Option(doc.getString(field)).nonEmpty){
      func(doc.getString(field))
    }
  }

  def parseStringField(doc: Document, field: String, func: (String) => Unit, default: String){
    if(Option(doc.getString(field)).nonEmpty){
      func(doc.getString(field))
    } else {
      func(default)
    }
  }

  def parseDoubleField(doc: Document, field: String, func: (Double) => Unit){
    val value = doc.get[org.bson.BsonValue](field).orNull
    if(value != null) {
      if(value.isDouble) {
        func(value.asDouble().getValue)
      } else if(value.isInt32) {
        func(value.asInt32().getValue.toDouble)
      } else if(value.isInt64) {
        func(value.asInt64().getValue.toDouble)
      }
    }
  }

  def parseDoubleField(doc: Document, field: String, func: (Double) => Unit, default: Double){
    val value = doc.get[org.bson.BsonValue](field).orNull
    if(value != null) {
      if(value.isDouble) {
        func(value.asDouble().getValue)
      } else if(value.isInt32) {
        func(value.asInt32().getValue.toDouble)
      } else if(value.isInt64) {
        func(value.asInt64().getValue.toDouble)
      } else {
        func(default)
      }
    } else {
      func(default)
    }
  }

  def parseLongField(doc: Document, field: String, func: (Long) => Unit){
    val value = doc.get[org.bson.BsonValue](field).orNull
    if(value != null) {
      if(value.isInt64) {
        func(value.asInt64().getValue)
      } else if(value.isInt32) {
        func(value.asInt32().getValue.toLong)
      }
    }
  }

  def parseIntField(doc: Document, field: String, func: (Int) => Unit){
    val value = doc.get[org.bson.BsonValue](field).orNull
    if(value != null && value.isInt32) {
      func(value.asInt32().getValue)
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
