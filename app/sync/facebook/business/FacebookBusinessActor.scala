package sync.facebook.business

import org.apache.pekko.actor.{Actor, Props}
import Shared.Shared._
import models.mongodb.MongoExtensions._
import models.mongodb.facebook.Facebook._
import org.apache.pekko.event.Logging
import sync.shared.Facebook._
import sync.facebook.business.ad_study._

class FacebookBusinessActor extends Actor{
  val log = Logging(context.system, this)
  
  def receive = {
    case cache_msg: PendingCacheMessage =>
      // TODO: CRUD for Business Accounts
      
    case _ =>
      try{
        log.debug("Processing Facebook business accounts")
        
        // Iterate over business accounts and sync information for each.
        facebookBusinessAccountCollection.find().toArray.foreach{
          bizAccountObj =>
            val fbBizAccount = FacebookBusinessAccount.fromDocument(bizAccountObj)
            val fbAdHelper = new FacebookAdStudyHelper(fbBizAccount, log)
            
            // Pull Split Test data
            fbAdHelper.getSplitTests.foreach(
              splitTest => context.system.actorOf(Props(new FacebookSplitTestActor)) ! FacebookSplitTestDataPullRequest(
                splitTest,
                fbAdHelper,
                false
              )
            )
        }
        
      } catch {
        case e: Exception =>
          log.debug("Error retrieving incremental data for Facebook Business account: %s".format(
              e.toString
          ))
          log.error(s"Error retrieving Facebook Business account data: ${e.getMessage}")
      } finally {
        context.stop(self)
      }
  }
}