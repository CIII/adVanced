package util.charts

import util.charts.performance.PerformanceGraph
import util.ChartDataAdapter
import play.api.libs.json._ 
import play.api.libs.json.Reads._ 
import play.api.libs.json.Writes._ 
import play.api.libs.functional.syntax._ 
import com.google.gson._
import scala.collection.JavaConverters._
import java.lang.reflect.Type
import Shared.Shared._
import com.google.gson.reflect.TypeToken
import org.joda.time.DateTime
import models.mongodb.performance.PerformanceEntityFilter
import be.objectify.deadbolt.scala.AuthenticatedRequest
import models.mongodb.UserAccount
import play.api.mvc.AnyContent
import models.mongodb.SessionSettings
import models.mongodb.performance.PerformanceField

case class ChartMetaData(
   var page: Int,                             // which page to display
   var pageSize: Int,                         // number of records to display on the page
   var orderBy: String,                       // field name of column to order by
   var orderDir: Int,                         // 1 = ASC, -1 = DESC
   val reloadUri: String,                     // uri passed to the UI that specifies which route to call on page reload
   var chartFilters: List[PerformanceEntityFilter],      // selected fields in filter
   var showColumns: List[String],             // list of column field names specifying which should be displayed.  Any others will be hidden 
   var startDate: Option[DateTime],           // for date based tables, start date of records to show. Specify None to retrieve all records
   var endDate: Option[DateTime],             // for date based tables, end date of records to show.  Specify None to retrieve all recods
   var graphs: List[PerformanceGraph],          // list of graphs currently displayed on the page
   var editMode: Boolean                      // true/false if we are in edit mode.  edit mode forces constraints on the data.
)


object ChartMetaData {
  
  class ChartMetaDataAdapter extends JsonSerializer[ChartMetaData] 
    with JsonDeserializer[ChartMetaData] {
    
    override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ChartMetaData = {
      val jsonObject: JsonObject = json.getAsJsonObject
      
      ChartMetaData(
        jsonObject.get("page").getAsInt,
        jsonObject.get("pageSize").getAsInt,
        jsonObject.get("orderBy").getAsString,
        jsonObject.get("orderDir").getAsInt,
        jsonObject.get("reloadUri").getAsString,
        deserializeRowFilters(jsonObject.get("chartFilters"), context),
        ChartDataAdapter.deserializeList(jsonObject.get("showColumns"), context).map(_.toString),
        Option(jsonObject.get("startDate")) match {
          case Some(startDate) => Some(DateTime.parse(startDate.getAsString))
          case _ => None
        },
        Option(jsonObject.get("endDate")) match {
          case Some(endDate) => Some(DateTime.parse(endDate.getAsString))
          case _ => None
        },
        deserializegraphs(jsonObject.get("graphs"), context),
        jsonObject.get("editMode").getAsBoolean
      )
    }    
    
    def deserializegraphs(json: JsonElement, context: JsonDeserializationContext): List[PerformanceGraph] = {
      context.deserialize[java.util.List[PerformanceGraph]](json, new TypeToken[java.util.ArrayList[PerformanceGraph]]{}.getType).asScala.toList
    }

    def deserializeRowFilters(json: JsonElement, context: JsonDeserializationContext): List[PerformanceEntityFilter] = {
      context.deserialize[java.util.List[PerformanceEntityFilter]](json, new TypeToken[java.util.ArrayList[PerformanceEntityFilter]]{}.getType).asScala.toList
    }
    
    override def serialize(src: ChartMetaData, typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
      val json = new JsonObject()
      json.add("page", context.serialize(src.page))
      json.add("pageSize", context.serialize(src.pageSize))
      json.add("orderBy", context.serialize(src.orderBy))
      json.add("orderDir", context.serialize(src.orderDir))
      json.add("reloadUri", context.serialize(src.reloadUri))
      json.add("chartFilters", serializeRowFilters(src.chartFilters, context))
      json.add("showColumns", context.serialize(src.showColumns.asJava))
      
      if(!src.startDate.isEmpty) {
        json.add("startDate", context.serialize(src.startDate.get.toString("yyyy-MM-dd")))
      }
      
      if(!src.endDate.isEmpty) {
        json.add("endDate", context.serialize(src.endDate.get.toString("yyyy-MM-dd")))
      }
      
      json.add("graphs", serializegraphs(src.graphs, context))
      json.add("editMode", context.serialize(src.editMode))
      
      json
    }

    def serializegraphs(graphs: List[PerformanceGraph], context: JsonSerializationContext): JsonElement = {
      context.serialize(graphs.asJava)
    }

    def serializeRowFilters(rowFilters: List[PerformanceEntityFilter], context: JsonSerializationContext): JsonElement = {
      context.serialize(rowFilters.asJava)
    }    
  }
  
  def getMetaData(request: AuthenticatedRequest[AnyContent],
    defaultFields: List[PerformanceField],
    defaultFilters: List[PerformanceEntityFilter],
    defaultMetaData: ((UserAccount, List[PerformanceField], List[PerformanceEntityFilter], AuthenticatedRequest[AnyContent]) => ChartMetaData)): ChartMetaData = {
    val user = request.subject.get.asInstanceOf[UserAccount]
    request.getQueryString("metaData") match {
      case Some(metaDataJson) => 
        val metaData = gson.fromJson(metaDataJson, classOf[ChartMetaData])
        metaData.startDate = metaData.startDate match {
          case None => Some(SessionSettings.getSettings(user).chartStartDate)
          case _=> metaData.startDate
        }
        
        metaData.endDate = metaData.endDate match {
          case None => Some(SessionSettings.getSettings(user).chartEndDate)
          case _=> metaData.endDate
        }
        
        SessionSettings.setChartStartDate(user, metaData.startDate.get)
        SessionSettings.setChartEndDate(user, metaData.endDate.get)
        metaData
      case _ => 
        defaultMetaData(user, defaultFields, defaultFilters, request)
    }
  }
}
