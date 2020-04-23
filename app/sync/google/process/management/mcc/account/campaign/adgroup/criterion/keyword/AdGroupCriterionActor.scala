package sync.google.process.management.mcc.account.campaign.adgroup.criterion.keyword

import Shared.Shared._
import akka.actor.Actor
import akka.event.Logging
import com.google.api.ads.adwords.axis.utils.v201609.SelectorBuilder
import com.google.api.ads.adwords.axis.v201609.cm._
import com.google.api.ads.adwords.lib.selectorfields.v201609.cm.AdGroupCriterionField
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import helpers.google.mcc.account.campaign.adgroup.criterion.KeywordControllerHelper._
import models.mongodb.google.Google._
import org.bson.types.ObjectId
import sync.google.adwords.AdWordsHelper
import sync.google.adwords.account.AdGroupCriterionHelper
import sync.shared.Google.{GoogleAdGroupCriterionDataPullRequest, _}

import scala.collection.mutable.ListBuffer

class AdGroupCriterionActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
      case adGroupCriterionDataPullRequest: GoogleAdGroupCriterionDataPullRequest =>
        try {
          log.info("Retrieving AdGroupCriterion Data (%s)".format(
            adGroupCriterionDataPullRequest.adGroupCriterionObject.adGroupCriterion.getCriterion.getId
          ))
          googleCriterionCollection.update(
            DBObject(
              "mccObjId" -> adGroupCriterionDataPullRequest.adGroupCriterionObject.adGroupObject.campaignObject.customerObject.mccObject.mccObjId,
              "customerObjId" -> adGroupCriterionDataPullRequest.adGroupCriterionObject.adGroupObject.campaignObject.customerObject.customerObjId,
              "campaignObjId" -> adGroupCriterionDataPullRequest.adGroupCriterionObject.adGroupObject.campaignObject.campaignObjId,
              "adGroupObjId" -> adGroupCriterionDataPullRequest.adGroupCriterionObject.adGroupObject.adGroupObjId,
              "apiId" -> adGroupCriterionDataPullRequest.adGroupCriterionObject.adGroupCriterion.getCriterion.getId,
              "criterionType" -> "adGroupCriterion"
            ),
           $set(
             "mccObjId" -> adGroupCriterionDataPullRequest.adGroupCriterionObject.adGroupObject.campaignObject.customerObject.mccObject.mccObjId,
             "customerObjId" -> adGroupCriterionDataPullRequest.adGroupCriterionObject.adGroupObject.campaignObject.customerObject.customerObjId,
             "campaignObjId" -> adGroupCriterionDataPullRequest.adGroupCriterionObject.adGroupObject.campaignObject.campaignObjId,
             "adGroupObjId" -> adGroupCriterionDataPullRequest.adGroupCriterionObject.adGroupObject.adGroupObjId,
             "apiId" -> adGroupCriterionDataPullRequest.adGroupCriterionObject.adGroupCriterion.getCriterion.getId,
             "criterionType" -> "adGroupCriterion",
             "criterion" -> DBObject(
                "classPath" -> adGroupCriterionDataPullRequest.adGroupCriterionObject.adGroupCriterion.getClass.getCanonicalName,
                "object" -> JSON.parse(gson.toJson(adGroupCriterionDataPullRequest.adGroupCriterionObject.adGroupCriterion)).asInstanceOf[DBObject]
              )
           ),
            true
          )
        } catch {
          case e: Exception =>
            log.info("Error Retrieving Data for Google AdGroup Criterion (%s) - %s".format(
              adGroupCriterionDataPullRequest.adGroupCriterionObject.adGroupCriterion.getCriterion.getId,
              e.getMessage
            ))
            e.printStackTrace()
        } finally {
          context.stop(self)
        }
    case cache: PendingCacheStructure =>
      try {
        val keyword_change = dboToAdGroupKeywordForm(cache.changeData.asDBObject)
        log.info("Processing Pending Cache for Keyword %s -> %s -> %s -> %s".format(
          cache.changeCategory,
          cache.changeType,
          cache.trafficSource,
          cache.id
        ))

        val rs = googleMccCollection.findOne(
          DBObject("_id" -> new ObjectId(keyword_change.parent.mccObjId.get))
        ).get

        val account_data = gson.fromJson(
          googleCustomerCollection.findOne(
            DBObject(
              "mccObjId" -> new ObjectId(keyword_change.parent.mccObjId.get),
              "apiId" -> keyword_change.parent.customerApiId
            )
          ).get.getAs[String]("customer").get,
          classOf[com.google.api.ads.adwords.axis.v201609.mcm.Customer]
        )

        val mcc_data = dboToMcc(
          googleMccCollection.findOne(
            DBObject("_id" -> new ObjectId(keyword_change.parent.mccObjId.get))
          ).get
        )

        val adWordsHelper = new AdWordsHelper(
          clientId=mcc_data.oAuthClientId,
          clientSecret=mcc_data.oAuthClientSecret,
          refreshToken=mcc_data.oAuthRefreshToken,
          developerToken=mcc_data.developerToken,
          customerId=Some(account_data.getCustomerId.toString)
        )

        val adGroupCriterionHelper = new AdGroupCriterionHelper(adWordsHelper, log)

        var keywords = ListBuffer[AdGroupCriterion]()

        if (cache.changeType != ChangeType.NEW) {
          keywords = keywords ++ adGroupCriterionHelper
            .getKeywords(
              List(AdGroupCriterionField.Id, AdGroupCriterionField.KeywordText),
              0,
              None,
              Some(
                (new SelectorBuilder)
                  .equals(AdGroupCriterionField.Id, keyword_change.apiId.getOrElse("").toString)
                  .build
              )
            ).getEntries.toList
        }

        cache.changeType match {
          case ChangeType.DELETE =>
            keywords.map(keyword => adGroupCriterionHelper.deleteAdGroupCriterion(keyword))
          case ChangeType.NEW =>
            val adgroupcriterion: AdGroupCriterion = CriterionUse.fromString(keyword_change.criterionUse) match {
              case CriterionUse.BIDDABLE =>
                new BiddableAdGroupCriterion
              case CriterionUse.NEGATIVE =>
                new NegativeAdGroupCriterion
              case _ =>
                new AdGroupCriterion
            }
            adgroupcriterion.setAdGroupId(keyword_change.parent.adGroupApiId.get.toLong)
            val keyword = new Keyword
            keyword.setMatchType(KeywordMatchType.fromString(keyword_change.matchType))
            keyword.setText(keyword_change.text)
            CriterionUse.fromString(keyword_change.criterionUse) match {
              case CriterionUse.BIDDABLE =>
                keyword.asInstanceOf[BiddableAdGroupCriterion].setDestinationUrl(keyword_change.destinationUrl.getOrElse(""))
                keyword.asInstanceOf[BiddableAdGroupCriterion].setBidModifier(keyword_change.bidModifier.getOrElse(0.0).asInstanceOf[Double])
            }
            adgroupcriterion.setCriterion(keyword)
            adgroupcriterion.setCriterionUse(CriterionUse.fromString(keyword_change.criterionUse))
            adGroupCriterionHelper.createAdGroupCriterion(adgroupcriterion)
          case ChangeType.UPDATE =>
            keywords.foreach{keyword =>
              keyword.getCriterion.asInstanceOf[Keyword].setMatchType(KeywordMatchType.fromString(keyword_change.matchType))
              keyword.getCriterion.asInstanceOf[Keyword].setText(keyword_change.text)
              CriterionUse.fromString(keyword_change.criterionUse) match {
                case CriterionUse.BIDDABLE =>
                  keyword.asInstanceOf[BiddableAdGroupCriterion].setDestinationUrl(keyword_change.destinationUrl.getOrElse(""))
                  keyword.asInstanceOf[BiddableAdGroupCriterion].setBidModifier(keyword_change.bidModifier.getOrElse(0.0).asInstanceOf[Double])
              }
              adGroupCriterionHelper.updateAdGroupCriterion(keyword)
            }
        }
      } catch {
        case e: Exception => e.printStackTrace
      } finally {
        context.stop(self)
      }
  }
}