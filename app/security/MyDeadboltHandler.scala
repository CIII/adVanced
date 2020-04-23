/**
 *
 */
package security

import be.objectify.deadbolt.scala.models.Subject
import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltHandler, DynamicResourceHandler}
import com.mongodb.casbah.Imports._
import models.mongodb.UserAccount
import play.api.Logger
import play.api.mvc.Results.Forbidden
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._


class MyDeadboltHandler(dynamicResourceHandler: Option[DynamicResourceHandler] = None) extends DeadboltHandler{

  def beforeAuthCheck[A](request: Request[A]): Future[Option[Result]] = Future(None)

  override def getDynamicResourceHandler[A](request: Request[A]): Future[Option[DynamicResourceHandler]] = {
    if (dynamicResourceHandler.isDefined) Future(dynamicResourceHandler)
    else Future(Some(new MyDynamicResourceHandler()))
  }

  override def getSubject[A](request: AuthenticatedRequest[A]): Future[Option[UserAccount]] = {
    request.session.get(Security.username) match {
      case Some(username) =>
        Future(Some(
          UserAccount.dboToUserAccount(
            UserAccount.userAccountCollection.findOne(DBObject("userName" -> username)).get.asDBObject
          )
        ))
      case None =>
        Future(None)
    }
  }

  override def onAuthFailure[A](request: AuthenticatedRequest[A]): Future[Result] =
    Future(Forbidden(views.html.permission_denied.render(null)))
}