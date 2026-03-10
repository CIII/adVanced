package sync.google.process.management.mcc.account.budget

import Shared.Shared._
import org.apache.pekko.actor.Actor
import org.apache.pekko.event.Logging
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import com.mongodb.client.model.UpdateOptions
import models.mongodb.google.Google._
import sync.shared.Google.{GoogleBudgetDataPullRequest, _}


class BudgetActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case budgetDataPullRequest: GoogleBudgetDataPullRequest =>
        try {
          val budgetDoc = budgetDataPullRequest.budgetObject.budgetDoc
          log.info("Processing Incoming Data for Budget (%s)".format(
            budgetDoc.getString("budgetId")
          ))

          googleBudgetCollection.updateOne(
            Document(
              "mccObjId" -> budgetDataPullRequest.budgetObject.customerObject.mccObject.mccObjId,
              "customerObjId" -> budgetDataPullRequest.budgetObject.customerObject.customerObjId
            ),
            Document("$set" -> Document(
              "mccObjId" -> budgetDataPullRequest.budgetObject.customerObject.mccObject.mccObjId,
              "customerObjId" -> budgetDataPullRequest.budgetObject.customerObject.customerObjId,
              "budget" -> budgetDoc
            )),
            new UpdateOptions().upsert(true)
          )
        } catch {
          case e: Exception =>
            log.info(s"Error Retrieving Data for Google Budget - ${e.getMessage}")
        } finally {
          context.stop(self)
        }
    case cache: PendingCacheStructure =>
      log.info("Processing Budget %s -> %s -> %s -> %s".format(
        cache.changeCategory,
        cache.changeType,
        cache.trafficSource,
        cache.id
      ))
      context.stop(self)
  }
}
