package models.mongodb

import be.objectify.deadbolt.scala.models.{Permission, Role}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import enumeratum._
import org.bson.types.ObjectId
import models.mongodb.MongoExtensions._

import scala.jdk.CollectionConverters._

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

  case object FacebookRead extends PermissionGroup
  case object FacebookWrite extends PermissionGroup
  case object FacebookCharts extends PermissionGroup

  case object LynxRead extends PermissionGroup
  case object LynxWrite extends PermissionGroup
  case object LynxCharts extends PermissionGroup
}

case class SecurityRole(_id: Option[ObjectId], roleName: String, permissions: Array[PermissionGroup]) extends Role {
  override def name: String = this.roleName
}

object SecurityRole {
  // Initialized by StartupTasks from MongoService
  var securityRoleCollection: MongoCollection[Document] = _

  def securityRoleToDocument(securityRole: SecurityRole): Document = {
    Document(
      "_id" -> Some(securityRole._id),
      "permissions" -> securityRole.permissions.map(_.toString).toList,
      "roleName" -> securityRole.roleName
    )
  }

  def documentToSecurityRole(doc: Document) = SecurityRole(
    _id = Option(doc.getObjectId("_id")),
    roleName = doc.getString("roleName"),
    permissions = Option(doc.getList("permissions", classOf[String]))
      .map(_.asScala.toList).getOrElse(List()).map(x => PermissionGroup.withName(x)).toArray
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
