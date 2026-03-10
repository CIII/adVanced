package sync.facebook.process.api_account

import Shared.Shared._
import org.apache.pekko.actor.{Actor, Props}
import org.apache.pekko.event.Logging
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import com.mongodb.client.model.{FindOneAndReplaceOptions, ReturnDocument}
import org.bson.types.ObjectId
import models.mongodb.facebook.Facebook._
import models.mongodb.MongoExtensions._
import sync.facebook.ads.FacebookMarketingHelper
import sync.facebook.ads.user._
import sync.facebook.process.api_account.campaign.CampaignActor
import sync.shared.Facebook._

class ApiAccountActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case cache_msg: PendingCacheMessage =>
      val cache: PendingCacheStructure = cache_msg.cache.get
      try {
        val api_account_data = documentToApiAccount(cache.changeData)
        log.info("Processing API Account %s -> %s -> %s -> %s".format(
          cache.changeCategory,
          cache.changeType,
          cache.trafficSource,
          cache.id
        ))

        val api_account = Some(models.mongodb.facebook.Facebook.FacebookApiAccount(
          Some(new ObjectId),
          api_account_data.accountId,
          api_account_data.applicationSecret,
          api_account_data.accessToken
        ))

        facebookApiAccountCollection.findOneAndReplaceSync(
          Document("accountId" -> api_account.get.accountId),
          apiAccountToDocument(api_account.get),
          new FindOneAndReplaceOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
        )
        val facebookMarketingHelper = new FacebookMarketingHelper(
          accountId = api_account_data.accountId,
          applicationSecret = api_account_data.applicationSecret,
          accessToken = api_account_data.accessToken
        )
        set_subprocess_count(taskKey(cache_msg.requestUsername.getOrElse("")), cache, 1)
        context.system.actorOf(Props(new CampaignActor)) ! FacebookCampaignDataPullRequest(
          facebookMarketingHelper,
          ApiAccountObject(
            api_account.get._id.get,
            api_account.get
          ),
          recursivePull = true,
          pushToExternal = true
        )
        complete_subprocess(taskKey(cache_msg.requestUsername.getOrElse("")), cache)
      } catch {
        case e: Exception =>
          log.error(s"Error processing Facebook API account: ${e.getMessage}")
          log.info("Error Retrieving Data for API Account (%s) - %s".format(
            cache.id,
            e.getMessage
          ))
      } finally {
        context.stop(self)
      }
    case _ =>
      try {
        facebookApiAccountCollection.find().toList.foreach {
          api_account_obj =>
            val api_account = documentToApiAccount(api_account_obj)
            val facebookMarketingHelper = new FacebookMarketingHelper(
              accountId = api_account.accountId,
              applicationSecret = api_account.applicationSecret,
              accessToken = api_account.accessToken
            )
            val campaignHelper = new CampaignHelper(facebookMarketingHelper, log)
            val campaigns = campaignHelper.getCampaigns()

            log.info("Campaigns to iterate over: " + campaigns.map(_.getFieldName).mkString(", "))

            context.system.actorOf(Props(new CampaignActor)) ! FacebookCampaignDataPullRequest(
              facebookMarketingHelper,
              ApiAccountObject(
                api_account._id.get,
                api_account
              ),
              recursivePull = true,
              pushToExternal = true
            )
        }
      } catch {
        case e: Exception =>
          log.info("Error Retrieving Incremental Data for API Account - %s".format(
            e.toString
          ))
          log.error(s"Error processing Facebook API account: ${e.getMessage}")
      } finally {
        context.stop(self)
      }
  }
}