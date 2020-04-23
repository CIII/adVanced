package controllers

import javax.inject.Inject

import Shared.Shared._
import akka.actor.Props
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.mongodb.casbah.Imports._
import models.mongodb.{PermissionGroup, Task}
import org.joda.time.DateTime
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import security.HandlerKeys
import sync.shared.Google._
import sync.shared.Msn._
import sync.shared.Yahoo._
import sync.tasks.TaskStatus

import scala.collection.immutable.List
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaskController @Inject()(
  val messagesApi: MessagesApi,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders
) extends Controller with I18nSupport {

  def tasks = WebSocket.tryAccept[String] { request =>
    TaskStatus.attach(request.session.get(Security.username).get)
  }

  def run(id: String) = deadbolt.SubjectPresent()() {
    implicit request =>
      val user = request.session.get(Security.username).get
      Task.taskCollection.findOne(DBObject("user" -> user, "task.id" -> id.toLong)) match {
        case Some(task_obj) =>
          setPendingCache(Left(request), pendingCache(Left(request)) ::: dboToTaskStructure(task_obj.as[DBObject]("task")).data)
        case _ =>
      }
      Future(Redirect(routes.TaskController.view(id)))
  }

  def view(id: String) = deadbolt.SubjectPresent()() {
    implicit request =>
      val user = request.session.get(Security.username).get
      val runningTasks = taskCache(Right(user))
      Task.taskCollection.findOne(DBObject("user" -> user, "task.id" -> id.toLong)) match {
        case Some(task_obj) =>
          Future(Ok(views.html.task.view_task(id.toLong, dboToTaskStructure(task_obj.as[DBObject]("task")), runningTasks)))
        case _ =>
          Future(NotFound)
      }
  }

  def pending_changes = deadbolt.SubjectPresent()() {
    implicit authRequest =>
      Future(Ok(views.html.pending(pendingCache(Left(authRequest)))))
  }

  def cancel_change(index: String) = deadbolt.SubjectPresent()() {
    implicit authRequest =>
      setPendingCache(Left(authRequest), pendingCache(Left(authRequest)).filterNot(_.id == index.toLong))
      Future(Ok(views.html.pending(pendingCache(Left(authRequest)))))
  }

  def confirm_changes = deadbolt.SubjectPresent()() {
    implicit request =>
      val user = request.session.get(Security.username).get
      val task = TaskStructure(
        id=Task.taskCollection.count(DBObject("user" -> user))+1,
        user=user,
        data=pendingCache(Left(request)),
        startTime=new DateTime(),
        complete=false,
        completeTime=None,
        processes=List()
      )
      var subprocess_count = 0
      for(item <- pendingCache(Left(request))) {
        item.trafficSource match {
          case TrafficSource.GOOGLE =>
            (item.changeCategory match {
                case ChangeCategory.MCC =>
                  googleManagementActorSystem.actorOf(Props(new MccActor))
                case ChangeCategory.CAMPAIGN =>
                  googleManagementActorSystem.actorOf(Props(new CampaignActor))
                case ChangeCategory.AD_GROUP =>
                  googleManagementActorSystem.actorOf(Props(new AdGroupActor))
              }
            ) ! PendingCacheMessage(cache=Some(item), request=Some(request))
          case TrafficSource.MSN =>
            (item.changeCategory match {
              case ChangeCategory.API_ACCOUNT =>
                msnManagementActorSystem.actorOf(Props(new ApiAccountActor))
              case ChangeCategory.ACCOUNT_INFO =>
                msnManagementActorSystem.actorOf(Props(new AccountInfoActor))
            }) ! PendingCacheMessage(cache=Some(item), request=Some(request))
                subprocess_count = 5
          case TrafficSource.YAHOO =>
            (
              item.changeCategory match {
                case ChangeCategory.API_ACCOUNT =>
                  yahooManagementActorSystem.actorOf(Props(new ApiAccountActor))
              }
            ) ! PendingCacheMessage(cache=Some(item), request=Some(request))
            
          case TrafficSource.FACEBOOK =>
            (
               item.changeCategory match {
                 case ChangeCategory.API_ACCOUNT =>
                   facebookManagementActorSystem.actorOf(Props(new sync.facebook.process.api_account.ApiAccountActor))
                 case ChangeCategory.CAMPAIGN =>
                   facebookManagementActorSystem.actorOf(Props(new sync.facebook.process.api_account.campaign.CampaignActor))
                 case ChangeCategory.AD_SET =>
                   facebookManagementActorSystem.actorOf(Props(new sync.facebook.process.api_account.campaign.ad_set.AdSetActor))
                 case ChangeCategory.AD =>
                   facebookManagementActorSystem.actorOf(Props(new sync.facebook.process.api_account.campaign.ad_set.ad.AdActor))
                 case ChangeCategory.AD_STUDY =>
                   facebookManagementActorSystem.actorOf(Props(new FacebookSplitTestActor))
               } 
            ) ! PendingCacheMessage(cache=Some(item), request=Some(request))
        }
        setPendingCache(
          Left(request),
          pendingCache(Left(request)).filterNot (_.id == item.id)
        )
        task.processes = task.processes :+ Process(changeDataId=item.id, subProcesses=subprocess_count, completedSubProcesses=0)
      }
      setTaskCache(Left(request), taskCache(Left(request)) :+ task)
      Task.taskCollection.insert(taskStructureToDbo(task))
      Future(Redirect(routes.DashboardController.dashboard()))
  }

  def history(page: Int, pageSize: Int, orderBy: Int, filter: String) =
    deadbolt.Dynamic(name = PermissionGroup.LynxRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
      implicit request =>
        val tasks = Task.taskCollection.find(DBObject()).skip(page * pageSize).limit(pageSize).toList
        Future(Ok(views.html.history(
          tasks.map(dboToTaskStructure),
          page,
          pageSize,
          orderBy,
          filter,
          Task.taskCollection.count()
        )))
    }
}
