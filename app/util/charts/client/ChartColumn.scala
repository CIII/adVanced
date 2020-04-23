package util.charts.client

import com.mongodb.casbah.Imports._
import util.charts.client.ChartColumn.ColumnDataType.ColumnDataType
import util.charts.client.ChartColumn.ColumnType.ColumnType
import util.charts.client.ChartColumn.ColumnType

/**
 * Encapsulate a column in the chart.  Left as a class instead of case class
 * for extensibility.
 */
class ChartColumn (
  val field: String,
  val fieldDBName: String,
  val header: String,
  val dataType: ColumnDataType,
  val colType: ColumnType
){
  var visible = false
  var filterOptions: List[Any] = List()
  var numberFormatPrefix: String = ""
  var numberFormatSuffix: String = ""
  var decimalPlaces: Int = 2
  var isMandatory = false
  var editModeOnly = false
  
  def isDimension: Boolean = {
    colType == ColumnType.dimension
  }
  
  def isMeasure: Boolean = {
    colType == ColumnType.measure
  }
  
  def isDate: Boolean = {
    colType == ColumnType.date
  }
  
  def isDynamic: Boolean = {
    colType == ColumnType.dynamic
  }
  
  /**
   * Set a prefix to be displayed at the start of a number.  ie. $. 
   * Will only be applied to columns of type measure.
   */
  def withNumberFormatPrefix(prefix: String): ChartColumn = {
    numberFormatPrefix = prefix
    this
  }
  
  /**
   * Set a suffix to be displayed at the end of a number.  ie %.  Will
   * only be applied to columns of type measure
   */
  def withNumberFormatSuffix(suffix: String): ChartColumn = {
    numberFormatSuffix = suffix
    this
  }
  
  /**
   * Set the number of decimal places to show in the chart.  Will only
   * be applied to columns of type measure
   */
  def withDecimalPlaces(places: Int): ChartColumn = {
    decimalPlaces = places
    this
  }
  
  /**
   * Set this column as mandatory (or not).  A mandatory column
   * will always be displayed on the chart.
   */
  def withMandatory(mandatory: Boolean): ChartColumn = {
    isMandatory = mandatory
    this
  }
}

object ChartColumn {
  
  trait OperationalColumn extends DependencyColumn{
    def getQueryObject(): DBObject
  }
  
  trait DynamicColumn extends DependencyColumn{
    def getValueForRow(rowValues: List[(String, Any)]): Any
  } 
  
  trait DependencyColumn {
    def getRequiredFields(): List[String]
  }
  
  object ColumnType extends Enumeration {
    type ColumnType = Value
    val dimension, measure, dynamic, date = Value
  }
  
  object ColumnDataType extends Enumeration {
    type ColumnDataType = Value
    val string, number = Value
  }
  
  def hasDependency(col: ChartColumn): Boolean = {
    col.isInstanceOf[DependencyColumn]
  }
  
  def isOperational(col: ChartColumn): Boolean = {
    col.isInstanceOf[OperationalColumn]
  }
  
  def isDynamic(col: ChartColumn): Boolean = {
    col.isInstanceOf[DynamicColumn]
  }
}