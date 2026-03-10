package util.charts

import util.charts.performance.PerformanceGraph
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._
import play.api.libs.functional.syntax._
import Shared.Shared._
import org.joda.time.DateTime
import models.mongodb.performance.PerformanceEntityFilter
import be.objectify.deadbolt.scala.AuthenticatedRequest
import models.mongodb.UserAccount
import play.api.mvc.AnyContent
import models.mongodb.SessionSettings
import models.mongodb.performance.PerformanceField

case class ChartMetaData(
   page: Int,                                 // which page to display
   pageSize: Int,                             // number of records to display on the page
   orderBy: String,                           // field name of column to order by
   orderDir: Int,                             // 1 = ASC, -1 = DESC
   reloadUri: String,                         // uri passed to the UI that specifies which route to call on page reload
   chartFilters: List[PerformanceEntityFilter],          // selected fields in filter
   showColumns: List[String],                 // list of column field names specifying which should be displayed.  Any others will be hidden
   startDate: Option[DateTime],               // for date based tables, start date of records to show. Specify None to retrieve all records
   endDate: Option[DateTime],                 // for date based tables, end date of records to show.  Specify None to retrieve all recods
   graphs: List[PerformanceGraph],            // list of graphs currently displayed on the page
   editMode: Boolean                          // true/false if we are in edit mode.  edit mode forces constraints on the data.
)


object ChartMetaData {

  implicit val chartMetaDataFormat: Format[ChartMetaData] = new Format[ChartMetaData] {
    override def reads(json: JsValue): JsResult[ChartMetaData] = {
      for {
        page <- (json \ "page").validate[Int]
        pageSize <- (json \ "pageSize").validate[Int]
        orderBy <- (json \ "orderBy").validate[String]
        orderDir <- (json \ "orderDir").validate[Int]
        reloadUri <- (json \ "reloadUri").validate[String]
        chartFilters <- (json \ "chartFilters").validate[List[PerformanceEntityFilter]](Reads.list(PerformanceEntityFilter.performanceEntityFilterFormat))
        showColumns <- (json \ "showColumns").validate[List[String]]
        startDate <- (json \ "startDate").validateOpt[String].map(_.map(DateTime.parse))
        endDate <- (json \ "endDate").validateOpt[String].map(_.map(DateTime.parse))
        graphs <- (json \ "graphs").validate[List[PerformanceGraph]](Reads.list(PerformanceGraph.performanceGraphFormat))
        editMode <- (json \ "editMode").validate[Boolean]
      } yield ChartMetaData(page, pageSize, orderBy, orderDir, reloadUri, chartFilters, showColumns, startDate, endDate, graphs, editMode)
    }

    override def writes(src: ChartMetaData): JsValue = {
      var obj = Json.obj(
        "page" -> src.page,
        "pageSize" -> src.pageSize,
        "orderBy" -> src.orderBy,
        "orderDir" -> src.orderDir,
        "reloadUri" -> src.reloadUri,
        "chartFilters" -> Json.toJson(src.chartFilters)(Writes.list(PerformanceEntityFilter.performanceEntityFilterFormat)),
        "showColumns" -> src.showColumns,
        "graphs" -> Json.toJson(src.graphs)(Writes.list(PerformanceGraph.performanceGraphFormat)),
        "editMode" -> src.editMode
      )

      src.startDate.foreach { sd =>
        obj = obj + ("startDate" -> JsString(sd.toString("yyyy-MM-dd")))
      }

      src.endDate.foreach { ed =>
        obj = obj + ("endDate" -> JsString(ed.toString("yyyy-MM-dd")))
      }

      obj
    }
  }

  def getMetaData(request: AuthenticatedRequest[AnyContent],
    defaultFields: List[PerformanceField],
    defaultFilters: List[PerformanceEntityFilter],
    defaultMetaData: ((UserAccount, List[PerformanceField], List[PerformanceEntityFilter], AuthenticatedRequest[AnyContent]) => ChartMetaData)): ChartMetaData = {
    val user = request.subject.get.asInstanceOf[UserAccount]
    request.getQueryString("metaData") match {
      case Some(metaDataJson) =>
        val parsedMetaData = Json.parse(metaDataJson).as[ChartMetaData]
        val metaData = parsedMetaData.copy(
          startDate = parsedMetaData.startDate match {
            case None => Some(SessionSettings.getSettings(user).chartStartDate)
            case _ => parsedMetaData.startDate
          },
          endDate = parsedMetaData.endDate match {
            case None => Some(SessionSettings.getSettings(user).chartEndDate)
            case _ => parsedMetaData.endDate
          }
        )

        SessionSettings.setChartStartDate(user, metaData.startDate.get)
        SessionSettings.setChartEndDate(user, metaData.endDate.get)
        metaData
      case _ =>
        defaultMetaData(user, defaultFields, defaultFilters, request)
    }
  }
}
