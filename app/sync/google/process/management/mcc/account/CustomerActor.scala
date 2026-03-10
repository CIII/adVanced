package sync.google.process.management.mcc.account

import Shared.Shared._
import org.apache.pekko.actor.{Actor, Props}
import org.apache.pekko.event.Logging
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import org.bson.types.ObjectId
import models.mongodb.google.Google._
import models.mongodb.MongoExtensions._
import sync.shared.Google._

class CustomerActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case customerDataPullRequest: GoogleCustomerDataPullRequest =>
      try {
        val customerDoc = customerDataPullRequest.customerObject.customerDoc
        val customerId = customerDoc.map(_.getString("customerId")).getOrElse("unknown")
        log.info("Processing Incoming Data for Account (%s)".format(customerId))

        // TODO: Not yet migrated to Google Ads API v18
        // The old AdWords API v201609 AdWordsHelper, CustomerHelper, CampaignHelper are no longer available.
        // Implement using Google Ads API v18 CustomerService and CampaignService.
        log.info("Not yet migrated to Google Ads API v18 - customer data pull is stubbed out")

      } catch {
        case e: Exception =>
          log.info(s"Error Retrieving Data for Google Account - ${e.toString}")
          log.error(s"Error processing Google customer account: ${e.getMessage}")
      } finally {
        context.stop(self)
      }
  }
}
