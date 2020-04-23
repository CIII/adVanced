package util.charts.client

import util.charts.client.ChartColumn
import util.charts.ChartData
import util.charts.ChartMetaData
import util.charts.performance.PerformanceGraph

/**
 * Charts for client side only
 */
trait Chart {
  def getMetaData(): ChartMetaData
  def getData(): ChartData
  def getColumns(): List[ChartColumn]
  def getVisibleColumns(): List[ChartColumn]
  def getTotalRecords(): Int
  def getGraphicColumns(graphic: PerformanceGraph): List[ChartColumn]
  def getGraphicData(graphic: PerformanceGraph): ChartData
}
