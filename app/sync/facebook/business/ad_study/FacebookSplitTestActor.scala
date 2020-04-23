package sync.facebook.business.ad_study

import akka.actor.Actor
import akka.event.Logging
import Shared.Shared._
import models.mongodb.facebook.Facebook._
import models.mongodb.facebook.Facebook.FacebookSplitTest._
import com.mongodb.casbah.Imports._
import javax.inject.Inject
import sync.shared.Facebook._

class FacebookSplitTestActor extends Actor {
  val log = Logging(context.system, this)
  
  def receive = {
    case fbSplitTestPullReq: FacebookSplitTestDataPullRequest =>
      val splitTest = fbSplitTestPullReq.fbSplitTest
      val helper = fbSplitTestPullReq.fbBusinessHelper
      
      log.debug("Processing split test: %s - %s".format(splitTest.name, splitTest.adStudyId))
      log.debug(splitTest.toString)
      // Check if split test already exists.  if so get mongodb id so we can update it.  If it's
      // new then a new objectId would be set on creation of the object.
      facebookSplitTestCollection.findOne(DBObject("adStudyId" -> splitTest.adStudyId)) match {
        case Some(existingSplitObj) =>
          splitTest._id = existingSplitObj._id
        case None =>
          // Nothing to do - Split Test is new and should have it's object Id populated on creation
      }
      
      // Update or insert split test.
      facebookSplitTestCollection.update(
          DBObject("_id" -> splitTest._id),
          toDBO(splitTest),
          upsert=true
      )
      
    case cacheMsg: PendingCacheMessage =>
      val cache = cacheMsg.cache.get
      val splitTest = fromDBO(cache.changeData.asDBObject)
      log.info(splitTest.toString)
      val fbBizAccount = FacebookBusinessAccount.fromDBO(facebookBusinessAccountCollection.findOne(DBObject("_id" -> splitTest.bizObjectId)).get) 
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