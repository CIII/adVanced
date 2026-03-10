package sync.facebook.business.ad_study

import org.apache.pekko.actor.Actor
import org.apache.pekko.event.Logging
import Shared.Shared._
import models.mongodb.MongoExtensions._
import models.mongodb.facebook.Facebook._
import models.mongodb.facebook.Facebook.FacebookSplitTest._
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import com.mongodb.client.model.ReplaceOptions
import javax.inject.Inject
import sync.shared.Facebook._

class FacebookSplitTestActor extends Actor {
  val log = Logging(context.system, this)
  
  def receive = {
    case fbSplitTestPullReq: FacebookSplitTestDataPullRequest =>
      val splitTestRaw = fbSplitTestPullReq.fbSplitTest
      val helper = fbSplitTestPullReq.fbBusinessHelper

      log.debug("Processing split test: %s - %s".format(splitTestRaw.name, splitTestRaw.adStudyId))
      log.debug(splitTestRaw.toString)
      // Check if split test already exists.  if so get mongodb id so we can update it.  If it's
      // new then a new objectId would be set on creation of the object.
      val splitTest = facebookSplitTestCollection.findOne(Document("adStudyId" -> splitTestRaw.adStudyId)) match {
        case Some(existingSplitObj) =>
          splitTestRaw.copy(_id = Option(existingSplitObj.getObjectId("_id")))
        case None =>
          // Nothing to do - Split Test is new and should have it's object Id populated on creation
          splitTestRaw
      }

      // Update or insert split test.
      facebookSplitTestCollection.replaceOne(
          Document("_id" -> splitTest._id),
          toDocument(splitTest),
          new ReplaceOptions().upsert(true)
      )
      
    case cacheMsg: PendingCacheMessage =>
      val cache = cacheMsg.cache.get
      val splitTest = fromDocument(cache.changeData)
      log.info(splitTest.toString)
      val fbBizAccount = FacebookBusinessAccount.fromDocument(facebookBusinessAccountCollection.findOne(Document("_id" -> splitTest.bizObjectId)).get)
      val fbAdStudyHelper = new FacebookAdStudyHelper(fbBizAccount, log)
      
      log.debug("Processing split test: %s - %s".format(splitTest.name, splitTest.adStudyId))
      
      cache.changeType match {
        case ChangeType.NEW =>
          fbAdStudyHelper.createSplitTest(splitTest)
        case ChangeType.UPDATE =>
          fbAdStudyHelper.updateSplitTest(splitTest)
        case ChangeType.DELETE =>
          fbAdStudyHelper.deleteSplitTest(splitTest)
      }
      
  }
}