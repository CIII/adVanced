package models.mongodb.facebook

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.ws.WSClient
import scala.jdk.CollectionConverters._
import scala.concurrent.Await
import scala.concurrent.duration._
import models.mongodb.MongoExtensions._

object Facebook {
  // Initialized by StartupTasks from MongoService
  var facebookApiAccountCollection: MongoCollection[Document] = _
  var facebookCampaignCollection: MongoCollection[Document] = _
  var facebookAdSetCollection: MongoCollection[Document] = _
  var facebookAdCollection: MongoCollection[Document] = _
  var facebookSplitTestCollection: MongoCollection[Document] = _
  var facebookBusinessAccountCollection: MongoCollection[Document] = _
  // TODO: Initialize with a real report collection in StartupTasks
  var facebookReportCollectionVar: MongoCollection[Document] = _

  def facebookReportCollection(reportType: String): MongoCollection[Document] = facebookReportCollectionVar

  // Initialized by StartupTasks - used by FacebookBusinessHelper and subclasses
  var configuration: Configuration = _
  var ws: WSClient = _

  def mongoToCsv(docs: List[Document]): String = {
    // TODO: Implement proper CSV export for Facebook reports
    if (docs.isEmpty) return ""
    val keys = docs.head.toBsonDocument.keySet.asScala.toList
    val header = keys.mkString(",")
    val rows = docs.map { doc =>
      keys.map(k => Option(doc.get(k)).map(_.toString).getOrElse("")).mkString(",")
    }
    (header :: rows).mkString("\n")
  }

  case class FacebookApiAccount(
    _id: Option[ObjectId],
    accountId: String,
    applicationSecret: String,
    accessToken: String
  )

  /**
   * TODO: Replace Gson deserialization with Play JSON when Facebook SDK v20 is integrated.
   */
  def documentToFacebookEntity(doc: Document, listKey: String, objectKey: Option[String]): Document = {
    objectKey match {
      case Some(key) =>
        val listDoc = Option(doc.toBsonDocument.get(listKey)).map(v => Document(v.asDocument())).get
        val objectDoc = Option(listDoc.toBsonDocument.get("object")).map(v => Document(v.asDocument())).get
        Option(objectDoc.toBsonDocument.get(key)).map(v => Document(v.asDocument())).get
      case None =>
        val listDoc = Option(doc.toBsonDocument.get(listKey)).map(v => Document(v.asDocument())).get
        Option(listDoc.toBsonDocument.get("object")).map(v => Document(v.asDocument())).get
    }
  }

  def apiAccountToDocument(faa: FacebookApiAccount): Document = {
    Document(
      "_id" -> faa._id.getOrElse(new ObjectId),
      "accountId" -> faa.accountId,
      "applicationSecret" -> faa.applicationSecret,
      "accessToken" -> faa.accessToken
    )
  }

  def documentToApiAccount(doc: Document): FacebookApiAccount = {
    FacebookApiAccount(
      _id = Option(doc.getObjectId("_id")),
      accountId = doc.getString("accountId"),
      applicationSecret = doc.getString("applicationSecret"),
      accessToken = doc.getString("accessToken")
    )
  }

  case class FacebookBusinessAccount(
    _id: Option[ObjectId],
    accountNumber: String,
    accessToken: String
  )

  object FacebookBusinessAccount{

    def toDocument(fbBizAccount: FacebookBusinessAccount): Document = {
      Document(
        "_id" -> fbBizAccount._id,
        "accountNumber" -> fbBizAccount.accountNumber,
        "accessToken" -> fbBizAccount.accessToken
      )
    }

    def fromDocument(doc: Document): FacebookBusinessAccount = {
      FacebookBusinessAccount(
        _id = Option(doc.getObjectId("_id")),
        accountNumber = doc.getString("accountNumber"),
        accessToken = doc.getString("accessToken")
      )
    }

    def findById(id: ObjectId): Option[FacebookBusinessAccount] = {
      facebookBusinessAccountCollection.findOne(Document("_id" -> id)).map(fromDocument)
    }

    def findByAccountNumber(accountNumber: String): Option[FacebookBusinessAccount] = {
      facebookBusinessAccountCollection.findOne(Document("accountNumber" -> accountNumber)).map(fromDocument)
    }
  }

  case class FacebookSplitTest(
    _id: Option[ObjectId],
    bizObjectId: Option[ObjectId],
    adStudyId: Option[String],
    name: String,
    description: String,
    startTime: DateTime,
    endTime: DateTime,
    testType: String,
    cells: List[FacebookSplitTestCell]
  )

  object FacebookSplitTest{

    def fromDocument(doc: Document): FacebookSplitTest = {
      FacebookSplitTest(
        _id = Option(doc.getObjectId("_id")),
        bizObjectId = Option(doc.getObjectId("bizObjectId")),
        adStudyId = Option(doc.getString("adStudyId")),
        name = doc.getString("name"),
        description = doc.getString("description"),
        startTime = DateTime.parse(doc.getString("startTime")),
        endTime = DateTime.parse(doc.getString("endTime")),
        testType = doc.getString("testType"),
        cells = doc.getList("cells", classOf[Document]).asScala.toList.map(FacebookSplitTestCell.fromDocument)
      )
    }

    def toDocument(st: FacebookSplitTest): Document = {
      Document(
        "_id" -> st._id,
        "bizObjectId" -> st.bizObjectId,
        "name" -> st.name,
        "adStudyId" -> st.adStudyId,
        "description" -> st.description,
        "startTime" -> st.startTime.toString,
        "endTime" -> st.endTime.toString,
        "testType" -> st.testType,
        "cells" -> st.cells.map(FacebookSplitTestCell.toDocument)
      )
    }

    def findById(id: ObjectId): Option[FacebookSplitTest] = {
      facebookSplitTestCollection.findOne(Document("_id" -> id)).map(fromDocument)
    }
  }

  case class FacebookSplitTestCell(
    cellId: Option[String],
    name: String,
    treatmentPercentage: Int,
    entityIds: List[String]
  )

  object FacebookSplitTestCell {

    def fromDocument(doc: Document): FacebookSplitTestCell = {
      FacebookSplitTestCell(
        cellId = Option(doc.getString("cellId")),
        name = doc.getString("name"),
        treatmentPercentage = doc.getInteger("treatmentPercentage"),
        entityIds = doc.getList("entityIds", classOf[String]).asScala.toList
      )
    }

    def toDocument(stc: FacebookSplitTestCell): Document = {
      Document(
        "cellId" -> stc.cellId,
        "name" -> stc.name,
        "treatmentPercentage" -> stc.treatmentPercentage,
        "entityIds" -> stc.entityIds
      )
    }
  }
}
