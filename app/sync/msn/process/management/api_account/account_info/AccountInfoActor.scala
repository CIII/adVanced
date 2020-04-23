package sync.msn.process.management.api_account.account_info

import Shared.Shared._
import akka.actor.Actor
import akka.event.Logging
import com.microsoft.bingads.customermanagement._
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import models.mongodb.msn.Msn._
import sync.shared.Msn.MsnAccountInfoDataPullRequest

class AccountInfoActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case accountDataPullRequest: MsnAccountInfoDataPullRequest =>
      try {
        log.info("Processing Incoming Data for Account (%s)".format(
          accountDataPullRequest.account.getId
        ))

        val qry = DBObject(
          "apiAccountObjId" -> accountDataPullRequest.apiAccountObjId,
          "apiId" -> accountDataPullRequest.account.getId
        )

        val newData = DBObject(
          "object" -> DBObject(
            "accountObj" -> JSON.parse(gson.toJson(accountDataPullRequest.account)),
            "accountInfoWithCustomerData" -> JSON.parse(gson.toJson(accountDataPullRequest.accountInfoWithCustomerData))
          )
        )
        msnAccountInfoCollection.update(qry, DBObject("$set" -> DBObject("account" -> newData)), upsert = true)

        //val accountHelper = new AccountHelper(accountDataPullRequest.bingAdsHelper)
        //val campaignHelper = new CampaignHelper(accountDataPullRequest.bingAdsHelper)

        //todo: FILTER UP TO OTHER BINGADS SYNC LOADERS
      } catch {
        case e: Exception =>
          e.printStackTrace()
          log.info("Error Retrieving Data for Msn Account Info (%s) - %s".format(
            accountDataPullRequest.accountInfoWithCustomerData.getAccountName,
            e.toString
          ))
      } finally {
        context.stop(self)
      }
    case _ =>
  }
}