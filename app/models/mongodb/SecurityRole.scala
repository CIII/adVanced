package models.mongodb

import Shared.Shared._
import be.objectify.deadbolt.scala.models.{Permission, Role}
import com.mongodb.casbah.Imports._
import enumeratum._
import org.bson.types.ObjectId

sealed trait PermissionGroup extends EnumEntry with Permission {
  def value: String = {
    this.toString
  }
}

object PermissionGroup extends Enum[PermissionGroup] with PlayJsonEnum[PermissionGroup] {

  val values = findValues
  
  case object Administrator extends PermissionGroup
  case object Advertiser extends PermissionGroup

  case object MSNRead extends PermissionGroup
  case object MSNWrite extends PermissionGroup
  case object MSNCharts extends PermissionGroup

  case object GoogleRead extends PermissionGroup
  case object GoogleWrite extends PermissionGroup
  case object GoogleCharts extends PermissionGroup

  case object YahooRead extends PermissionGroup
  case object YahooWrite extends PermissionGroup
  case object YahooCharts extends PermissionGroup

  case object FacebookRead extends PermissionGroup
  case object FacebookWrite extends PermissionGroup
  case object FacebookCharts extends PermissionGroup

  case object LynxRead extends PermissionGroup
  case object LynxWrite extends PermissionGroup
  case object LynxCharts extends PermissionGroup
}

case class SecurityRole(_id: Option[ObjectId], var roleName: String, var permissions: Array[PermissionGroup]) extends Role {
  def getName = this.roleName
  def getValue = this.permissions.mkString(",")

  override def name: String = this.roleName
}

object SecurityRole {
  def securityRoleCollection = advancedCollection("security_role")
  def securityRoleToDbo(securityRole: SecurityRole): DBObject = {
    DBObject(
      "_id" -> Some(securityRole._id),
      "permissions" -> securityRole.permissions.map(_.toString),
      "roleName" -> securityRole.roleName
    )
  }

  def dboToSecurityRole(dbo: DBObject) = SecurityRole(
    _id=dbo._id,
    roleName=dbo.getAs[String]("roleName").get,
    permissions=dbo.getAsOrElse[List[String]]("permissions", List()).map(x => PermissionGroup.withName(x)).toArray
  )
  
  def isAdministrator(securityRole: SecurityRole): Boolean = {
    securityRole.permissions.contains(PermissionGroup.Administrator)
  }
  
  def isAdvertiser(securityRole: SecurityRole): Boolean = {
    securityRole.permissions.contains(PermissionGroup.Advertiser)
  }
  
  def hasPermission(securityRole: SecurityRole, permission: PermissionGroup): Boolean = {
    securityRole.permissions.contains(permission)
  }
}