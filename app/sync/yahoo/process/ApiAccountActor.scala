package sync.yahoo.process

import Shared.Shared._
import akka.actor.{Actor, Props}
import akka.event.Logging
import com.mongodb.casbah.Imports._
import models.mongodb.yahoo.Yahoo._
import org.bson.types.ObjectId
import sync.shared.Yahoo._
import sync.yahoo.gemini.GeminiHelper
import sync.yahoo.gemini.advertiser.AdvertiserHelper
import sync.yahoo.process.advertiser.AdvertiserActor

class ApiAccountActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case cache_msg: PendingCacheMessage =>
      val cache: PendingCacheStructure = cache_msg.cache.get
      try {
        val api_account_data = dboToApiAccount(cache.changeData.asDBObject)
        log.info("Processing ApiAccount %s -> %s -> %s -> %s".format(
          cache.changeCategory,
          cache.changeType,
          cache.trafficSource,
          cache.id
        ))

        var apiAccount: Option[models.mongodb.yahoo.Yahoo.ApiAccount] = None
        yahooApiAccountCollection.findOne(DBObject("refreshToken" -> api_account_data.refreshToken)) match {
          case Some(existingMcc) =>
            apiAccount = Some(models.mongodb.yahoo.Yahoo.ApiAccount(
              existingMcc._id,
              api_account_data.name,
              api_account_data.clientId,
              api_account_data.clientSecret,
              api_account_data.refreshToken
            ))
          case None =>
            apiAccount = Some(models.mongodb.yahoo.Yahoo.ApiAccount(
              Some(new ObjectId),
              api_account_data.name,
              api_account_data.clientId,
              api_account_data.clientSecret,
              api_account_data.refreshToken
            ))
        }

        yahooApiAccountCollection.update(DBObject("refreshToken" -> apiAccount.get.refreshToken), apiAccountToDBObject(apiAccount.get), upsert=true)

        val geminiHelper = new GeminiHelper(
          client_id = api_account_data.clientId,
          client_secret = api_account_data.clientSecret,
          refresh_token = api_account_data.refreshToken,
          log = log
        )
        val advertiserHelper = new AdvertiserHelper(geminiHelper, log)
        val advertisers = advertiserHelper.getAdvertisers()

        set_subprocess_count(taskKey(Left(cache_msg.request.get)), cache, 1)

        log.info("Advertisers to iterate over: " + advertisers.map(_.advertiserName).mkString(", "))

        advertisers.foreach(adv =>
          yahooManagementActorSystem.actorOf(Props(new AdvertiserActor)) ! YahooAdvertiserDataPullRequest(
            None,
            AdvertiserObject(ApiAccountObject(apiAccount.get._id.get, apiAccount.get), None, adv),
            recursivePull = true,
            pushToExternal = true
          )
        )
        complete_subprocess(taskKey(Left(cache_msg.request.get)), cache)
      } catch {
        case e: Exception =>
          log.info("Error Retrieving Data for ApiAccount (%s) - %s".format(
            cache.id,
            e.getMessage
          ))
      } finally {
        context.stop(self)
      }
    case _ =>
      try {
        yahooApiAccountCollection.find.toArray.foreach {
          api_account_obj =>
            val api_account = dboToApiAccount(api_account_obj.asDBObject)
            val geminiHelper = new GeminiHelper(
              client_id = api_account.clientId,
              client_secret = api_account.clientSecret,
              refresh_token = api_account.refreshToken,
              log = log
            )
            val advertiserHelper = new AdvertiserHelper(geminiHelper, log)
            val advertisers = advertiserHelper.getAdvertisers()

            log.info("Advertisers to iterate over: " + advertisers.map(_.advertiserName).mkString(", "))

            advertisers.foreach(adv => yahooManagementActorSystem.actorOf(Props(new AdvertiserActor)) ! YahooAdvertiserDataPullRequest(
              None,
              AdvertiserObject(ApiAccountObject(api_account._id.get, api_account), None, adv),
              recursivePull = true,
              pushToExternal = true
            ))
        }
      } catch {
        case e: Exception =>
          log.info("Error Retrieving Incremental Data for ApiAccount - %s".format(
            e.toString
          ))
          e.printStackTrace()
      } finally {
        context.stop(self)
      }
  }
}