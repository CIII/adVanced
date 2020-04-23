package sync.yahoo.process.advertiser

import Shared.Shared._
import akka.actor.Actor
import akka.event.Logging
import com.mongodb.casbah.Imports._
import models.mongodb.yahoo.Yahoo._
import sync.shared.Yahoo.YahooAdvertiserDataPullRequest

class AdvertiserActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case advertiserDataPullRequest: YahooAdvertiserDataPullRequest =>
      try {
        log.info("Processing Incoming Data for Yahoo Advertiser (%s)".format(
          advertiserDataPullRequest.advertiserObject.advertiser.advertiserName
        ))

        val qry = DBObject(
          "apiAccountObjId" -> advertiserDataPullRequest.advertiserObject.apiAccountObject.apiAccountObjId,
          "apiId" -> advertiserDataPullRequest.advertiserObject.advertiser.apiId
        )

        val startTsecs = java.lang.System.currentTimeMillis() / 1000
        val endTsecs = java.lang.System.currentTimeMillis() / 1000

        val newData = DBObject(
          "startTsecs" -> startTsecs,
          "endTsecs" -> -1,
          "object" -> advertiserToDBObject(advertiserDataPullRequest.advertiserObject.advertiser)
        )

        var matchFound = true

        yahooAdvertiserCollection.findOne(
          qry ++ ("advertiser.endTsecs" -> -1)
        ) match {
          case Some(advertiserRs) =>
            if (
              !gson.toJson(advertiserRs.as[String]("advertiser"))
                .equals(gson.toJson(advertiserDataPullRequest.advertiserObject.advertiser))
            ) {
              yahooAdvertiserCollection.update(
                qry ++ ("advertiser.endTsecs" -> -1),
                DBObject("$set" -> DBObject("advertiser.0.endTsecs" -> endTsecs))
              )
              matchFound = false
              log.debug("Yahoo Advertiser match found. Changes detected. Updating...")
            }
          case _ =>
            matchFound = false
            log.debug("No Yahoo Advertiser record Found. Inserting...")
        }
        if(!matchFound) {
          yahooAdvertiserCollection.update(qry, DBObject("$push" -> DBObject("advertiser" -> newData)), upsert = true)

          if (advertiserDataPullRequest.pushToExternal) {
            //yahooAdvertiserSyncActor ! (advertiserDataPullRequest, endTsecs)
          }
        }

        if(advertiserDataPullRequest.recursivePull) {
          //todo: recursive pull
        }
      } catch {
        case e: Exception =>
          log.info("Error Retrieving Data for Google Account (%s) - %s".format(
            advertiserDataPullRequest.advertiserObject.advertiser.advertiserName,
            e.toString
          ))
          e.printStackTrace()
      } finally {
        context.stop(self)
      }
  }
}