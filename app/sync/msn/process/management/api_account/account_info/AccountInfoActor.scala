package sync.msn.process.management.api_account.account_info

import Shared.Shared._
import org.apache.pekko.actor.Actor
import org.apache.pekko.event.Logging
import com.microsoft.bingads.v13.customermanagement._
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import com.mongodb.client.model.UpdateOptions
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

        val qry = Document(
          "apiAccountObjId" -> accountDataPullRequest.apiAccountObjId.toString,
          "apiId" -> accountDataPullRequest.account.getId.longValue()
        )

        val mapper = new com.fasterxml.jackson.databind.ObjectMapper()
        val newData = Document(
          "object" -> Document(
            "accountObj" -> Document(mapper.writeValueAsString(accountDataPullRequest.account)),
            "accountInfoWithCustomerData" -> Document(mapper.writeValueAsString(accountDataPullRequest.accountInfoWithCustomerData))
          )
        )
        msnAccountInfoCollection.updateOne(qry, Document("$set" -> Document("account" -> newData)), new UpdateOptions().upsert(true))

        //val accountHelper = new AccountHelper(accountDataPullRequest.bingAdsHelper)
        //val campaignHelper = new CampaignHelper(accountDataPullRequest.bingAdsHelper)

        //todo: FILTER UP TO OTHER BINGADS SYNC LOADERS
      } catch {
        case e: Exception =>
          log.error(s"Error processing MSN account info: ${e.getMessage}")
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