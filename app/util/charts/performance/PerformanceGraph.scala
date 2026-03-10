package util.charts.performance

import play.api.libs.json._

case class PerformanceGraph(
  val graphType: String,
  val fieldNames: List[String]
)

object PerformanceGraph {

  implicit val performanceGraphFormat: Format[PerformanceGraph] = Json.format[PerformanceGraph]
}
