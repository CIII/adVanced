package models.mongodb.performance

import models.mongodb.performance.PerformanceField.PerformanceFieldType._
import models.mongodb.performance.HtmlPerformanceField._
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON

class HtmlPerformanceField(
  fieldName: String,
  builder: HtmlFieldBuilder
  )extends PerformanceField(fieldName, dimension){
  
  override def dependantFields(): List[PerformanceField] = builder.getRequiredFields()
  override def projectionQueryObject(): DBObject = {
    DBObject(fieldName -> (JSON.parse("{" + builder.build + "}").asInstanceOf[DBObject]))
  }
}

object HtmlPerformanceField{
  
  trait HtmlFieldSegment {
    def getStringValue(): String
    def getRequiredFields(): List[PerformanceField]
  }
  
  class DimensionFieldSegment(field: PerformanceField) extends HtmlFieldSegment{
    override def getStringValue(): String = "$_id." + field.fieldName
    override def getRequiredFields(): List[PerformanceField] = List(field)
  }
  
  /**
   * Fields which are stored in mongodb as numbers must first be converted
   * to a string before they can be concatenated with other strings.  This is
   * done with the substr aggregation method.
   */
  class MeasureFieldSegment(field: PerformanceField) extends HtmlFieldSegment{
    override def getStringValue(): String = {
      "{ $substr: [$" + field.fieldName + ",0,-1]}"
    }
    override def getRequiredFields(): List[PerformanceField] = List(field)
  }
  
  class HtmlSegment(html: String) extends HtmlFieldSegment{
    override def getStringValue(): String = html
    override def getRequiredFields(): List[PerformanceField] = List()
  }
  
  class HtmlFieldBuilder(segments: HtmlFieldSegment*){
    def build(): String = {
      "$concat: [\"" + segments.map { segment => segment.getStringValue() }
        .mkString("\",\"") + "\"]"
    }
    
    def getRequiredFields(): List[PerformanceField] = {
      segments.map { segment => segment.getRequiredFields() }.flatten.distinct.toList
    }
  }
  
  def clickthroughBuilder(visibleField: PerformanceField,
      idField: PerformanceField,
      clickthroughRoute: String): HtmlFieldBuilder = {
    new HtmlFieldBuilder(
      new HtmlSegment("<div>"),
        new HtmlSegment("""<a href=\"""" + clickthroughRoute + """?filterById="""),
        new DimensionFieldSegment(idField),
        new HtmlSegment("""\">"""),
        new DimensionFieldSegment(visibleField),
        new HtmlSegment("</a>"),
      new HtmlSegment("</div>")
    )
  }
  
  def clickthroughAndEditBuilder(visibleField: PerformanceField, 
      clickthroughRoute: String, editRoute: String): HtmlFieldBuilder = {
    new HtmlFieldBuilder(
      new HtmlSegment("<div>"),
        new HtmlSegment("""<a href=\"""" + clickthroughRoute + """?filterById="""),
        new DimensionFieldSegment(PerformanceEntity.idField),
        new HtmlSegment("""\">"""),
        new DimensionFieldSegment(visibleField),
        new HtmlSegment("</a>"),
        new HtmlSegment("""<span class=\"pull-right\">"""),
          new HtmlSegment("""<a href=\"""" + editRoute),
          new DimensionFieldSegment(PerformanceEntity.idField),
          new HtmlSegment("""/\"><i class=\"fa fa-edit\"></i></a>"""),
        new HtmlSegment("</span>"),
      new HtmlSegment("</div>")
    )
  }
  
  def editBuilder(visibleField: PerformanceField, editRoute: String): HtmlFieldBuilder = {
    new HtmlFieldBuilder(
      new HtmlSegment("<div>"),
        new DimensionFieldSegment(visibleField),
        new HtmlSegment("""<span class=\"pull-right\">"""),
          new HtmlSegment("""<a href=\"""" + editRoute + """/"""),
          new DimensionFieldSegment(PerformanceEntity.idField),
          new HtmlSegment("""/\"><i class=\"fa fa-edit\"></i></a>"""),
        new HtmlSegment("</span>"),
      new HtmlSegment("</div>")
    )
  }
}