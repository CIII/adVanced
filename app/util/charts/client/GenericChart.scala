package util.charts.client

import util.charts.ChartMetaData
import util.charts.performance.PerformanceGraph
import util.charts.client.ChartColumn.DynamicColumn

abstract class GenericChart(
  columns: List[ChartColumn],
  metaData: ChartMetaData
) extends Chart {
  updateColumns
  
  override def getMetaData(): ChartMetaData = {
    metaData
  }

  override def getColumns(): List[ChartColumn] = {
    columns
  }
  
  override def getVisibleColumns(): List[ChartColumn] = {
    columns.filter { column => column.visible }
  }
  
  override def getGraphicColumns(graphic: PerformanceGraph): List[ChartColumn] = {
    graphic.fieldNames.map { field => getColumnForField(field) }
  }
  
  def updateColumns = {
    for(column <- columns){
      if(column.isMandatory){
        // mandatory columns are always visible
        column.visible = true
      } else if(metaData.editMode && (column.isDimension || column.isDate)){
        // In edit mode, we only display mandatory columns and measures.
        column.visible = false
      } else {
        column.visible = metaData.showColumns.contains(column.field)
      }
    }
  }
  
  def getDynamicFields(row: List[(String, Any)]): List[Any] = {
    columns.filter { column => column.isDynamic && column.visible }.map { 
      column => column.asInstanceOf[DynamicColumn].getValueForRow(row) 
    }
  }
  
  def getColumnForField(field: String): ChartColumn = {
    for(column <- getColumns){
      if(column.field.equals(field)){
        return column
      }
    }
    
    throw new Exception(s"Invalid field - No column found: $field")
  }
  
  def getColumnsForFields(fields: List[String]): List[ChartColumn] = {
    fields.map { field => getColumnForField(field) }
  }
}