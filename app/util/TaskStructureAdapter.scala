package util

import java.lang.reflect.Type

import Shared.Shared._
import com.google.gson._
import org.joda.time.format.DateTimeFormat

import scala.collection.JavaConverters._

class TaskStructureAdapter extends JsonSerializer[TaskStructure] with JsonDeserializer[TaskStructure] {

  val formatter = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss")

  override def serialize(src: TaskStructure, typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
    val result = new JsonObject()

    result.add("id", context.serialize(src.id))
    result.add("data", context.serialize(src.data.asJava))
    result.add("start_time", context.serialize(formatter.print(src.startTime)))
    result.add("complete_time", context.serialize(
      src.completeTime match {
        case Some(x) =>
          formatter.print(src.completeTime.get)
        case _ =>
          None
      }
    ))
    result.add("complete", context.serialize(src.complete))
    result.add("processes", context.serialize(src.processes.asJava))
    result
  }

  @throws[JsonParseException]
  override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): TaskStructure = {
    val jsonObject = json.getAsJsonObject

    var data = List[PendingCacheStructure]()

    var processes = List[Process]()

    jsonObject.get("data")
      .getAsJsonArray
      .asScala
      .foreach { x =>
        val jsonObj = x.asInstanceOf[JsonObject]
        data = data :+ PendingCacheStructure(
          id=jsonObj.get("id").getAsLong,
          changeCategory=ChangeCategory.withName(jsonObj.get("changeCategory").getAsString),
          trafficSource=TrafficSource.withName(jsonObj.get("trafficSource").getAsString),
          changeType=ChangeType.withName(jsonObj.get("changeType").getAsString),
          changeData=context.deserialize[com.mongodb.BasicDBObject](
            jsonObj.get("changeData")
              .getAsJsonObject.get("properties"),
            Class.forName(
              jsonObj.get("changeData")
                .getAsJsonObject
                .get("type")
                .getAsString
            )
          )
        )
    }

    jsonObject.get("processes")
    .getAsJsonArray
    .asScala
    .foreach { x =>
      val jsonObj = x.asInstanceOf[JsonObject]
      processes = processes :+ Process(
        changeDataId=jsonObj.get("changeDataId").getAsLong,
        subProcesses=jsonObj.get("subProcesses").getAsInt,
        completedSubProcesses=jsonObj.get("completedSubProcesses").getAsInt
      )
    }

    TaskStructure(
      id=jsonObject.get("id").getAsLong,
      user=jsonObject.get("user").getAsString,
      data=data,
      startTime=formatter.parseDateTime(jsonObject.get("start_time").getAsString),
      completeTime=jsonObject.get("complete_time").isJsonObject match {
        case true => None
        case false =>
          Some(formatter.parseDateTime(jsonObject.get("complete_time").getAsString))
      },
      complete=jsonObject.get("complete").getAsBoolean,
      processes=processes
    )
  }
}