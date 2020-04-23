package util.charts.performance

import com.google.gson.JsonSerializer
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonSerializationContext
import java.lang.reflect.Type
import util.ChartDataAdapter._
import com.google.gson.JsonObject
import scala.collection.JavaConverters._

case class PerformanceGraph(
  val graphType: String,
  val fieldNames: List[String]
)

object PerformanceGraph {
  
  class PerformanceGraphAdapter extends JsonSerializer[PerformanceGraph] 
    with JsonDeserializer[PerformanceGraph] {
    
    override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): PerformanceGraph = {
      val jsonObject: JsonObject = json.getAsJsonObject
      PerformanceGraph(
        jsonObject.get("graphType").getAsString,
        deserializeList(jsonObject.get("fieldNames"), context).map(_.toString)
      )
    }
    
    override def serialize(src: PerformanceGraph, typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
      val json = new JsonObject()
      json.add("graphType", context.serialize(src.graphType))
      json.add("fieldNames", context.serialize(src.fieldNames.asJava))
      
      json
    }
  }
}
