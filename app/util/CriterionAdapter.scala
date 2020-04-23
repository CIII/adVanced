package util

import java.lang.reflect.Type

import com.google.api.ads.adwords.axis.v201609.cm.Criterion
import com.google.gson._

class CriterionAdapter extends JsonSerializer[Criterion] with JsonDeserializer[Criterion] {

  override def serialize(src: Criterion, typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
    val result: JsonObject = new JsonObject()
    result.add("type", new JsonPrimitive(src.getClass.getName))
    result.add("properties", context.serialize(src, src.getClass))
    result
  }

  @throws[JsonParseException]
  override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Criterion = {
    val jsonObject: JsonObject = json.getAsJsonObject
    val typestring = jsonObject.get("type").getAsString
    val element: JsonElement = jsonObject.get("properties")

    try {
      context.deserialize(element, Class.forName(typestring))
    } catch {
      case cnfe: ClassNotFoundException =>
      throw new JsonParseException("Unknown element type: " + typestring, cnfe)
    }
  }
}