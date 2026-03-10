package models.mongodb.performance

import models.mongodb.performance.PerformanceField.PerformanceFieldType.PerformanceFieldType
import models.mongodb.performance.PerformanceField.PerformanceFieldType.dimension
import models.mongodb.performance.PerformanceField.VisualizationDataType.VisualizationDataType
import models.mongodb.performance.PerformanceField.VisualizationDataType.{string, number}
import org.mongodb.scala.bson.Document
import play.api.libs.json._

class PerformanceField (
  val fieldName: String,
  val fieldType: PerformanceFieldType
){
  def dependantFields(): List[PerformanceField] = List()
  def projectionQueryObject(): Document = {
    if(fieldType == dimension){
      Document(fieldName -> ("$_id." + fieldName))
    } else {
      Document(fieldName -> ("$" + fieldName))
    }
  }

  lazy val visualizationDataType: VisualizationDataType = {
    if(fieldType == dimension){
      string
    } else {
      number
    }
  }

}

object PerformanceField {

  /**
   * Type of performance field.  This will define how we format
   * the field, as well as how we roll up the data in the mongodb query
   */
  object PerformanceFieldType extends Enumeration {
    type PerformanceFieldType = Value
    val dimension, measure = Value
  }

  object VisualizationDataType extends Enumeration {
    type VisualizationDataType = Value
    val string, number = Value
  }

  implicit val performanceFieldFormat: Format[PerformanceField] = new Format[PerformanceField] {
    override def reads(json: JsValue): JsResult[PerformanceField] = {
      for {
        fieldName <- (json \ "fieldName").validate[String]
        fieldType <- (json \ "fieldType").validate[String].map(PerformanceFieldType.withName)
      } yield new PerformanceField(fieldName, fieldType)
    }

    override def writes(src: PerformanceField): JsValue = {
      Json.obj(
        "fieldName" -> src.fieldName,
        "fieldType" -> src.fieldType.toString
      )
    }
  }
}
