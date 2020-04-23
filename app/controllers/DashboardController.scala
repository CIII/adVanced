package controllers


import javax.inject.Inject

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import helpers.DashboardControllerHelper._
import models.mongodb.{SessionSettings, UserAccount}
import org.joda.time.DateTime
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DashboardController @Inject()(
  val messagesApi: MessagesApi,
  deadbolt: DeadboltActions,
  handler: HandlerCache,
  actionBuilder: ActionBuilders
) extends Controller with I18nSupport {
  
  def dashboard = deadbolt.SubjectPresent()() {
      // Route depending on the user.  If they are an advertiser, send them 
      // to the advertiser dashboard.  Otherwise, route them to the standard dash.
      implicit authRequest => {
        authRequest.subject.get.asInstanceOf[UserAccount] match {
          case user if UserAccount.isAdvertiser(user) =>
            Future(Redirect(controllers.advertisers.routes.AdvertiserController.advertiser(user.advertiserIds.head)))
          case user => 
            val userSettings = SessionSettings.getSettings(user)
            val startDate: DateTime = authRequest.getQueryString("startDate") match {
              case Some(dateStr) => 
                val dateTime = DateTime.parse(dateStr)
                SessionSettings.setChartStartDate(user, dateTime)
                dateTime
              case _ => userSettings.chartStartDate
            }
            
            val endDate: DateTime = authRequest.getQueryString("endDate") match {
              case Some(dateStr) => 
                val dateTime = DateTime.parse(dateStr)
                SessionSettings.setChartEndDate(user, dateTime)
                dateTime
              case _ => userSettings.chartEndDate
            }
            
            Future(Ok(views.html.dashboard(new DashboardPerformanceChart(dashboardMetaData(user)))))
        }
      }
  }

  def permission_denied = Action.async {
    implicit request =>
      Future(Ok(views.html.permission_denied()))
  }
}