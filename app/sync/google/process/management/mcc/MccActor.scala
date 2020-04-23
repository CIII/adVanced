package sync.google.process.management.mcc

import Shared.Shared._
import akka.actor.{Actor, Props}
import akka.event.Logging
import com.mongodb.casbah.Imports.{ObjectId, _}
import models.mongodb.google.Google._
import sync.google.adwords.AdWordsHelper
import sync.google.adwords.account._
import sync.google.process.management.mcc.account.CustomerActor
import sync.shared.Google._

class MccActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case cache_msg: PendingCacheMessage =>
      val cache: PendingCacheStructure = cache_msg.cache.get
      try {
        val mcc_data = dboToMcc(cache.changeData.asDBObject)
        log.info("Processing MCC %s -> %s -> %s -> %s".format(
          cache.changeCategory,
          cache.changeType,
          cache.trafficSource,
          cache.id
        ))

        val mccObj = googleMccCollection.update(
          DBObject("oAuthRefreshToken" -> mcc_data.oAuthRefreshToken),
          mccToDBObject(mcc_data),
          true
        )

        val adWordsHelper = new AdWordsHelper(
          clientId = mcc_data.oAuthClientId,
          clientSecret = mcc_data.oAuthClientSecret,
          refreshToken = mcc_data.oAuthRefreshToken,
          developerToken = mcc_data.developerToken,
          customerId = None
        )

        val customerHelper = new CustomerHelper(adWordsHelper, log)
        customerHelper.getCustomers.foreach { customer =>
          try {
            var offset = 0
            var totalNumEntries = 0
            while (offset <= totalNumEntries) {
              val managedCustomerPage = customerHelper.getManagedCustomers(customer, managedCustomerFields, offset)
              totalNumEntries = managedCustomerPage.getTotalNumEntries
              managedCustomerPage.getEntries.foreach { managedCustomer =>
                googleManagementActorSystem.actorOf(Props(new CustomerActor)) ! GoogleCustomerDataPullRequest(
                  Some(adWordsHelper),
                  CustomerObject(MccObject(mccObj.getUpsertedId.asInstanceOf[ObjectId], mcc_data), None, managedCustomer, Some(customer)),
                  recursivePull = true,
                  pushToExternal = false
                )
              }
              offset += adWordsHelper.PAGE_SIZE
            }
          } catch {
            case e: Exception =>
              e.printStackTrace()
          }
        }
        complete_subprocess(taskKey(Left(cache_msg.request.get)), cache)
      } catch {
        case e: Exception => log.info("Error Retrieving Data for MCC (%s) - %s".format(cache.id, e.getMessage))
      } finally {
        context.stop(self)
      }
    case _ =>
      try {
        googleMccCollection.find().toSeq.foreach {
          mcc_obj =>
            val mcc = dboToMcc(mcc_obj.asDBObject)
            val adWordsHelper = new AdWordsHelper(
              clientId = mcc.oAuthClientId,
              clientSecret = mcc.oAuthClientSecret,
              refreshToken = mcc.oAuthRefreshToken,
              developerToken = mcc.developerToken,
              customerId = None
            )

            val customerHelper = new CustomerHelper(adWordsHelper, log)
            customerHelper.getCustomers.foreach { customer =>
              try {
                var offset = 0
                var totalNumEntries = 0
                while (offset <= totalNumEntries) {
                  val managedCustomerPage = customerHelper.getManagedCustomers(customer, managedCustomerFields, offset)
                  totalNumEntries = managedCustomerPage.getTotalNumEntries
                  managedCustomerPage.getEntries.foreach { managedCustomer =>
                    googleManagementActorSystem.actorOf(Props(new CustomerActor)) ! GoogleCustomerDataPullRequest(
                      Some(adWordsHelper),
                      CustomerObject(MccObject(mcc._id.get, mcc), None, managedCustomer, Some(customer)),
                      recursivePull = true,
                      pushToExternal = true
                    )
                  }
                  offset += adWordsHelper.PAGE_SIZE
                }
              } catch {
                case e: Exception =>
                  log.info(s"CLARENCE DEBUG -- MCC EXCEPTION -> ${e.toString}")
                  e.printStackTrace()
              }
            }
        }
      } catch {
        case e: Exception =>
          log.info("Error Retrieving Incremental Data for MCC - %s".format(
            e.toString
          ))
          e.printStackTrace()
      } finally {
        context.stop(self)
      }
  }
}