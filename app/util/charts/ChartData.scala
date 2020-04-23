package util.charts

import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonObject
import util.ChartDataAdapter

case class ChartData (
  data: List[List[Any]]
)

object ChartData {
  
  class ChartDataAdapter extends JsonSerializer[ChartData]
    with JsonDeserializer[ChartData] {
    
    override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ChartData = {
      val jsonObject: JsonObject = json.getAsJsonObject
      ChartData(
          ChartDataAdapter.deserializeListOfLists(jsonObject.get("data"), context)
      )
    }
    
    override def serialize(src: ChartData, typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
      var json = new JsonObject()
      json.add("data", ChartDataAdapter.serializeListOfLists(src.data, context))
      
      json
    }
  }
}