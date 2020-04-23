package sync.msn.process.management.api_account

import Shared.Shared._
import akka.actor.{Actor, Props}
import akka.event.Logging
import com.mongodb.casbah.Imports._
import models.mongodb.msn.Msn._
import org.bson.types.ObjectId
import sync.msn.bingads.BingAdsHelper
import sync.msn.bingads.account.AccountHelper
import sync.msn.process.management.api_account.account_info.AccountInfoActor
import sync.shared.Msn._

class ApiAccountActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case cache_msg: PendingCacheMessage =>
      try {
        val cache: PendingCacheStructure = cache_msg.cache.get
        try {
          log.info("Processing API Account %s -> %s -> %s -> %s".format(
            cache.changeCategory,
            cache.changeType,
            cache.trafficSource,
            cache.id
          ))

          val api_account_data = dboToApiAccount(cache.changeData.asDBObject)

          var api_account: Option[models.mongodb.msn.Msn.ApiAccount] = None
          msnApiAccountCollection.findOne(DBObject("developerToken" -> api_account_data.developerToken)) match {
            case Some(existingApiAccount) =>
              api_account = Some(models.mongodb.msn.Msn.ApiAccount(
                existingApiAccount._id,
                api_account_data.name,
                api_account_data.userName,
                api_account_data.password,
                api_account_data.developerToken
              ))
            case None =>
              api_account = Some(models.mongodb.msn.Msn.ApiAccount(
                Some(new ObjectId),
                api_account_data.name,
                api_account_data.userName,
                api_account_data.password,
                api_account_data.developerToken
              ))
          }

          msnApiAccountCollection.update(DBObject("developerToken" -> api_account.get.developerToken), apiAccountToDbo(api_account.get), upsert = true)

          val bingAdsHelper = new BingAdsHelper(api_account.get.userName, api_account.get.password, api_account.get.developerToken)
          val accountHelper = new AccountHelper(bingAdsHelper)

          val data = accountHelper.findAccountsOrCustomersInfo().getAccountInfoWithCustomerDatas

          for (i <- 0 until data.size()) {
            msnManagementActorSystem.actorOf(Props(new AccountInfoActor)) ! MsnAccountInfoDataPullRequest(
              bingAdsHelper,
              api_account.get._id.get,
              accountHelper.getAccount(data.get(i).getAccountId).get,
              data.get(i),
              false
            )
          }
        } catch {
          case e: Exception =>
            e.printStackTrace()
            log.info("Error Retrieving Data for ApiAccount - %s".format(
              e.toString
            ))
        }
      } finally {
        context.stop(self)
      }
  }
}