package util

import java.lang.reflect.Type

import Shared.Shared._
import com.google.gson._

class PendingCacheStructureAdapter extends JsonSerializer[PendingCacheStructure] with JsonDeserializer[PendingCacheStructure] {

  override def serialize(src: PendingCacheStructure, typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
    val result: JsonObject = new JsonObject()
    val change_data_obj: JsonObject = new JsonObject()
    change_data_obj.add("type", new JsonPrimitive(src.changeData.getClass.getName))
    change_data_obj.add("properties", context.serialize(src.changeData, src.changeData.getClass))

    result.add("id", context.serialize(src.id))
    result.add("changeCategory", context.serialize(src.changeCategory.toString))
    result.add("trafficSource", context.serialize(src.trafficSource.toString))
    result.add("changeType", context.serialize(src.changeType.toString))
    result.add("changeData", change_data_obj)
    result
  }

  @throws[JsonParseException]
  override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): PendingCacheStructure = {
    val jsonObject: JsonObject = json.getAsJsonObject
    val changeDataJsonObject: JsonObject = jsonObject.get("changeData").getAsJsonObject
    val changeDataType: String = changeDataJsonObject.get("type").getAsString
    val changeData = context.deserialize[com.mongodb.BasicDBObject](changeDataJsonObject.get("properties"), Class.forName(changeDataType))

    PendingCacheStructure(
    id=context.deserialize(jsonObject.get("id"), classOf[Long]),
    changeCategory=ChangeCategory.withName(context.deserialize(jsonObject.get("changeCategory"), classOf[String])),
    trafficSource=TrafficSource.withName(context.deserialize(jsonObject.get("trafficSource"), classOf[String])),
    changeType=ChangeType.withName(context.deserialize(jsonObject.get("changeType"), classOf[String])),
    changeData=changeData
    )
  }
}