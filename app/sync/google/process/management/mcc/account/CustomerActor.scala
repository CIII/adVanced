package sync.google.process.management.mcc.account

import Shared.Shared._
import akka.actor.{Actor, Props}
import akka.event.Logging
import com.google.api.ads.adwords.axis.v201609.mcm.ManagedCustomer
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import models.mongodb.google.Google._
import sync.google.adwords.AdWordsHelper
import sync.google.adwords.account._
import sync.google.process.management.mcc.account.bidding_strategy.BiddingStrategyActor
import sync.google.process.management.mcc.account.budget.BudgetActor
import sync.google.process.management.mcc.account.campaign.CampaignActor
import sync.shared.Google._

class CustomerActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case customerDataPullRequest: GoogleCustomerDataPullRequest =>
      try {
        log.info("Processing Incoming Data for Account (%s)".format(
          customerDataPullRequest.customerObject.managedCustomer.getCustomerId
        ))
        customerDataPullRequest.adWordsHelper = Some(
          new AdWordsHelper(
            customerDataPullRequest.customerObject.mccObject.mcc.oAuthClientId,
            customerDataPullRequest.customerObject.mccObject.mcc.oAuthClientSecret,
            customerDataPullRequest.customerObject.mccObject.mcc.oAuthRefreshToken,
            customerDataPullRequest.customerObject.mccObject.mcc.developerToken,
            Some(customerDataPullRequest.customerObject.managedCustomer.getCustomerId.toString),
            enablePartialFailure = false
          )
        )
        val cust = googleCustomerCollection.update(
          DBObject(
            "mccObjId" -> customerDataPullRequest.customerObject.mccObject.mccObjId,
            "customerId" -> customerDataPullRequest.customerObject.customer.get.getCustomerId,
            "apiId" -> customerDataPullRequest.customerObject.managedCustomer.getCustomerId
          ),
          $set(
            "mccObjId" -> customerDataPullRequest.customerObject.mccObject.mccObjId,
            "customerId" -> customerDataPullRequest.customerObject.customer.get.getCustomerId,
            "apiId" -> customerDataPullRequest.customerObject.managedCustomer.getCustomerId,
            "customer" -> DBObject(
              "classPath" -> classOf[ManagedCustomer].getCanonicalName,
              "object" -> JSON.parse(
                gson.toJson(customerDataPullRequest.customerObject.managedCustomer)
              ).asInstanceOf[DBObject]
            )
          ),
          true
        )
        if (customerDataPullRequest.recursivePull) {
          var offset = 0
          customerDataPullRequest.customerObject.customerObjId = Some(cust.getUpsertedId.asInstanceOf[ObjectId])
          val customerHelper = new CustomerHelper(customerDataPullRequest.adWordsHelper.get, log)
          val campaignHelper = new CampaignHelper(customerDataPullRequest.adWordsHelper.get, log)

          var budgets = customerHelper.getBudgets(budgetFields, offset)
          while (offset < budgets.getTotalNumEntries) {
            budgets.getEntries.foreach { budget =>
              googleManagementActorSystem.actorOf(Props(new BudgetActor)) ! GoogleBudgetDataPullRequest(
                customerDataPullRequest.adWordsHelper.get,
                BudgetObject(
                  customerDataPullRequest.customerObject,
                  None,
                  budget
                ),
                false
              )
            }
            offset += customerDataPullRequest.adWordsHelper.get.PAGE_SIZE
            budgets = customerHelper.getBudgets(budgetFields, offset)

          }

          offset = 0
          var biddingStrategies = customerHelper.getSharedBiddingStrategies(sharedBiddingStrategyFields)
          while (offset < biddingStrategies.getTotalNumEntries) {
            biddingStrategies.getEntries.foreach { strategy =>
              googleManagementActorSystem.actorOf(Props(new BiddingStrategyActor)) ! GoogleBiddingStrategyDataPullRequest(
                customerDataPullRequest.adWordsHelper.get,
                SharedBiddingStrategyObject(
                  customerDataPullRequest.customerObject,
                  None,
                  strategy
                ),
                false
              )
            }
            offset += customerDataPullRequest.adWordsHelper.get.PAGE_SIZE
            biddingStrategies = customerHelper.getSharedBiddingStrategies(sharedBiddingStrategyFields)
          }


          offset = 0
          var campaigns = campaignHelper.getCampaigns(campaignFields, offset)
          while (offset < campaigns.getTotalNumEntries) {
            campaigns.getEntries.foreach { campaign =>
              googleManagementActorSystem.actorOf(Props(new CampaignActor)) ! GoogleCampaignDataPullRequest(
                adWordsHelper = customerDataPullRequest.adWordsHelper.get,
                campaignObject = CampaignObject(
                  customerDataPullRequest.customerObject,
                  None,
                  campaign
                ),
                customerDataPullRequest.recursivePull,
                true
              )
            }
            offset += customerDataPullRequest.adWordsHelper.get.PAGE_SIZE
            campaigns = campaignHelper.getCampaigns(campaignFields, offset)
          }
        }
      } catch {
        case e: Exception =>
          log.info(s"Error Retrieving Data for Google Account (${customerDataPullRequest.customerObject.managedCustomer.getName}) - ${e.toString}")
          e.printStackTrace()
      }  finally {
      context.stop(self)
    }
  }
}