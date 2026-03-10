package models.mongodb.msn

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import org.bson.types.ObjectId
import models.mongodb.MongoExtensions._

import scala.reflect.ClassTag

object Msn {
  // Initialized by StartupTasks from MongoService
  var msnCustomerCollection: MongoCollection[Document] = _
  var msnReportCollection: MongoCollection[Document] = _
  var msnAccountInfoCollection: MongoCollection[Document] = _
  var msnApiAccountCollection: MongoCollection[Document] = _
  var msnCampaignCollection: MongoCollection[Document] = _
  var msnAdGroupCollection: MongoCollection[Document] = _

  private val mapper = new com.fasterxml.jackson.databind.ObjectMapper()

  /**
   * Deserialize a stored MSN entity from a MongoDB document.
   * The data was originally stored using Jackson serialization, so we use
   * Jackson to deserialize it back to the typed entity.
   */
  def documentToMsnEntity[T: ClassTag](doc: Document, listKey: String, objectKey: Option[String]): T = {
    val json = objectKey match {
      case Some(key) =>
        val listDoc = Option(doc.toBsonDocument.get(listKey)).map(v => Document(v.asDocument())).get
        val objectDoc = Option(listDoc.toBsonDocument.get("object")).map(v => Document(v.asDocument())).get
        Option(objectDoc.toBsonDocument.get(key)).map(v => Document(v.asDocument())).get.toJson()
      case None =>
        val listDoc = Option(doc.toBsonDocument.get(listKey)).map(v => Document(v.asDocument())).get
        Option(listDoc.toBsonDocument.get("object")).map(v => Document(v.asDocument())).get.toJson()
    }
    mapper.readValue(json, implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]])
  }

  case class ApiAccount(
    _id: Option[ObjectId],
    name: String,
    userName: String,
    password: String,
    developerToken: String
  )

  def apiAccountToDocument(aa: ApiAccount) = Document(
    "_id" -> aa._id,
    "name" -> aa.name,
    "userName" -> aa.userName,
    "password" -> aa.password,
    "developerToken" -> aa.developerToken
  )

  def documentToApiAccount(doc: Document) = ApiAccount(
    _id = Option(doc.getObjectId("_id")),
    name = Option(doc.getString("name")).getOrElse(""),
    userName = Option(doc.getString("userName")).getOrElse(""),
    password = Option(doc.getString("password")).getOrElse(""),
    developerToken = Option(doc.getString("developerToken")).getOrElse("")
  )
}
