package models.mongodb

import Shared.Shared._
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import org.joda.time.DateTime
import models.mongodb.MongoExtensions._

import scala.jdk.CollectionConverters._
import scala.collection.immutable.List

object Task {
  // Initialized by StartupTasks from MongoService
  var taskCollection: MongoCollection[Document] = _

  def taskToDocument(ts: TaskStructure): Document =
    Document(
      "id" -> ts.id,
      "user" -> ts.user,
      "data" -> ts.data.map(pendingCacheStructureToDocument),
      "startTime" -> ts.startTime.getMillis,
      "complete" -> ts.complete,
      "completeTime" -> (if(ts.completeTime.isEmpty) None else Some(ts.completeTime.get.getMillis)),
      "processes" -> ts.processes.map(processToDocument)
    )

  def documentToTask(doc: Document) = TaskStructure(
    id = doc.getLong("id"),
    user = Option(doc.getString("user")).getOrElse("SYSTEM"),
    data = Option(doc.getList("data", classOf[Document]))
      .map(_.asScala.toList.map(documentToPendingCacheStructure)).getOrElse(List()),
    startTime = new DateTime(doc.getLong("startTime")),
    complete = Option(doc.getBoolean("complete")).map(_.booleanValue()).getOrElse(false),
    completeTime = Option(doc.getLong("completeTime")).map(ct => new DateTime(ct.longValue())),
    processes = Option(doc.getList("processes", classOf[Document]))
      .map(_.asScala.toList.map(documentToProcess)).getOrElse(List())
  )

  def pendingCacheStructureToDocument(pcs: PendingCacheStructure): Document =
    Document(
      "id" -> pcs.id,
      "changeCategory" -> pcs.changeCategory.toString,
      "trafficSource" -> pcs.trafficSource.toString,
      "changeType" -> pcs.changeType.toString,
      "changeData" -> pcs.changeData
    )

  def documentToPendingCacheStructure(doc: Document): PendingCacheStructure =
    PendingCacheStructure(
      id = doc.getLong("id"),
      changeCategory = ChangeCategory.withName(doc.getString("changeCategory")),
      trafficSource = TrafficSource.withName(doc.getString("trafficSource")),
      changeType = ChangeType.withName(doc.getString("changeType")),
      changeData = Option(doc.toBsonDocument.get("changeData")).map(v => Document(v.asDocument())).getOrElse(Document())
    )

  def processToDocument(p: Process): Document =
    Document(
      "changeDataId" -> p.changeDataId,
      "subProcesses" -> p.subProcesses,
      "completedSubProcesses" -> p.completedSubProcesses
    )

  def documentToProcess(doc: Document): Process =
    Process(
      changeDataId = doc.getLong("changeDataId"),
      subProcesses = doc.getInteger("subProcesses"),
      completedSubProcesses = doc.getInteger("completedSubProcesses")
    )
}
