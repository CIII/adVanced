package sync.google.process.management.mcc.account.budget

import Shared.Shared._
import akka.actor.Actor
import akka.event.Logging
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import models.mongodb.google.Google._
import sync.google.adwords.account.CustomerHelper
import sync.shared.Google.{GoogleBudgetDataPullRequest, _}


class BudgetActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case budgetDataPullRequest: GoogleBudgetDataPullRequest =>
        try {
          log.info("Processing Incoming Data for Budget (%s)".format(
            budgetDataPullRequest.budgetObject.budget.getBudgetId
          ))

          googleBudgetCollection.update(
            DBObject(
              "mccObjId" -> budgetDataPullRequest.budgetObject.customerObject.mccObject.mccObjId,
              "customerObjId" -> budgetDataPullRequest.budgetObject.customerObject.customerObjId
            ),
            $set(
              "mccObjId" -> budgetDataPullRequest.budgetObject.customerObject.mccObject.mccObjId,
              "customerObjId" -> budgetDataPullRequest.budgetObject.customerObject.customerObjId,
              "budget" -> DBObject(
              "classPath" -> budgetDataPullRequest.budgetObject.budget.getClass.getCanonicalName,
              "object" -> JSON.parse(gson.toJson(budgetDataPullRequest.budgetObject.budget)).asInstanceOf[DBObject]
            )),
            true
          )
        } catch {
          case e: Exception =>
            log.info(s"Error Retrieving Data for Google Budget(${budgetDataPullRequest.budgetObject.budget.getName}) - ${e.getMessage}")
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