package models.mongodb.performance

import models.mongodb.performance.PerformanceField.PerformanceFieldType.PerformanceFieldType
import models.mongodb.performance.PerformanceField.PerformanceFieldType.dimension
import models.mongodb.performance.PerformanceField.VisualizationDataType.VisualizationDataType
import models.mongodb.performance.PerformanceField.VisualizationDataType.{string, number}
import com.google.gson.JsonSerializer
import com.google.gson.JsonDeserializer
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import java.lang.reflect.Type
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.mongodb.casbah.Imports._

class PerformanceField (
  val fieldName: String,
  val fieldType: PerformanceFieldType
){
  def dependantFields(): List[PerformanceField] = List()
  def projectionQueryObject(): DBObject = {
    if(fieldType == dimension){
      DBObject(fieldName -> ("$_id." + fieldName)) 
    } else {
      DBObject(fieldName -> ("$" + fieldName))
    }
  }
  
  lazy val visualizationDataType: VisualizationDataType = {
    if(fieldType == dimension){
      string
    } else {
      number
    }
  }
  
}

object PerformanceField {
  
  /**
   * Type of performance field.  This will define how we format
   * the field, as well as how we roll up the data in the mongodb query
   */
  object PerformanceFieldType extends Enumeration {
    type PerformanceFieldType = Value
    val dimension, measure = Value
  }
  
  object VisualizationDataType extends Enumeration {
    type VisualizationDataType = Value
    val string, number = Value
  }
  
  class PerformanceFieldAdapter extends JsonSerializer[PerformanceField]
    with JsonDeserializer[PerformanceField] {
    
    override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): PerformanceField = {
      val jsonObject: JsonObject = json.getAsJsonObject
      new PerformanceField(
        jsonObject.get("fieldName").getAsString,
        PerformanceFieldType.withName(jsonObject.get("fieldType").getAsString)
      )
    }
    
    override def serialize(src: PerformanceField, typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
      val json = new JsonObject()
      json.add("fieldName", context.serialize(src.fieldName))
      json.add("fieldType", context.serialize(src.fieldType.toString))
      json
    }
  }
}