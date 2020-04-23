package util.charts.client

import scala.reflect.runtime.universe._

class ChartItem(
    var cc: AnyRef
){
  val ccMap: Map[String, Any] = (ccToMap(cc) - "$outer")
  
  def getRow: List[Any] = {
    ccMap.keys.toList.sorted.map(field => ccMap(field))
  }
  
  def getRow(fields: List[String]): List[Any] ={
    fields.map(field => ccMap(field))
  }
  
  def getValue(field: String): Any = {
    ccMap(field)
  }
  
  def ccToMap(cc: AnyRef) = (Map[String, Any]() /: cc.getClass.getDeclaredFields) {
    (fieldMap, field) =>
      field.setAccessible(true)
      var fieldValue = field.get(cc) match {
        case (opt): Option[Any] => opt match {
          case Some(optVal) => optVal
          case None => ""
        }
        case (any): Any => any 
        case null => ""
      }
      fieldMap + (field.getName -> fieldValue)
  }
}

