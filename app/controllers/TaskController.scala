package controllers

import javax.inject.Inject

import Shared.Shared._
import org.apache.pekko.actor.{ActorSystem, Props}
import org.apache.pekko.stream.scaladsl.Flow
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.{MongoExtensions, PermissionGroup, Task}
import models.mongodb.MongoExtensions._
import org.joda.time.DateTime
import play.api.i18n.I18nSupport
import play.api.mvc._
import security.HandlerKeys

import scala.collection.immutable.List
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class TaskController @Inject()(
  val controllerComponents: ControllerComponents,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders,
  actorSystem: ActorSystem
)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {

  // TODO: Implement with Pekko Streams when task status broadcasting is re-enabled
  def tasks = WebSocket.accept[String, String] { _ =>
    Flow[String].map(_ => "{}")
  }

  def run(id: String) = deadbolt.SubjectPresent()() {
    implicit request =>
      val user = request.session.get("username").get
      Task.taskCollection.findOne(Document("user" -> user, "task.id" -> id.toLong)) match {
        case Some(task_obj) =>
          setPendingCache(Left(request), pendingCache(Left(request)) ::: Task.documentToTask(Document(task_obj.toBsonDocument.getDocument("task"))).data)
        case _ =>
      }
      Future(Redirect(routes.TaskController.view(id)))
  }

  def view(id: String) = deadbolt.SubjectPresent()() {
    implicit request =>
      val user = request.session.get("username").get
      val runningTasks = taskCache(Right(user))
      Task.taskCollection.findOne(Document("user" -> user, "task.id" -> id.toLong)) match {
        case Some(task_obj) =>
          Future(Ok(views.html.task.view_task(id.toLong, Task.documentToTask(Document(task_obj.toBsonDocument.getDocument("task"))), runningTasks)))
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
      val user = request.session.get("username").get
      val pendingItems = pendingCache(Left(request))
      var subprocess_count = 0
      var processList: List[Process] = List()
      for(item <- pendingItems) {
        item.trafficSource match {
          case TrafficSource.GOOGLE =>
            (item.changeCategory match {
                case ChangeCategory.MCC =>
                  actorSystem.actorOf(Props(new sync.google.process.management.mcc.MccActor))
                case ChangeCategory.CAMPAIGN =>
                  actorSystem.actorOf(Props(new sync.google.process.management.mcc.account.campaign.CampaignActor))
                case ChangeCategory.AD_GROUP =>
                  actorSystem.actorOf(Props(new sync.google.process.management.mcc.account.campaign.adgroup.AdGroupActor))
              }
            ) ! PendingCacheMessage(cache=Some(item), requestUsername=Some(user))
          case TrafficSource.MSN =>
            (item.changeCategory match {
              case ChangeCategory.API_ACCOUNT =>
                actorSystem.actorOf(Props(new sync.msn.process.management.api_account.ApiAccountActor))
              case ChangeCategory.ACCOUNT_INFO =>
                actorSystem.actorOf(Props(new sync.msn.process.management.api_account.account_info.AccountInfoActor))
            }) ! PendingCacheMessage(cache=Some(item), requestUsername=Some(user))
                subprocess_count = 5
          case TrafficSource.FACEBOOK =>
            (
               item.changeCategory match {
                 case ChangeCategory.API_ACCOUNT =>
                   actorSystem.actorOf(Props(new sync.facebook.process.api_account.ApiAccountActor))
                 case ChangeCategory.CAMPAIGN =>
                   actorSystem.actorOf(Props(new sync.facebook.process.api_account.campaign.CampaignActor))
                 case ChangeCategory.AD_SET =>
                   actorSystem.actorOf(Props(new sync.facebook.process.api_account.campaign.ad_set.AdSetActor))
                 case ChangeCategory.AD =>
                   actorSystem.actorOf(Props(new sync.facebook.process.api_account.campaign.ad_set.ad.AdActor))
                 case ChangeCategory.AD_STUDY =>
                   actorSystem.actorOf(Props(new sync.facebook.business.FacebookBusinessActor))
               }
            ) ! PendingCacheMessage(cache=Some(item), requestUsername=Some(user))
        }
        setPendingCache(
          Left(request),
          pendingCache(Left(request)).filterNot (_.id == item.id)
        )
        processList = processList :+ Process(changeDataId=item.id, subProcesses=subprocess_count, completedSubProcesses=0)
      }
      val task = TaskStructure(
        id=Task.taskCollection.count(Document("user" -> user)).toInt+1,
        user=user,
        data=pendingItems,
        startTime=new DateTime(),
        complete=false,
        completeTime=None,
        processes=processList
      )
      setTaskCache(Left(request), taskCache(Left(request)) :+ task)
      Task.taskCollection.insertOne(Task.taskToDocument(task))
      Future(Redirect(routes.DashboardController.dashboard))
  }

  def history(page: Int, pageSize: Int, orderBy: Int, filter: String) =
    deadbolt.Dynamic(name = PermissionGroup.LynxRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
      implicit request =>
        val tasks = Task.taskCollection.find(Document()).skip(page * pageSize).limit(pageSize).toList
        Future(Ok(views.html.history(
          tasks.map(Task.documentToTask),
          page,
          pageSize,
          orderBy,
          filter,
          Task.taskCollection.countSync().toInt
        )))
    }
}
