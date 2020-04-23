package controllers

import javax.inject.Inject

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.mongodb.casbah.Imports._
import helpers.GoogleAuthenticationHelper
import helpers.LoginControllerHelper._
import models.mongodb.UserAccount
import org.mindrot.jbcrypt.BCrypt
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import util._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationController @Inject()(
  val messagesApi: MessagesApi,
  deadbolt: DeadboltActions,
  handler: HandlerCache,
  actionBuilder: ActionBuilders,
  googleAuthHelper: GoogleAuthenticationHelper
) extends Controller with I18nSupport {

  
  def login = Action.async {
    implicit request =>
      request.session.get(Security.username) match {
        case Some(_) => Future(Redirect(routes.DashboardController.dashboard()))
        case None => Future(Ok(views.html.login(loginForm)))
      }
  }

  def logout = Action.async {
    Future(Redirect(routes.ApplicationController.login()).withNewSession.flashing(
      "success" -> "You are now logged out."
    ))
  }

  def changePassword = deadbolt.SubjectPresent()(){
    implicit request =>
      Future(Ok(views.html.password_change(
        passwordChangeForm.fill(
          PasswordChange(
            username=request.subject.get.asInstanceOf[UserAccount].userName,
            currentPassword="",
            newPassword="",
            confirmNewPassword=""
          )
        ),
        ""
      )))
  }

  def executeChangePassword = deadbolt.SubjectPresent()(){
    implicit request =>
      passwordChangeForm.bindFromRequest().fold(
        formWithErrors => {
          Future(BadRequest(views.html.password_change(formWithErrors, "")))
        },
        passwordChange => {
          if(!passwordChange.newPassword.equals(passwordChange.confirmNewPassword)){
            Future(BadRequest(views.html.password_change(
              passwordChangeForm.fill(passwordChange),
              "New password fields must match"
            )
            ))
          } else {
            AuthUtil.authenticate(passwordChange.username, passwordChange.currentPassword) match {
              case Some(userAcct) =>
                userAcct.password = BCrypt.hashpw(passwordChange.newPassword, BCrypt.gensalt)
                UserAccount.userAccountCollection.update(DBObject("userName" -> userAcct.userName), UserAccount.userAccountToDbo(userAcct));
            }

            Future(Redirect(controllers.routes.UserProfileController.user_profile(passwordChange.username)))
          }
        }
      )
  }
  
  def authenticate = Action.async {
    implicit request =>
      loginForm.bindFromRequest.fold(
          formWithErrors => Future(BadRequest(views.html.login(formWithErrors))),
          user => {
            if(UserAccount.isAdvertiser(user.get)){
              Logger.info("Logged in advertiser:%d for user account %s".format(user.get.advertiserIds.head, user.get.userName))
              Future(Redirect(controllers.advertisers.routes.AdvertiserController.advertiser(user.get.advertiserIds.head))
                .withSession(request.session + (Security.username -> user.get.userName)))
            } else {
                Logger.info("Successfully logged in user: %s".format(user.get.userName))
                Future(Redirect(routes.DashboardController.dashboard())
                  .withSession(request.session + (Security.username -> user.get.userName)))
            }
          }
      )
  }
  val loginForm = Form(
      mapping(
          "username" -> text,
          "password" -> text
      )(AuthUtil.authenticate)(_.map(u => (u.userName, ""))).verifying("Invalid username or password.", result => result.isDefined)
  )
}
