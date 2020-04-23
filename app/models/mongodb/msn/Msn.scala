package models.mongodb.msn

import Shared.Shared._
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON

import scala.reflect.{ClassTag, classTag}

object Msn {
  def msnApiAccountCollection = advancedCollection("msn_api_account")
  def msnCustomerCollection = advancedCollection("msn_customer")
  def msnAccountInfoCollection = advancedCollection("msn_account")
  def msnCampaignCollection = advancedCollection("msn_campaign")
  def msnAdGroupCollection = advancedCollection("msn_adgroup")
  def msnSitePlacementCollection = advancedCollection("msn_site_placement")
  def msnReportCollection(
    reportType: MsnReportType.Value
  ) = advancedCollection(
    "Msn%s".format(reportType)
  )

  def dboToMsnEntity[T: ClassTag](dbo: DBObject, listKey: String, objectKey: Option[String]): T = {
    gson.fromJson(
      JSON.serialize(
        objectKey match {
          case Some(key) =>
            dbo.as[MongoDBList](listKey).asInstanceOf[DBObject].as[DBObject]("object").expand[DBObject](key)
          case None =>
            dbo.as[MongoDBList](listKey).asInstanceOf[DBObject].as[DBObject]("object")
        }
      ),
      classTag[T].runtimeClass.asInstanceOf[Class[T]]
    )
  }

  case class ApiAccount(
    _id: Option[ObjectId],
    name: String,
    userName: String,
    password: String,
    developerToken: String
  )

  def apiAccountToDbo(aa: ApiAccount) = DBObject(
    "_id" -> aa._id,
    "name" -> aa.name,
    "userName" -> aa.userName,
    "password" -> aa.password,
    "developerToken" -> aa.developerToken
  )


  def dboToApiAccount(dbo: DBObject) = ApiAccount(
    _id=dbo._id,
    name=dbo.getAsOrElse[String]("name", ""),
    userName=dbo.getAsOrElse[String]("userName", ""),
    password=dbo.getAsOrElse[String]("password", ""),
    developerToken=dbo.getAsOrElse[String]("developerToken", "")
  )
}