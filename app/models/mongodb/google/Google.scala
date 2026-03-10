package models.mongodb.google

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import org.bson.types.ObjectId
import play.api.libs.json.{JsValue, Json}
import models.mongodb.MongoExtensions._

import scala.reflect.{ClassTag, classTag}

object Google {

  // Initialized by StartupTasks from MongoService
  var googleMccCollection: MongoCollection[Document] = _
  var googleCustomerCollection: MongoCollection[Document] = _
  var googleCampaignCollection: MongoCollection[Document] = _
  var googleAdGroupCollection: MongoCollection[Document] = _
  var googleAdCollection: MongoCollection[Document] = _
  var googleCriterionCollection: MongoCollection[Document] = _
  var googleBudgetCollection: MongoCollection[Document] = _
  var googleBiddingStrategyCollection: MongoCollection[Document] = _

  // Report collections are dynamically named by report type
  private var _mongoService: Option[services.MongoService] = None
  def setMongoService(ms: services.MongoService): Unit = _mongoService = Some(ms)
  def googleReportCollection(reportType: String): MongoCollection[Document] = {
    _mongoService.map(_.collection(s"google_${reportType.toLowerCase}")).getOrElse(
      throw new RuntimeException("MongoService not initialized")
    )
  }

  /**
   * Deserialize a stored Google entity Document back to its original type.
   *
   * TODO: Replace Gson-based deserialization with Play JSON or direct Document field access
   * when the Google Ads API v18 migration is complete.
   */
  def documentToEntity[T: ClassTag](doc: Document, listKey: String, objectKey: Option[String]): T = {
    val innerDoc = objectKey match {
      case Some(key) =>
        val listDoc = Option(doc.toBsonDocument.get(listKey)).map(v => Document(v.asDocument())).get
        val objectDoc = Option(listDoc.toBsonDocument.get("object")).map(v => Document(v.asDocument())).get
        Option(objectDoc.toBsonDocument.get(key)).map(v => Document(v.asDocument())).get
      case None =>
        val listDoc = Option(doc.toBsonDocument.get(listKey)).map(v => Document(v.asDocument())).get
        Option(listDoc.toBsonDocument.get("object")).map(v => Document(v.asDocument())).get
    }
    // TODO: Replace with Play JSON deserialization
    val json = Json.parse(innerDoc.toJson())
    json.as[T](implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]] match {
      case _ => throw new RuntimeException(s"TODO: Implement Play JSON Reads for ${classTag[T].runtimeClass.getName}")
    })
  }

  def mccToDocument(mcc: Mcc): Document = {
    Document(
      "_id" -> mcc._id,
      "developerToken" -> mcc.developerToken,
      "name" -> mcc.name,
      "oAuthClientId" -> mcc.oAuthClientId,
      "oAuthClientSecret" -> mcc.oAuthClientSecret,
      "oAuthRefreshToken" -> mcc.oAuthRefreshToken
    )
  }

  def documentToMcc(doc: Document): Mcc = {
    Mcc(
      _id = Option(doc.getObjectId("_id")),
      name = doc.getString("name"),
      developerToken = doc.getString("developerToken"),
      oAuthClientId = doc.getString("oAuthClientId"),
      oAuthClientSecret = doc.getString("oAuthClientSecret"),
      oAuthRefreshToken = doc.getString("oAuthRefreshToken")
    )
  }

  case class Mcc(
    _id: Option[ObjectId],
    name: String,
    developerToken: String,
    oAuthClientId: String,
    oAuthClientSecret: String,
    oAuthRefreshToken: String
  )
}
