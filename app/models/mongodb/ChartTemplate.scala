package models.mongodb

import util.charts.ChartMetaData
import Shared.Shared._
import org.bson.types.ObjectId
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON;
import org.joda.time.DateTime

case class ChartTemplate (
    templateName: String,
    userName: String,
    metaData: ChartMetaData
)

object ChartTemplate{
  def chartTemplateCollection = advancedCollection("chart_template");
  def toDbo(template: ChartTemplate): DBObject = DBObject(
    "templateName" -> template.templateName,
    "userName" -> template.userName,
    "metaData" -> JSON.parse(gson.toJson(template.metaData))
  )
  
  def fromDbo(dbo: DBObject): ChartTemplate = ChartTemplate(
    dbo.getAs[String]("templateName").get,
    dbo.getAs[String]("userName").get,
    gson.fromJson(dbo.getAs[DBObject]("metaData").get.toString, classOf[ChartMetaData])
  )
  
}