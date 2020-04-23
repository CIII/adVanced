package sync.facebook.process.api_account

import Shared.Shared._
import akka.actor.{Actor, Props}
import akka.event.Logging
import com.mongodb.casbah.Imports.{ObjectId, _}
import models.mongodb.facebook.Facebook._
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
        val api_account_data = dboToApiAccount(cache.changeData.asDBObject)
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

        facebookApiAccountCollection.findAndModify(
          DBObject("accountId" -> api_account.get.accountId),
          null,
          null,
          false,
          apiAccountToDBO(api_account.get),
          true,
          true
        )
        val facebookMarketingHelper = new FacebookMarketingHelper(
          accountId = api_account_data.accountId,
          applicationSecret = api_account_data.applicationSecret,
          accessToken = api_account_data.accessToken
        )
        set_subprocess_count(taskKey(Left(cache_msg.request.get)), cache, 1)
        facebookManagementActorSystem.actorOf(Props(new CampaignActor)) ! FacebookCampaignDataPullRequest(
          facebookMarketingHelper,
          ApiAccountObject(
            api_account.get._id.get,
            api_account.get
          ),
          recursivePull = true,
          pushToExternal = true
        )
        complete_subprocess(taskKey(Left(cache_msg.request.get)), cache)
      } catch {
        case e: Exception =>
          e.printStackTrace()
          log.info("Error Retrieving Data for API Account (%s) - %s".format(
            cache.id,
            e.getMessage
          ))
      } finally {
        context.stop(self)
      }
    case _ =>
      try {
        facebookApiAccountCollection.find().toArray.foreach {
          api_account_obj =>
            val api_account = dboToApiAccount(api_account_obj.asDBObject)
            val facebookMarketingHelper = new FacebookMarketingHelper(
              accountId = api_account.accountId,
              applicationSecret = api_account.applicationSecret,
              accessToken = api_account.accessToken
            )
            val campaignHelper = new CampaignHelper(facebookMarketingHelper, log)
            val campaigns = campaignHelper.getCampaigns()

            log.info("Campaigns to iterate over: " + campaigns.map(_.getFieldName).mkString(", "))

            facebookManagementActorSystem.actorOf(Props(new CampaignActor)) ! FacebookCampaignDataPullRequest(
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
          e.printStackTrace()
      } finally {
        context.stop(self)
      }
  }
}