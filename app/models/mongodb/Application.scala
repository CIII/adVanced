package models.mongodb

import java.io.{File, FileReader, IOException, StringReader}

import au.com.bytecode.opencsv.CSVReader
import play.api.libs.Files
import play.api.libs.json._
import play.api.mvc.MultipartFormData

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.reflect.runtime.universe._


case class UserSetting(
  var Username: String,
  var ShowAlertsOnDashboard: Boolean,
  var AlertTypes: List[String],
  var AlertSeverity: List[String]
)


case class NotificationConfiguration(
  var Username: String,
  var ShowAlertsOnDashboard: Boolean,
  var AlertTypes: List[String],
  var AlertSeverity: List[String],
  var EnableSmsAlerts: Boolean,
  var Sms: String,
  var EnableEmailAlerts: Boolean,
  var Email: String
)

object Utilities {
  def getCaseClassParameter[T: TypeTag] = typeOf[T].members.collect {
    case m: MethodSymbol if m.isCaseAccessor => m
  }.toList

  def getMethodName(field: MethodSymbol): String = {
    field.toString.split(" ")(1)
  }

  def isOptionalParameter(field: MethodSymbol): Boolean = {
    field.returnType.typeSymbol.toString.contains("Option")
  }

  def bulkImport(bulk: MultipartFormData.FilePart[Files.TemporaryFile], fields: List[MethodSymbol]):
  collection.mutable.MutableList[(collection.mutable.Map[String, String], String)] = {
    val filename = "/tmp/bulk/" + bulk.filename
    val contentType = bulk.contentType
    bulk.ref.moveTo(new File(filename), replace = true)

    val field_names = fields.map(field => getMethodName(field))
    var required_fields = fields.filter(field => !isOptionalParameter(field)).map(field => getMethodName(field)).filter(field_name => field_name != "_id")

    val fields_map = collection.mutable.Map[String, Int]()
    val data_map_list = collection.mutable.MutableList[(collection.mutable.Map[String, String], String)]()

    var parse_header = true
    var action_index = -1

    val reader = new CSVReader(new FileReader(filename))
    val rows = reader.readAll()
    for (row <- rows) {
      if (parse_header) {
        for ((col: String, index: Int) <- row.asInstanceOf[List[String]].zipWithIndex) {
          if (field_names.contains(col)) fields_map(col) = index
          if (col == "action") action_index = index
        }
        required_fields = required_fields.filter(field_name => !fields_map.keys.contains(field_name))
        if (required_fields.nonEmpty) throw new IOException("CSV file does not have all required columns: " + required_fields.toString())
        parse_header = false
      } else {
        val data_map = collection.mutable.Map[String, String]()
        for ((k: String, v: Int) <- fields_map) {
          data_map(k) = row.asInstanceOf[List[String]](v).toString
        }
        var action = row.asInstanceOf[List[String]](action_index).toString
        if (action_index < 0) action = "new"
        val tup = (data_map, action)
        data_map_list += tup
      }
    }
    data_map_list
  }

  def csvToJson(csv_raw: String): List[JsObject] = {
    val reader = new CSVReader(new StringReader(csv_raw))
    var header = ListBuffer[String]()
    var result = ListBuffer[JsObject]()
    val rows = reader.readAll
    for(i <- 0 until rows.length) {
      i match {
        case 0 =>
          for (col <- rows(i).asInstanceOf[Array[String]]) {
            header += col
          }
        case _ =>
          var jsObj = Json.obj()
          rows(i).asInstanceOf[Array[String]].zipWithIndex.foreach {
            case (field, field_index) =>
              jsObj = jsObj + (header(field_index).toLowerCase -> Json.toJson(field))
          }
          result += jsObj
      }
    }
    result.toList
  }
}