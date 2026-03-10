package models.mongodb.performance

import play.api.libs.json._

case class PerformanceEntityFilter(
  field: PerformanceField,
  operation: String,                // Filter operation, such as "in", "gt", "lt", "eq", etc.
  values: List[Any]                 // values to filter. for "in", this should be specific values.  for other ones like gt, it should be a number
)

object PerformanceEntityFilter {

  private def anyReads: Reads[Any] = Reads {
    case JsString(s) => JsSuccess(s)
    case JsNumber(n) =>
      if (n.isValidInt) JsSuccess(n.toInt)
      else if (n.isValidLong) JsSuccess(n.toLong)
      else JsSuccess(n.toDouble)
    case JsBoolean(b) => JsSuccess(b)
    case JsNull => JsSuccess(null)
    case other => JsError(s"Unsupported type: $other")
  }

  private def anyWrites: Writes[Any] = Writes {
    case s: String => JsString(s)
    case n: Int => JsNumber(n)
    case n: Long => JsNumber(n)
    case n: Double => JsNumber(n)
    case n: BigDecimal => JsNumber(n)
    case b: Boolean => JsBoolean(b)
    case null => JsNull
    case other => JsString(other.toString)
  }

  implicit val performanceEntityFilterFormat: Format[PerformanceEntityFilter] = new Format[PerformanceEntityFilter] {
    override def reads(json: JsValue): JsResult[PerformanceEntityFilter] = {
      for {
        field <- (json \ "field").validate[PerformanceField](PerformanceField.performanceFieldFormat)
        operation <- (json \ "operation").validate[String]
        values <- (json \ "values").validate[List[Any]](Reads.list(anyReads))
      } yield PerformanceEntityFilter(field, operation, values)
    }

    override def writes(src: PerformanceEntityFilter): JsValue = {
      Json.obj(
        "field" -> Json.toJson(src.field)(PerformanceField.performanceFieldFormat),
        "operation" -> src.operation,
        "values" -> JsArray(src.values.map(v => anyWrites.writes(v)))
      )
    }
  }
}
