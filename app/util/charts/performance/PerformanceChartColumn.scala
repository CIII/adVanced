package util.charts.performance

import models.mongodb.performance.PerformanceField
import util.charts.ChartMetaData

class PerformanceChartColumn(
  val field: PerformanceField,
  val header: String
){
  var numberFormatPrefix: String = ""
  var numberFormatSuffix: String = ""
  var groupingSymbol: String = ","
  var decimalPlaces: Int = 0
  
  /**
   * Set a prefix to be displayed at the start of a number.  ie. $. 
   * Will only be applied to columns of type measure.  Default is empty
   */
  def withNumberFormatPrefix(prefix: String): PerformanceChartColumn = {
    numberFormatPrefix = prefix
    this
  }
  
  /**
   * Set a suffix to be displayed at the end of a number.  ie %.  Will
   * only be applied to columns of type measure.  Default is empty
   */
  def withNumberFormatSuffix(suffix: String): PerformanceChartColumn = {
    numberFormatSuffix = suffix
    this
  }
  
  /**
   * Set the number of decimal places to show in the chart.  Will only
   * be applied to columns of type measure.  Default is 0
   */
  def withDecimalPlaces(places: Int): PerformanceChartColumn = {
    decimalPlaces = places
    this
  }
  
  /**
   * Option to specify a grouping symbol (ie ",") which will be used
   * as the thousands separator. Default is a comma
   */
  def withGroupingSymbol(symbol: String): PerformanceChartColumn = {
    groupingSymbol = symbol
    this
  }
  
  def isDimension: Boolean = {
    field.fieldType == PerformanceField.PerformanceFieldType.dimension
  }
  
  def isMeasure: Boolean = {
    field.fieldType == PerformanceField.PerformanceFieldType.measure
  }
  
  /**
   * Option to specify whether the column should be visible or not.  Accepts the metaData
   * to allow visibility based on situation.  Override to add functionality.
   */
  def isVisible(metaData: ChartMetaData): Boolean = {
    metaData.showColumns.contains(field.fieldName)
  }
  
  /**
   * Option to specify whether or not this field can be filtered on.  Usually true but in some
   * cases like budget or max cpc we can't filter because they're foreign fields.
   */
  def isFilterable: Boolean = true
  
  /**
   * When getting the options to filter a column on, the filter field will be used.  In most
   * cases this is the same field, but in cases where html is displayed than you'll want to 
   * specify a different field by overriding this method.
   */
  def filterField: PerformanceField = field
}