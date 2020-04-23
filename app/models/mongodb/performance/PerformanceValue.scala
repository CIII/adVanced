package models.mongodb.performance

case class PerformanceValue(
  val field: PerformanceField,
  var value: Any
)