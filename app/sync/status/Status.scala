package sync.status

import Shared.Shared._
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import org.joda.time._
import org.joda.time.format._
import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

case class Refresh()
case class Connect( user: String )
case class Connected( enumerator: Enumerator[ String ] )

object Status {
  implicit val timeout = Timeout( 5 second )
  var actors: Map[ String, ActorRef ] = Map()

  def actor( user: String ) = actors.synchronized {
    actors.find( _._1 == user ).map( _._2 ) match {
      case Some(actor) => actor
      case None => {
        val actor = Akka.system.actorOf( Props( new StatusActor(user) ), name = s"user-$user" )
        Akka.system.scheduler.schedule( 0.seconds, 3.second, actor, Refresh )
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

class StatusActor(user: String ) extends Actor {
  val ( enumerator, channel ) = Concurrent.broadcast[String]

  def receive = {
    case Connect( host ) => sender ! Connected( enumerator )
    case Refresh => broadcast( user )
  }

  def broadcast( user: String ) {
    val task_key = user + task_ext
    val task_cache = Cache.get(task_key).orNull
    val data = scala.collection.mutable.ListBuffer[(String, Any)]()
    if(task_cache != null) {
      for (task <- task_cache.asInstanceOf[List[TaskStructure]]) {
        val period = new Period(task.startTime, new DateTime)
        val hms = new PeriodFormatterBuilder minimumPrintedDigits 2 printZeroAlways() appendHours() appendSeparator ":" appendMinutes() appendSuffix ":" appendSeconds() toFormatter()
        data += ("id" -> 1234)
      }
    }
    channel.push(gson.toJson(data.toList))
  }
}