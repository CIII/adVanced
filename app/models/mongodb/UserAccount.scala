package models.mongodb

import be.objectify.deadbolt.scala.models.{Permission, Role, Subject}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import org.mongodb.scala.model.Filters._
import models.mongodb.SecurityRole._
import models.mongodb.MongoExtensions._
import org.bson.types.ObjectId
import play.api.Logger

import scala.jdk.CollectionConverters._
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._

case class UserAccount(
  _id: Option[ObjectId],
  userName: String,
  password: String,
  email: String,
  advertiserIds: List[Int],
  securityRoles: Array[ObjectId]
) extends Subject {

  // TODO: Replace blocking calls with async Future composition
  override def roles: List[Role] = {
    var userSecurityRoles: ListBuffer[SecurityRole] = new ListBuffer()
    this.securityRoles.foreach(
      roleId => {
        val result = Await.result(
          SecurityRole.securityRoleCollection.find(equal("_id", roleId)).first().toFuture(),
          10.seconds
        )
        userSecurityRoles += documentToSecurityRole(result)
      }
    )
    userSecurityRoles.toList
  }

  // TODO: Replace blocking calls with async Future composition
  override def permissions: List[Permission] = {
    var userSecurityRoles: ListBuffer[PermissionGroup] = new ListBuffer()
    this.securityRoles.foreach(
      roleId => {
        val result = Await.result(
          SecurityRole.securityRoleCollection.find(equal("_id", roleId)).first().toFuture(),
          10.seconds
        )
        userSecurityRoles = userSecurityRoles ++ documentToSecurityRole(result).permissions
      }
    )
    userSecurityRoles.toList
  }

  override def identifier: String = userName
}

object UserAccount {
  // NOTE: securityRoleCollection and userAccountCollection must be provided
  // via dependency injection (MongoService). These are placeholder references
  // that will need to be wired up by the caller or converted to use DI.
  var securityRoleCollection: MongoCollection[Document] = _
  var userAccountCollection: MongoCollection[Document] = _

  def userAccountToDocument(userAccount: UserAccount): Document = {
    Document(
      "_id" -> userAccount._id.getOrElse(new ObjectId),
      "email" -> userAccount.email,
      "securityRoles" -> userAccount.securityRoles.toList,
      "userName" -> userAccount.userName,
      "advertiserIds" -> userAccount.advertiserIds,
      "password" -> userAccount.password
    )
  }

  def documentToUserAccount(doc: Document): UserAccount = {
    UserAccount(
      _id = Option(doc.getObjectId("_id")),
      email = doc.getString("email"),
      securityRoles = Option(doc.getList("securityRoles", classOf[ObjectId]))
        .map(_.asScala.toList).getOrElse(List()).toArray,
      userName = doc.getString("userName"),
      advertiserIds = Option(doc.getList("advertiserIds", classOf[Integer]))
        .map(_.asScala.toList.map(_.intValue())).getOrElse(List()),
      password = Option(doc.getString("password")).getOrElse("password")
    )
  }

  def isAdministrator(userAccount: UserAccount): Boolean = {
    this.hasPermission(userAccount, PermissionGroup.Administrator)
  }

  def isAdvertiser(userAccount: UserAccount): Boolean = {
    this.hasPermission(userAccount, PermissionGroup.Advertiser)
  }

  /**
   * Check if this user account contains the specified permission.  Iterates over security roles,
   * and checks each for the permission.
   */
  def hasPermission(userAccount: UserAccount, permission: PermissionGroup): Boolean = {
     userAccount.roles.foreach(
      role =>
        if(SecurityRole.hasPermission(role.asInstanceOf[SecurityRole], permission)){
          return true;
        }
    )

    return false;
  }
}
