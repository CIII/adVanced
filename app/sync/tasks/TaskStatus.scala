package sync.tasks

import Shared.Shared._
import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import com.mongodb.casbah.Imports._
import models.mongodb.Task
import org.joda.time._
import org.joda.time.format._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.duration._

case class Refresh()
case class Connect( user: String )
case class Connected( enumerator: Enumerator[ String ] )

object TaskStatus {
  implicit val timeout = Timeout( 10 second )
  var actors: Map[ String, ActorRef ] = Map()

  def actor( user: String ) = actors.synchronized {
    actors.find( _._1 == user ).map( _._2 ) match {
      case Some(actor) => actor
      case None => {
        val actor = Akka.system.actorOf( Props( new TasksActor(user) ), name = s"user-$user" )
        Akka.system.scheduler.schedule( 0.seconds, 2.second, actor, Refresh )
        actors += ( user -> actor )
        actor
      }
    }
  }

  def attach( user: String ): Future[ Either[Result, ( Iteratee[ String, _ ], Enumerator[ String ] ) ]] = {
    ( actor( user ) ? Connect( user ) ).map {
      case Connected( enumerator ) => Right(( Iteratee.ignore[String], enumerator ))
    }
  }
}

class TasksActor( user: String ) extends Actor {
  val log = Logging(context.system, this)
  val ( enumerator, channel ) = Concurrent.broadcast[String]

  def receive = {
    case Connect( host ) => sender ! Connected( enumerator )
    case Refresh => broadcast( user )
  }

  def broadcast( user: String ) {
    val data = scala.collection.mutable.ListBuffer[Map[String, String]]()
    val runningTasks = taskCache(Right(user))

    for (task <- Task.taskCollection.find(DBObject("user" -> user)).toList.map(y => dboToTaskStructure(y.as[DBObject]("task")))) {
      val isRunningTask = runningTasks.contains(task)
      val period = new Period(task.startTime, task.completeTime.getOrElse(isRunningTask match {
        case true =>
          new DateTime()
        case _ => task.startTime
      }))
      val hms = new PeriodFormatterBuilder()
        .minimumPrintedDigits(2)
        .printZeroAlways
        .appendHours
        .appendSeparator(":")
        .appendMinutes
        .appendSuffix(":")
        .appendSeconds
        .toFormatter
      val progress = try {
        Math.ceil((task.processes.map(_.completedSubProcesses).sum / task.processes.map(_.subProcesses).sum) * 100).toInt
      } catch {
        case _: Throwable => 0
      }
      data += Map(
        "taskId" -> task.id.toString,
        "progress" -> progress.toString,
        "totalProcesses" -> task.processes.size.toString,
        "totalCompletedProcesses" -> task.processes.map(x =>
          if(x.completedSubProcesses == (if(x.subProcesses == 0) 1 else x.subProcesses)) 1 else 0
        ).sum.toString,
        "totalSubProcesses" -> task.processes.map(x =>
          if(x.subProcesses == 0) 1 else x.subProcesses
        ).sum.toString,
        "totalCompletedSubProcesses" -> task.processes.map(x => x.completedSubProcesses).sum.toString,
        "elapstedTime" -> (hms print period),
        "changeCount" -> task.data.length.toString,
        "running" -> isRunningTask.toString
      )
    }
    if(!data.toArray.isEmpty)
      channel.push(Json.stringify(Json.toJson(data.toArray)))
  }
}