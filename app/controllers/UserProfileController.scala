package controllers

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.mongodb.casbah.Imports._
import com.tapquality.dao.MandrillDAO
import helpers.UserProfileControllerHelper
import helpers.UserProfileControllerHelper._
import models.mongodb.{SecurityRole, UserAccount}
import play.api.{Configuration, Logger}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserProfileController @Inject()(
  val messagesApi: MessagesApi,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders,
  mandrillDAO: MandrillDAO,
  config: Configuration
) extends Controller with I18nSupport {
  val passResetTemplateName = config.getString("password_reset_mandrill_template").getOrElse(throw new Exception("Missing Mandrill Password Reset Template Name"))

  def user_profile(username: String) = deadbolt.SubjectPresent()() {
    implicit request =>
      UserAccount.userAccountCollection
        .findOne(DBObject("userName" -> username)) match {
          case Some(userAccountObj) =>
            val availSecurityRoles = SecurityRole.securityRoleCollection.find.toList
              .map(x => SecurityRole.dboToSecurityRole(x.asDBObject))
            val userAccount = UserAccount.dboToUserAccount(userAccountObj.asDBObject)
            val userSecurityRoleNames = availSecurityRoles.filter(role => userAccount.securityRoles.contains(role._id.get)).map(_.roleName)
            
            Future(Ok(
              views.html.user_profile(
                userProfileForm.fill(
                  UserProfileForm(
                    _id = Some(userAccountObj._id.get.toString),
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
        .map(x => SecurityRole.dboToSecurityRole(x.asDBObject))
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
      val availSecurityRoles = SecurityRole.securityRoleCollection.find.toList.map(x => SecurityRole.dboToSecurityRole(x.asDBObject))
      userProfileForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(views.html.user_profile(formWithErrors, availSecurityRoles)))
        },
        user_profile_form => {
          Logger.debug("Saving User Profile: %s".format(user_profile_form.toString))
          
          // Get the object Id for each specified security role
          val securityRoleIds = user_profile_form.security_roles.map{ 
            roleName => {
              SecurityRole.securityRoleCollection.findOne(DBObject("roleName" -> roleName)) match {
                case Some(role) =>
                  role._id.get
              }
            }
          }
          
          // Upsert user account collection
          if(user_profile_form.password.equals("")){
            Logger.debug("Generating new password")
            var newPass = generatePassword()
            sendPasswordEmail(user_profile_form.email, newPass)
            user_profile_form.password = encryptPassword(newPass)
          }
          
          Logger.debug("Updating user account")
          UserAccount.userAccountCollection.update(
            DBObject("userName" -> user_profile_form.username),
            UserAccount.userAccountToDbo(
              UserAccount(
                _id = user_profile_form._id match {
                  case Some(id) => {
                    Some(new ObjectId(id))
                  }
                  case None => {
                    Some(new ObjectId)
                  }
                },
                userName = user_profile_form.username,
                password = user_profile_form.password,
                email = user_profile_form.email,
                advertiserIds = user_profile_form.advertiserIds match{
                  case Some(ids) => ids.split(",").toList.map { str => str.toInt }
                  case None => List()
                },
                securityRoles = securityRoleIds.toArray
              )
            ),
            true
          )
          
          Future(Redirect(controllers.routes.UserProfileController.user_profile(user_profile_form.username)))
        }
      )
  }
  
  def resetPassword(username: String) = deadbolt.SubjectPresent()(){
    implicit request =>
      UserAccount.userAccountCollection.findOne(DBObject("userName" -> username)) match {
        case Some(userObj) =>
          var userAcct = UserAccount.dboToUserAccount(userObj)
          val newPass = generatePassword()
          sendPasswordEmail(userAcct.email, newPass)
          userAcct.password = encryptPassword(newPass)
          UserAccount.userAccountCollection.update(DBObject("userName" -> userAcct.userName), UserAccount.userAccountToDbo(userAcct));
      }
      
      Future(Redirect(controllers.routes.UserProfileController.user_profile(username)))
  }
  
  def sendPasswordEmail(email: String, password: String){
    Logger.debug("Sending password reset to " + email)
    val variables : Map[String, String] = Map("NEW_PASSWORD" -> password)
    Logger.debug(mandrillDAO.post(email, variables, passResetTemplateName, false))
  }

}

