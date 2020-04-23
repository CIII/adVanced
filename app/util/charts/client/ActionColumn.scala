package util.charts.client

import util.charts.client.ChartColumn
import util.charts.client.ChartColumn.ColumnDataType._
import util.charts.client.ChartColumn.ColumnType._
import util.charts.client.ChartColumn.DynamicColumn

class ActionColumn(
  getEditUrl: (List[Any]) => Any
) extends ChartColumn ("action", "", "Actions", string, dynamic) 
  with DynamicColumn {
  
  override def getValueForRow(rowValues: List[(String, Any)]): Any = {
    """<a class="btn btn-primary btn-xs" href="""" + 
      getEditUrl(rowValues.map(pair => pair._2)) + 
      """">Edit</a>"""
  }

  def getRequiredFields(): List[String] = {
    List()
  }
}