package security

import be.objectify.deadbolt.scala.models.Subject
import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltHandler, DynamicResourceHandler}
import org.mongodb.scala.bson.Document
import models.mongodb.UserAccount
import play.api.Logging
import play.api.mvc.Results.Forbidden
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class MyDeadboltHandler(dynamicResourceHandler: Option[DynamicResourceHandler] = None)(implicit ec: ExecutionContext)
  extends DeadboltHandler with Logging {

  def beforeAuthCheck[A](request: Request[A]): Future[Option[Result]] = Future.successful(None)

  override def getDynamicResourceHandler[A](request: Request[A]): Future[Option[DynamicResourceHandler]] = {
    if (dynamicResourceHandler.isDefined) Future.successful(dynamicResourceHandler)
    else Future.successful(Some(new MyDynamicResourceHandler()))
  }

  override def getSubject[A](request: AuthenticatedRequest[A]): Future[Option[Subject]] = {
    request.session.get("username") match {
      case Some(username) =>
        import models.mongodb.MongoExtensions._
        UserAccount.userAccountCollection.findOne(Document("userName" -> username)) match {
          case Some(doc) => Future.successful(Some(UserAccount.documentToUserAccount(doc)))
          case None => Future.successful(None)
        }
      case None =>
        Future.successful(None)
    }
  }

  override def onAuthFailure[A](request: AuthenticatedRequest[A]): Future[Result] =
    Future.successful(Forbidden(views.html.permission_denied.render(null)))
}
