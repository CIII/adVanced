package api

import javax.inject.Inject

import play.api.mvc._
import com.mongodb.casbah.Imports._
import models.mongodb.ApiToken

trait TokenAuthentication {
  self: Controller =>

  def extractToken(authHeader: String): Option[String] = {
    authHeader.split("Token token=") match {
      case Array(_, token) => Some(token)
      case _ => None
    }
  }

  /**
    * Fetch an API token from the request headers
    *
    * If one exists then allow the request else deny it
    *
    * curl -i https://tapatlas.com/api/[ROUTE] -H "Authorization: Token token=[TOKEN]"
    */
  def withApiToken(f: => ApiToken => Request[AnyContent] => Result) = Action { implicit request =>
    request.headers.get("Authorization") match {
      case Some(authHeaderToken) =>
        extractToken(authHeaderToken) match {
          case Some(token) =>
            ApiToken.apiTokenCollection.findOne(DBObject("token" -> token)) match {
              case Some(token_obj) if ApiToken.dboToApiToken(token_obj).active =>
                f(ApiToken.dboToApiToken(token_obj))(request)
              case _ => Unauthorized("Invalid API token")
            }
          case None => Unauthorized("Invalid API token")
        }
      case None => Unauthorized("Missing Authorization")
    }
  }
}