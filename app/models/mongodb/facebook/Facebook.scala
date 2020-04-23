package models.mongodb.facebook

import Shared.Shared._
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import org.bson.types.ObjectId
import org.joda.time.DateTime
import scala.reflect.{ClassTag, classTag}
import com.google.gson.annotations.SerializedName

object Facebook {
  def facebookBusinessAccountCollection = advancedCollection("facebook_business_account")
  def facebookApiAccountCollection = advancedCollection("facebook_api_account")
  def facebookCampaignCollection = advancedCollection("facebook_campaign")
  def facebookAdSetCollection = advancedCollection("facebook_ad_set")
  def facebookAdCollection = advancedCollection("facebook_ad")
  def facebookSplitTestCollection = advancedCollection("facebook_split_test")
  def facebookReportCollection(reportType: String) = advancedCollection(
    "facebook_%s_report".format(reportType.toString.toLowerCase)
  )
  
  case class FacebookApiAccount(
    _id: Option[ObjectId],
    accountId: String,
    applicationSecret: String,
    accessToken: String
  )

  def dboToFacebookEntity[T: ClassTag](dbo: DBObject, listKey: String, objectKey: Option[String]): T = {
    gson.fromJson(
      JSON.serialize(
        objectKey match {
          case Some(key) =>
            dbo.as[MongoDBList](listKey).asInstanceOf[DBObject].as[DBObject]("object").expand[DBObject](key)
          case None =>
            dbo.as[MongoDBList](listKey).asInstanceOf[DBObject].as[DBObject]("object")
        }
      ),
      classTag[T].runtimeClass.asInstanceOf[Class[T]]
    )
  }

  def apiAccountToDBO(faa: FacebookApiAccount): DBObject = {
    DBObject(
      "_id" -> faa._id.getOrElse(new ObjectId),
      "accountId" -> faa.accountId,
      "applicationSecret" -> faa.applicationSecret,
      "accessToken" -> faa.accessToken
    )
  }

  def dboToApiAccount(dbo: DBObject): FacebookApiAccount = {
    FacebookApiAccount(
      _id = dbo._id,
      accountId = dbo.as[String]("accountId"),
      applicationSecret = dbo.as[String]("applicationSecret"),
      accessToken = dbo.as[String]("accessToken")
    )
  }
  
  case class FacebookBusinessAccount(
    _id: Option[ObjectId],
    accountNumber: String,     // Business account Id.
    accessToken: String    // Access token for the system user associated with the business account.
  )
  
  object FacebookBusinessAccount{
    
    def findByAccountNumber(accountNumber: String): Option[FacebookBusinessAccount] = {
      facebookBusinessAccountCollection.findOne(DBObject("accountNumber" -> accountNumber)) match {
        case Some(accountObj) => Some(fromDBO(accountObj))
        case None => None
      }
    }
    
    def findById(id: ObjectId): Option[FacebookBusinessAccount] = {
      facebookBusinessAccountCollection.findOne(DBObject("_id" -> id)) match {
        case Some(accountObj) => Some(fromDBO(accountObj))
        case None => None
      }
    }
  
    def toDBO(fbBizAccount: FacebookBusinessAccount): DBObject = {
      DBObject(
        "_id" -> fbBizAccount._id,
        "accountNumber" -> fbBizAccount.accountNumber,
        "accessToken" -> fbBizAccount.accessToken
      )
    }
    
    def fromDBO(dbo: DBObject): FacebookBusinessAccount = {
      FacebookBusinessAccount(
        _id = dbo._id,
        accountNumber = dbo.as[String]("accountNumber"),
        accessToken = dbo.as[String]("accessToken")
      )
    }
  }
  
  case class FacebookSplitTest(
    var _id: Option[ObjectId],
    var bizObjectId: Option[ObjectId],
    var adStudyId: Option[String],
    var name: String,
    var description: String,
    var startTime: DateTime,
    var endTime: DateTime,
    var testType: String,
    var cells: List[FacebookSplitTestCell]
  )
  
  object FacebookSplitTest{
    
    def fromDBO(dbo: DBObject): FacebookSplitTest = {
      FacebookSplitTest(
        _id = dbo._id,
        bizObjectId = Some(dbo.as[ObjectId]("bizObjectId")),
        adStudyId = Some(dbo.as[String]("adStudyId")),
        name = dbo.as[String]("name"),
        description = dbo.as[String]("description"),
        startTime = DateTime.parse(dbo.as[String]("startTime")),
        endTime = DateTime.parse(dbo.as[String]("endTime")),
        testType = dbo.as[String]("testType"),
        cells = dbo.as[List[DBObject]]("cells").map(FacebookSplitTestCell.fromDBO)
      )
    }
    
    def toDBO(st: FacebookSplitTest): DBObject = {
      DBObject(
        "_id" -> st._id,
        "bizObjectId" -> st.bizObjectId,
        "name" -> st.name,
        "adStudyId" -> st.adStudyId,
        "description" -> st.description,
        "startTime" -> st.startTime.toString,
        "endTime" -> st.endTime.toString,
        "testType" -> st.testType,
        "cells" -> st.cells.map(FacebookSplitTestCell.toDBO)
      )
    }
    
    def findById(id: ObjectId): Option[FacebookSplitTest] = {
      facebookSplitTestCollection.findOne(DBObject("_id" -> id)) match {
        case Some(obj) => Some(fromDBO(obj))
        case None => None
      }
    }
  }
  
  case class FacebookSplitTestCell(
    var cellId: Option[String],
    var name: String,
    var treatmentPercentage: Int,
    var entityIds: List[String]
  )
  
  object FacebookSplitTestCell {
    
    def fromDBO(dbo: DBObject): FacebookSplitTestCell = {
      FacebookSplitTestCell(
        cellId = Some(dbo.as[String]("name")),
        name = dbo.as[String]("name"),
        treatmentPercentage = dbo.as[Integer]("treatmentPercentage"),
        entityIds = dbo.as[List[String]]("entityIds")
      )
    }
    
    def toDBO(stc: FacebookSplitTestCell): DBObject = {
      DBObject(
        "cellId" -> stc.cellId,
        "name" -> stc.name,
        "treatmentPercentage" -> stc.treatmentPercentage,
        "entityIds" ->stc.entityIds
      )
    }
  }
}
