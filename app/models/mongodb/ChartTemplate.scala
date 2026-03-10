package models.mongodb

import util.charts.ChartMetaData
import Shared.Shared._
import org.bson.types.ObjectId
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}

case class ChartTemplate (
    templateName: String,
    userName: String,
    metaData: ChartMetaData
)

object ChartTemplate {
  // Initialized by StartupTasks from MongoService
  var chartTemplateCollection: MongoCollection[Document] = _

  implicit val format: Format[ChartTemplate] = Json.format[ChartTemplate]

  def toDocument(template: ChartTemplate): Document = Document(
    "templateName" -> template.templateName,
    "userName" -> template.userName,
    "metaData" -> Document(Json.toJson(template.metaData).toString())
  )

  def fromDocument(doc: Document): ChartTemplate = ChartTemplate(
    doc.getString("templateName"),
    doc.getString("userName"),
    Json.parse(Option(doc.toBsonDocument.get("metaData")).map(v => Document(v.asDocument())).get.toJson()).as[ChartMetaData]
  )

}
