package models.mongodb

import Shared.Shared._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime

import scala.collection.immutable.List

object Task extends {
  def taskCollection = advancedCollection("task")

  def taskToDbo(ts: TaskStructure): DBObject =
    DBObject(
      "id" -> ts.id,
      "user" -> ts.user,
      "data" -> ts.data.map(pendingCacheStructureToDbo),
      "startTime" -> ts.startTime.getMillis,
      "complete" -> ts.complete,
      "completeTime" -> (if(ts.completeTime.isEmpty) None else ts.completeTime.get.getMillis),
      "processes" -> ts.processes.map(processToDbo)
    )

  def dboToTask(dbo: DBObject) = TaskStructure(
    id=dbo.as[Long]("id"),
    user=dbo.getAsOrElse[String]("user", "SYSTEM"),
    data=dbo.getAsOrElse[List[DBObject]]("data", List()).map(dboToPendingCacheStructure),
    startTime=new DateTime(dbo.as[Long]("startTime")),
    complete=dbo.getAsOrElse[Boolean]("complete", false),
    completeTime=Some(new DateTime(dbo.getAsOrElse[Option[Long]]("completeTime", None).getOrElse(0L))),
    processes=dbo.getAsOrElse[List[DBObject]]("processes", List()).map(dboToProcess)
  )
}