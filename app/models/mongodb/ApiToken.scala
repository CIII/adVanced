package models.mongodb

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import org.bson.types.ObjectId
import models.mongodb.MongoExtensions._

case class ApiToken(
  _id: Option[ObjectId],
  token: String,
  active: Boolean
)

object ApiToken {

  def apiTokenToDocument(apiToken: ApiToken): Document = {
    Document(
      "_id" -> apiToken._id.getOrElse(new ObjectId),
      "token" -> apiToken.token,
      "active" -> apiToken.active
    )
  }

  def documentToApiToken(doc: Document): ApiToken = {
    ApiToken(
      _id = Option(doc.getObjectId("_id")),
      token = doc.getString("token"),
      active = doc.getBoolean("active")
    )
  }
}
