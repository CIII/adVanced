/**
 *
 */
package security

import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltHandler, DynamicResourceHandler}
import com.mongodb.casbah.Imports._
import models.mongodb.{PermissionGroup, UserAccount}
import play.api.Logger
import play.api.mvc.{Request, Security}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class MyDynamicResourceHandler extends DynamicResourceHandler{

  override def isAllowed[A](name: String, meta: Option[Any], deadboltHandler: DeadboltHandler, request: AuthenticatedRequest[A]): Future[Boolean] = {
    request.subject match {
      case Some(subject) =>
        UserAccount.userAccountCollection.findOne(DBObject("userName" -> subject.identifier)) match {
          case Some(userAccount) =>
            Future(UserAccount.dboToUserAccount(userAccount).permissions.contains(PermissionGroup.withName(name)))
          case None =>
            Future(false)
        }
      case None =>
        Future(false)
    }
  }

  override def checkPermission[A](
    permissionValue: String,
    meta: Option[Any],
    deadboltHandler: DeadboltHandler,
    request: AuthenticatedRequest[A]
  ): Future[Boolean] = Future(false)
}