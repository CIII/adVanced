package util

import Shared.Shared._
import com.mongodb.casbah.Imports._
import models.mongodb.UserAccount

case class UserNotFoundException(error: String)  extends Exception(error)

object AuthUtil {
  
  def authenticate(username: String, password: String): Option[UserAccount] = {
    UserAccount.userAccountCollection.findOne(DBObject("userName" -> username)) match {
      case Some(userAccountObj) =>
        val userAccount = UserAccount.dboToUserAccount(userAccountObj)
        if(checkPasswordsMatch(password, userAccount.password)){
          Some(userAccount)
        } else {
          None
        }
      case _ => None
    }
  }
}
