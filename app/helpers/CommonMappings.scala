package helpers

import play.api.data.Mapping
import play.api.data.Forms
import play.api.data.Forms._
import org.bson.types.ObjectId
import org.joda.time.DateTime
import java.util.Date

trait CommonMappings {
  
  def objectIdMapping: Mapping[ObjectId] = (
    Forms.mapping(
      "id" -> text
    )
    ((idStr: String) => new ObjectId(idStr))
    ((objectId: ObjectId) => Some(objectId.toString))
  )
  
  def dateTimeMapping: Mapping[DateTime] = (
    mapping(
      "date" -> date
    )
    ((dateTimeStr: Date) => new DateTime(dateTimeStr))
    ((dateTime: DateTime) => Some(dateTime.toDate))
  )
  
  def commaSeparatedListMapping: Mapping[List[String]] = (
    mapping(
      "csStr" -> nonEmptyText
    )
    ((csStr: String) => csStr.split(",").toList )                  
    ((strList : List[String]) => Some(strList.mkString(",")))
  ) 
  
}