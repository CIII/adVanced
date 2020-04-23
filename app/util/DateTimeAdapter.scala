package util

import java.lang.reflect.Type
import com.google.gson._
import org.joda.time._

class DateTimeAdapter extends JsonSerializer[DateTime] with JsonDeserializer[DateTime] {
  
  def serialize(src: DateTime, srcType: Type, context: JsonSerializationContext): JsonElement = {
    new JsonPrimitive(src.toString)
  }
  
  def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): DateTime = {
    new DateTime(json.getAsLong)
  }
}