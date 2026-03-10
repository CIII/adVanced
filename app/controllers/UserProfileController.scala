package controllers

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import com.mongodb.client.model.ReplaceOptions
import helpers.UserProfileControllerHelper
import helpers.UserProfileControllerHelper._
import models.mongodb.MongoExtensions._
import models.mongodb.{SecurityRole, UserAccount}
import play.api.{Configuration, Logger}
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class UserProfileController @Inject()(
  val controllerComponents: ControllerComponents,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders,
  config: Configuration
)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {
  val logger = Logger(this.getClass)

  def user_profile(username: String) = deadbolt.SubjectPresent()() {
    implicit request =>
      UserAccount.userAccountCollection
        .findOne(Document("userName" -> username)) match {
          case Some(userAccountObj) =>
            val availSecurityRoles = SecurityRole.securityRoleCollection.find.toList
              .map(x => SecurityRole.documentToSecurityRole(x))
            val userAccount = UserAccount.documentToUserAccount(userAccountObj)
            val userSecurityRoleNames = availSecurityRoles.filter(role => userAccount.securityRoles.contains(role._id.get)).map(_.roleName)
            
            Future(Ok(
              views.html.user_profile(
                userProfileForm.fill(
                  UserProfileForm(
                    _id = Some(userAccountObj.getObjectId("_id").toString),
                    username = userAccount.userName,
                    password = userAccount.password,
                    email = userAccount.email,
                    advertiserIds = Some(userAccount.advertiserIds.mkString(",")),
                    security_roles = userSecurityRoleNames,
                    security_roles_str = Some(userSecurityRoleNames.mkString)
                  )
                ),
                availSecurityRoles
              )
            ))
      }
      
  }
  
  def new_user = deadbolt.SubjectPresent()() {
    implicit request =>
      val availSecurityRoles = SecurityRole.securityRoleCollection.find.toList
        .map(x => SecurityRole.documentToSecurityRole(x))
      Future(Ok(
        views.html.user_profile(
            userProfileForm.fill(
        		  UserProfileForm(None, "", "", "", Some(""), List(), Some(""))
            ),
            availSecurityRoles
        )
      ))
  }

  def save = deadbolt.SubjectPresent()() {
    implicit request =>
      val availSecurityRoles = SecurityRole.securityRoleCollection.find.toList.map(x => SecurityRole.documentToSecurityRole(x))
      userProfileForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(views.html.user_profile(formWithErrors, availSecurityRoles)))
        },
        user_profile_form => {
          logger.debug("Saving User Profile: %s".format(user_profile_form.toString))
          
          // Get the object Id for each specified security role
          val securityRoleIds = user_profile_form.security_roles.map{ 
            roleName => {
              SecurityRole.securityRoleCollection.findOne(Document("roleName" -> roleName)) match {
                case Some(role) =>
                  SecurityRole.documentToSecurityRole(role)._id.get
              }
            }
          }
          
          // Upsert user account collection
          val finalPassword = if(user_profile_form.password.equals("")){
            logger.debug("Generating new password")
            val newPass = generatePassword()
            sendPasswordEmail(user_profile_form.email, newPass)
            encryptPassword(newPass)
          } else {
            user_profile_form.password
          }

          logger.debug("Updating user account")
          UserAccount.userAccountCollection.replaceOne(
            Document("userName" -> user_profile_form.username),
            UserAccount.userAccountToDocument(
              UserAccount(
                _id = user_profile_form._id match {
                  case Some(id) => {
                    Some(new org.bson.types.ObjectId(id))
                  }
                  case None => {
                    Some(new org.bson.types.ObjectId)
                  }
                },
                userName = user_profile_form.username,
                password = finalPassword,
                email = user_profile_form.email,
                advertiserIds = user_profile_form.advertiserIds match{
                  case Some(ids) => ids.split(",").toList.map { str => str.toInt }
                  case None => List()
                },
                securityRoles = securityRoleIds.toArray
              )
            ),
            new ReplaceOptions().upsert(true)
          )
          
          Future(Redirect(controllers.routes.UserProfileController.user_profile(user_profile_form.username)))
        }
      )
  }
  
  def resetPassword(username: String) = deadbolt.SubjectPresent()(){
    implicit request =>
      UserAccount.userAccountCollection.findOne(Document("userName" -> username)) match {
        case Some(userObj) =>
          val userAcct = UserAccount.documentToUserAccount(userObj)
          val newPass = generatePassword()
          sendPasswordEmail(userAcct.email, newPass)
          val updatedAcct = userAcct.copy(password = encryptPassword(newPass))
          UserAccount.userAccountCollection.replaceOne(Document("userName" -> updatedAcct.userName), UserAccount.userAccountToDocument(updatedAcct));
      }
      
      Future(Redirect(controllers.routes.UserProfileController.user_profile(username)))
  }
  
  def sendPasswordEmail(email: String, password: String): Unit = {
    // TODO: Re-implement with Play WS client to Mandrill API
    logger.warn(s"Password reset email not sent (Mandrill not configured) to: $email")
  }

}

