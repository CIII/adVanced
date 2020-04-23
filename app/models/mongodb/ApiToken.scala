package models.mongodb

import Shared.Shared._
import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import play.libs.Scala

case class ApiToken(
  _id: Option[ObjectId],
  var token: String,
  var active: Boolean
)

object ApiToken {
  def apiTokenCollection = advancedCollection("api_token")

  def apiTokenToDbo(apiToken: ApiToken): DBObject = {
    DBObject(
      "_id" -> apiToken._id.getOrElse(new ObjectId),
      "token" -> apiToken.token,
      "active" -> apiToken.active
    )
  }

  def dboToApiToken(dbo: DBObject): ApiToken = {
    ApiToken(
      _id=dbo._id,
      token=dbo.getAs[String]("token").get,
      active=dbo.getAs[Boolean]("active").get
    )
  }
}