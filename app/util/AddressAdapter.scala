package util

import java.lang.reflect.Type

import com.google.api.ads.adwords.axis.v201609.cm.Address
import com.google.gson._

class AddressAdapter extends JsonSerializer[Address] with JsonDeserializer[Address] {
  override def serialize(src: Address, typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
    val result: JsonObject = new JsonObject()
    result.add("type", new JsonPrimitive(src.getClass.getName))
    result.add("properties", context.serialize(src, src.getClass))
    result
  }

  @throws[JsonParseException]
  override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Address = {
    val jsonObject: JsonObject = json.getAsJsonObject
    val typestring: String = jsonObject.get("type").getAsString
    val element: JsonElement = jsonObject.get("properties")

    try {
      context.deserialize(element, Class.forName(typestring))
    } catch {
      case cnfe: ClassNotFoundException =>
        throw new JsonParseException("Unknown element type: " + typestring, cnfe)
    }
  }
}