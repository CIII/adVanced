package sync.tasks

import javax.inject.{Inject, Singleton}
import org.apache.pekko.actor.{Actor, ActorRef, ActorSystem, Props}
import org.apache.pekko.event.Logging
import org.apache.pekko.pattern.ask
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.Timeout
import org.mongodb.scala.bson.Document
import models.mongodb.Task
import org.joda.time._
import org.joda.time.format._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

case class TaskRefresh()
case class TaskConnect(user: String)
case class TaskConnected(source: Source[String, _])

/**
 * Manages WebSocket connections for task status updates.
 * Migrated from Play 2.5 Iteratee/Enumerator to Pekko Streams.
 */
@Singleton
class TaskStatus @Inject()(actorSystem: ActorSystem)(implicit ec: ExecutionContext) {
  implicit val timeout: Timeout = Timeout(10.seconds)
  private var actors: Map[String, ActorRef] = Map()

  def actor(user: String): ActorRef = actors.synchronized {
    actors.get(user) match {
      case Some(ref) => ref
      case None =>
        val ref = actorSystem.actorOf(Props(new TasksActor(user)), name = s"task-user-$user")
        actorSystem.scheduler.scheduleWithFixedDelay(0.seconds, 2.seconds, ref, TaskRefresh())
        actors += (user -> ref)
        ref
    }
  }
}

class TasksActor(user: String) extends Actor {
  val log = Logging(context.system, this)

  def receive: Receive = {
    case TaskConnect(_) =>
      // TODO: Implement Pekko Streams source for task updates
      sender() ! TaskConnected(Source.empty[String])
    case TaskRefresh() =>
      // TODO: Re-implement broadcast using MongoService (async)
      // Original code queried Task.taskCollection and taskCache
  }
}
