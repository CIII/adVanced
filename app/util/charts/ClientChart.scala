package util.charts

import util.charts._
import util.charts.client._
import util.charts.client.ChartColumn.ColumnType._
import scala.collection.mutable.ListBuffer
import util.charts.performance.PerformanceGraph

class ClientChart(
  columns: List[ChartColumn], 
  items: List[AnyRef]
) extends GenericChart(columns, ChartMetaData(1, 1, "", 1, "", List(), columns.map { column => column.field }, None, None, List(), false)){
  val data: List[ChartItem] = items.map { item => new ChartItem(item) }
  
  override def getData(): ChartData = {
    ChartData(data.map { 
      chartItem => {
        val row = chartItem.getRow(columns.filter(
          column => !column.isDynamic
        ).map {
          column => column.field 
        })
        
        row ++ getDynamicFields(columns.filter(
          column => !column.isDynamic
        ).map {
          column => column.field 
        }.zip(row))
      }  
    })
  }
  
  override def getTotalRecords: Int = {
    data.size
  }

  override def getGraphicData(graphic: PerformanceGraph): ChartData = {
    throw new Exception("Graphics not implemented for client charts")
  }
}