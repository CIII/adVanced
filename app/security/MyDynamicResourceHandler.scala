package security

import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltHandler, DynamicResourceHandler}
import org.mongodb.scala.bson.Document
import models.mongodb.{PermissionGroup, UserAccount}
import play.api.Logging

import scala.concurrent.{ExecutionContext, Future}

class MyDynamicResourceHandler(implicit ec: ExecutionContext) extends DynamicResourceHandler with Logging {

  override def isAllowed[A](name: String, meta: Option[Any], deadboltHandler: DeadboltHandler, request: AuthenticatedRequest[A]): Future[Boolean] = {
    request.subject match {
      case Some(subject) =>
        val user = subject.asInstanceOf[UserAccount]
        val permission = PermissionGroup.withName(name)
        Future.successful(user.permissions.exists(_.value == permission.value))
      case None =>
        Future.successful(false)
    }
  }

  override def checkPermission[A](
    permissionValue: String,
    meta: Option[Any],
    deadboltHandler: DeadboltHandler,
    request: AuthenticatedRequest[A]
  ): Future[Boolean] = {
    request.subject match {
      case Some(subject) =>
        val user = subject.asInstanceOf[UserAccount]
        Future.successful(user.permissions.exists(_.value == permissionValue))
      case None =>
        Future.successful(false)
    }
  }
}
