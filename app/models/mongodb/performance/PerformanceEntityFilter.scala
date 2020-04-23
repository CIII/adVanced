package models.mongodb.performance

import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonObject
import util.ChartDataAdapter
import com.google.gson.JsonSerializationContext
import com.google.gson.reflect.TypeToken
import scala.collection.JavaConverters._
import com.google.gson.JsonPrimitive

case class PerformanceEntityFilter(
  var field: PerformanceField,
  var operation: String,            // Filter operation, such as "in", "gt", "lt", "eq", etc.
  var values: List[Any]             // values to filter. for "in", this should be specific values.  for other ones like gt, it should be a number
)

object PerformanceEntityFilter{
  class PerformanceEntityFilterAdapter extends JsonSerializer[PerformanceEntityFilter] 
    with JsonDeserializer[PerformanceEntityFilter] {
    
    override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): PerformanceEntityFilter = {
      val jsonObject: JsonObject = json.getAsJsonObject
      PerformanceEntityFilter(
        context.deserialize(jsonObject.get("field"), new TypeToken[PerformanceField]{}.getType),
        jsonObject.get("operation").getAsString,
        ChartDataAdapter.deserializeList(jsonObject.get("values"), context)
      )

    }
    
    override def serialize(src: PerformanceEntityFilter, typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
      val json = new JsonObject()
      val fieldJson = new JsonObject();
      fieldJson.add("fieldName", new JsonPrimitive(src.field.fieldName))
      fieldJson.add("fieldType", new JsonPrimitive(src.field.fieldType.toString))
      json.add("field", fieldJson)
      json.add("operation", context.serialize(src.operation))
      json.add("values", context.serialize(src.values.asJava))
      
      json
    }
  }
}