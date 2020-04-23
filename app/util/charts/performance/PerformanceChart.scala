package util.charts.performance

import models.mongodb.performance.PerformanceEntity
import models.mongodb.performance.PerformanceEntityLoader
import models.mongodb.performance.PerformanceEntityFilter
import models.mongodb.performance.PerformanceField
import models.mongodb.performance.PerformanceField.PerformanceFieldType._
import util.charts.ChartMetaData
import util.charts.ChartData
import util.charts.ChartCache
import com.mongodb.casbah.MongoCollection
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.io.File
import java.io.PrintWriter
import Shared.Shared._
import play.libs.Json

class PerformanceChart[T <: PerformanceEntity](
  val columns: List[PerformanceChartColumn],         
  val metaData: ChartMetaData,         
  entityFactory: () => T,         
  collection: MongoCollection
){
  
  /**
   * Which field should be used as the date field.  The match stage will use this field
   * name in order to filter the entities.  Override this if you don't want it to be "date"
   */
  val dateField: PerformanceField = PerformanceEntity.dateField
  val dtf: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
  
  /**
   * List of visible columns.  Filter on column.isVisible method, passing in the metaData for this
   * chart.  To make columns mandatory / optional override the isVisible method and handle based on the 
   * meta data.
   */
  lazy val visibleColumns: List[PerformanceChartColumn] = {
    columns.filter { 
      column => column.isVisible(metaData)
    }
  }
  
  lazy val dateFilters: List[PerformanceEntityFilter] = List(
    PerformanceEntityFilter(dateField, "gte", List(metaData.startDate.get.toString(dtf))),
    PerformanceEntityFilter(dateField, "lte", List(metaData.endDate.get.toString(dtf)))
  )
  
  lazy val entityLoader = new PerformanceEntityLoader[T](
    includeRequiredFields(visibleColumns.map { column => column.field }),
    collection,
    entityFactory
  )
      
  lazy val entities: List[T] = {
    entityLoader.withMatchStage(
      dateFilters ++ metaData.chartFilters.filter { filter => filter.field.fieldType == dimension }
    ).withGroupSumStage()
    .withProject()
    .withMatchStage(metaData.chartFilters.filter { filter => filter.field.fieldType == measure })
    .withSortStage(-1)
    .withPaginationStages(metaData.page, metaData.pageSize)
    .execute
  }
  
  def getData(): ChartData = {
    ChartData(entities.map { 
      entity => entity.toList(visibleColumns.map { 
        col => col.field 
      }) 
    })
  }
  
  def getColumnFilterOptions(column: PerformanceChartColumn): List[Any] = {
    ChartCache.getDistinctValuesForColumn(collection, column)
  }
  
  def includeRequiredFields(fields: List[PerformanceField]): List[PerformanceField] = { 
    fields.map { field => field.dependantFields }.flatten.distinct ++ fields
  }
  
  lazy val totalRecords: Int = {
    val visibleFields = visibleColumns.map { column => column.field }
    val totalRecordsLoader = new PerformanceEntityLoader[T](
      visibleFields,
      collection,
      entityFactory
    )
    
    totalRecordsLoader.withMatchStage(
      dateFilters ++ metaData.chartFilters.filter { filter => filter.field.fieldType == dimension }
    ).withGroupSumStage(includeRequiredFields(visibleFields)    
    ).withMatchStage(metaData.chartFilters.filter { filter => filter.field.fieldType == measure })
    .withCountStage
    .executeCount
  }
  
  def getGraphEntities(graph: PerformanceGraph): List[T] = {
    val graphCols = getGraphColumns(graph)
    val graphFields = includeRequiredFields(graphCols.map { col => col.field })
    val graphDataLoader = new PerformanceEntityLoader[T](
      graphFields,
      collection,
      entityFactory
    )
    
    graphDataLoader.withMatchStage(
      dateFilters ++ metaData.chartFilters.filter { filter => filter.field.fieldType == dimension }
    ).withGroupSumStage()
    .withProject()
    .withMatchStage(metaData.chartFilters.filter { filter => filter.field.fieldType == measure })
    .withSortStage(1)
    .execute
  }

  def getGraphData(graph: PerformanceGraph): ChartData = {
    ChartData(getGraphEntities(graph).map { 
      entity => entity.toList(
        getGraphColumns(graph).map { col => col.field }
      ) 
    })
  }
  
  def getGraphColumns(graph: PerformanceGraph): List[PerformanceChartColumn] = {
    columns.filter { column => graph.fieldNames.contains(column.field.fieldName) }
  }
  
  def exportCsv(filename: String): File = {
    val file = new File(filename)
    using(new PrintWriter(file)){
      printWriter =>
        printWriter.write(visibleColumns.map { col => col.header }.mkString(",").concat("\n"))
        getData.data.foreach { 
          row => 
            val formattedRow = row.map { value => Json.toJson(value) }
            printWriter.write(formattedRow.mkString(",").concat("\n"))
        }
    }
    
    file
  }
}