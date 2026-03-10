package api

import play.api.mvc._
import org.mongodb.scala.bson.Document
import models.mongodb.ApiToken

trait TokenAuthentication {
  self: BaseController =>

  def extractToken(authHeader: String): Option[String] = {
    authHeader.split("Token token=") match {
      case Array(_, token) => Some(token)
      case _ => None
    }
  }

  /**
   * Fetch an API token from the request headers.
   *
   * If one exists then allow the request else deny it.
   *
   * curl -i https://tapatlas.com/api/[ROUTE] -H "Authorization: Token token=[TOKEN]"
   *
   * TODO: Make async with MongoService when MongoDB migration is complete.
   */
  def withApiToken(f: => ApiToken => Request[AnyContent] => Result): Action[AnyContent] = Action { implicit request =>
    request.headers.get("Authorization") match {
      case Some(authHeaderToken) =>
        extractToken(authHeaderToken) match {
          case Some(_) =>
            // TODO: Re-implement with async MongoDB driver
            Unauthorized("API token authentication not yet migrated")
          case None => Unauthorized("Invalid API token")
        }
      case None => Unauthorized("Missing Authorization")
    }
  }
}
