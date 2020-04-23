package models.mongodb

import Shared.Shared._
import be.objectify.deadbolt.scala.models.{Permission, Role, Subject}
import com.mongodb.casbah.Imports._
import models.mongodb.SecurityRole._
import org.bson.types.ObjectId
import play.api.Logger
import play.libs.Scala

import scala.collection.mutable.ListBuffer

case class UserAccount(
  _id: Option[ObjectId],
  var userName: String,
  var password: String,
  var email: String,
  var advertiserIds: List[Int],
  var securityRoles: Array[ObjectId]
) extends Subject {

  override def roles: List[Role] = {
    var userSecurityRoles: ListBuffer[SecurityRole] = new ListBuffer()
    this.securityRoles.foreach(
      roleId =>
        userSecurityRoles += dboToSecurityRole(
          SecurityRole.securityRoleCollection.findOne(DBObject("_id" -> roleId)).get.asDBObject
        )
    )
    userSecurityRoles.toList
  }

  override def permissions: List[Permission] = {
    var userSecurityRoles: ListBuffer[PermissionGroup] = new ListBuffer()
    this.securityRoles.foreach(
      roleId =>
        userSecurityRoles = userSecurityRoles ++ dboToSecurityRole(SecurityRole.securityRoleCollection
          .findOne(DBObject("_id" -> roleId)).get.asDBObject).permissions
    )
    userSecurityRoles.toList
  }

  override def identifier: String = userName
}

object UserAccount {
  def userAccountCollection = advancedCollection("user_account")

  def userAccountToDbo(userAccount: UserAccount): DBObject = {
    DBObject(
      "_id" -> userAccount._id.getOrElse(new ObjectId),
      "email" -> userAccount.email,
      "securityRoles" -> userAccount.securityRoles,
      "userName" -> userAccount.userName,
      "advertiserIds" -> userAccount.advertiserIds,
      "password" -> userAccount.password
    )
  }

  def dboToUserAccount(dbo: DBObject): UserAccount = {
    UserAccount(
      _id=dbo._id,
      email=dbo.getAs[String]("email").get,
      securityRoles=dbo.getAsOrElse[List[ObjectId]]("securityRoles", List()).toArray,
      userName=dbo.getAs[String]("userName").get,
      advertiserIds=dbo.getAsOrElse[List[Int]]("advertiserIds", List()),
      password=dbo.getAsOrElse[String]("password", "password")
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