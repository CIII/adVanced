package util.charts

import org.joda.time.DateTime
import util.charts.client.ChartColumn
import com.mongodb.casbah.MongoCollection
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
  def getDistinctValuesForColumn(collection: MongoCollection, column: PerformanceChartColumn): List[Any] = {
    val cacheKey = s"${collection.name}::${column.filterField.fieldName}"
    chartDistinctValuesMap.get(cacheKey) match {
      case Some(values)  => values
      case _ =>
        val values = collection.distinct(column.filterField.fieldName).toList
        chartDistinctValuesMap = chartDistinctValuesMap + (cacheKey -> values)
        values   
    }
  }
  
  def clearCache = {
    chartDistinctValuesMap = Map()
  }
  
}