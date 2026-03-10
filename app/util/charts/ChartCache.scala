package util.charts

import org.joda.time.DateTime
import util.charts.client.ChartColumn
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import play.api.Logger
import util.charts.performance.PerformanceChartColumn

object ChartCache {
  
  /**
   * Cache the "distinct values" for a collection/column.  This is used for loading the chart dimension filters,
   * where we need to show the distinct values to the user so that they may filter on a value or set of values. 
   * Instead of loading this on every chart page load, cache it here.
   * 
   * The cache is cleared whenever the GoogleReportActor is finished running. 
   */
  var chartDistinctValuesMap: Map[String, List[Any]] = Map()
  
  /**
   * Get a list of distinct values for a column from the cache, if it exists.  If not, load it from the
   * db and store it in the cache.  Include a timestamp so that we throw out values that aren't from today.
   */
  def getDistinctValuesForColumn(collection: MongoCollection[Document], column: PerformanceChartColumn): List[Any] = {
    val cacheKey = s"${collection.namespace.getCollectionName}::${column.filterField.fieldName}"
    chartDistinctValuesMap.get(cacheKey) match {
      case Some(values)  => values
      case _ =>
        val values: List[Any] = collection.distinctSync(column.filterField.fieldName)
        chartDistinctValuesMap = chartDistinctValuesMap + (cacheKey -> values)
        values   
    }
  }
  
  def clearCache = {
    chartDistinctValuesMap = Map()
  }
  
}