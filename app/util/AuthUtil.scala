package util

import Shared.Shared._
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.UserAccount
import models.mongodb.MongoExtensions._

case class UserNotFoundException(error: String)  extends Exception(error)

object AuthUtil {
  
  // TODO: Convert to injectable class
  def authenticate(username: String, password: String): Option[UserAccount] = {
    UserAccount.userAccountCollection.findOne(Document("userName" -> username)) match {
      case Some(userAccountObj) =>
        val userAccount = UserAccount.documentToUserAccount(userAccountObj)
        if(checkPasswordsMatch(password, userAccount.password)){
          Some(userAccount)
        } else {
          None
        }
      case _ => None
    }
  }
}
