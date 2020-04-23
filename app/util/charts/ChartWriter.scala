package util.charts

import play.api.libs.json.Writes
import play.api.libs.json.Json

object ChartWriter {
  implicit val anyValWriter = Writes[Any] (a => a match {
    case v:String => Json.toJson(v)
    case v:Int => Json.toJson(v)
    case v:Double => Json.toJson(v)
    case v:Boolean => Json.toJson(v)
    case v:Long => Json.toJson(v)
    case other => Json.toJson(other.toString)
  })  
}