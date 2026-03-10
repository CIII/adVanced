package util.charts

import play.api.libs.json._

case class ChartData (
  data: List[List[Any]]
)

object ChartData {

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

  implicit val chartDataFormat: Format[ChartData] = new Format[ChartData] {
    override def reads(json: JsValue): JsResult[ChartData] = {
      (json \ "data").validate[List[List[Any]]](Reads.list(Reads.list(anyReads))).map(ChartData(_))
    }

    override def writes(src: ChartData): JsValue = {
      Json.obj(
        "data" -> JsArray(src.data.map(row => JsArray(row.map(v => anyWrites.writes(v)))))
      )
    }
  }
}
