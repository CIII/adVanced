package sync.yahoo.process.advertiser.campaign.adgroup

import akka.actor.Actor
import akka.event.Logging


class AdGroupActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case _ =>
    /*case adGroupDataPullRequest: YahooAdGroupDataPullRequest =>
      try {
        log.info("Retrieving AdGroup Data (%s)".format(
          adGroupDataPullRequest.adGroupObject.adGroup.apiId
        ))

        val qry = DBObject(
          "mccObjId" -> adGroupDataPullRequest.adGroupObject.campaignObject.customerObject.mccObject.mccObjId,
          "customerObjId" -> adGroupDataPullRequest.adGroupObject.campaignObject.customerObject.customerObjId,
          "campaignObjId" -> adGroupDataPullRequest.adGroupObject.campaignObject.campaignObjId.get,
          "apiId" -> adGroupDataPullRequest.adGroupObject.adGroup.getId
        )

        val startTsecs = java.lang.System.currentTimeMillis() / 1000
        val endTsecs = java.lang.System.currentTimeMillis() / 1000

        val newData = DBObject(
          "startTsecs" -> startTsecs,
          "endTsecs" -> -1,
          "classPath" -> adGroupDataPullRequest.adGroupObject.adGroup.getClass.getCanonicalName,
          "object" -> JSON.parse(gson.toJson(adGroupDataPullRequest.adGroupObject.adGroup)).asInstanceOf[DBObject]
        )

        var matchFound = true

        googleAdGroupCollection.findOne(
          qry ++ ("adGroup.endTsecs" -> -1)
        ) match {
          case Some(adGroupRs) =>
            if (
              !gson.toJson(dboToGoogleEntity[AdGroup](adGroupRs, "adGroup", None))
                .equals(gson.toJson(adGroupDataPullRequest.adGroupObject.adGroup))
            ) {
              googleAdGroupCollection.update(
                qry ++ ("adGroup.endTsecs" -> -1),
                DBObject("$set" -> DBObject("adGroup.0.endTsecs" -> endTsecs))
              )
              matchFound = false
              log.debug("Google AdGroup match found. Changes detected. Updating...")
            }
          case _ =>
            matchFound = false
            log.debug("No Google AdGroup record Found. Inserting...")
        }
        if(!matchFound) {
          googleAdGroupCollection.update(qry, DBObject("$push" -> DBObject("adGroup" -> newData)), upsert = true)
          if(adGroupDataPullRequest.pushToExternal)
            googleAdGroupSyncActor ! (adGroupDataPullRequest, endTsecs)
        }

        if(adGroupDataPullRequest.recursivePull) {

          adGroupDataPullRequest.adGroupObject.adGroupObjId = googleAdGroupCollection.findOne(qry).get._id

          val adHelper = new AdGroupAdHelper(adGroupDataPullRequest.adWordsHelper, log)

          val adSelector = (new SelectorBuilder)
            .offset(0)
            .fields(adGroupAdFields: _*)
            .equals(AdGroupAdField.AdGroupId, adGroupDataPullRequest.adGroupObject.adGroup.getId.toString)
            .limit(adGroupDataPullRequest.adWordsHelper.PAGE_SIZE)
            .build()

          adHelper.getAdGroupAds(adGroupAdFields, None, Some(adSelector)).foreach(ad =>
            googleAdGroupAdActor ! GoogleAdGroupAdDataPullRequest(
              adWordsHelper = adGroupDataPullRequest.adWordsHelper,
              adGroupAdObject = AdGroupAdObject(
                adGroupObject = adGroupDataPullRequest.adGroupObject,
                adGroupAdObjId = None,
                adGroupAd = ad
              ),
              pushToExternal = true
            )
          )


          val keywordHelper = new AdGroupCriterionHelper(adGroupDataPullRequest.adWordsHelper, log)

          val keywordSelector = (new SelectorBuilder)
            .offset(0)
            .fields(adGroupCriterionFields: _*)
            .equals(AdGroupCriterionField.AdGroupId, adGroupDataPullRequest.adGroupObject.adGroup.getId.toString)
            .limit(adGroupDataPullRequest.adWordsHelper.PAGE_SIZE)
            .build()

          keywordHelper.getAdGroupCriterion(adGroupCriterionFields, None, Some(keywordSelector)).foreach(criterion =>
            googleAdGroupCriterionActor ! GoogleAdGroupCriterionDataPullRequest(
              adGroupDataPullRequest.adWordsHelper,
              adGroupCriterionObject = AdGroupCriterionObject(
                adGroupObject = adGroupDataPullRequest.adGroupObject,
                adGroupCriterionObjId = None,
                adGroupCriterion = criterion
              ),
              pushToExternal = true
            )
          )
        }
      } catch {
        case e: Exception =>
          log.info("Error Retrieving Data for Google AdGroup (%s) - %s".format(
            adGroupDataPullRequest.adGroupObject.adGroup.getName,
            e.getMessage
          ))
          e.printStackTrace()
      }
    case cache: PendingCacheStructure =>
      val adgroup_change = AdGroupController.dboToAdGroupForm(cache.changeData.asDBObject)
      log.info("Processing Pending Cache %s -> %s -> %s -> %s".format(
        cache.changeCategory,
        cache.changeType,
        cache.trafficSource,
        cache.id
      ))

      val account_data = gson.fromJson(
        googleCustomerCollection.findOne(
          DBObject(
            "mccObjId" -> new ObjectId(adgroup_change.parent.mccObjId.get),
            "apiId" -> adgroup_change.parent.customerApiId
          ),
          DBObject("$slice" -> -1)
        ).get.getAs[String]("customer").get,
        classOf[com.google.api.ads.adwords.axis.v201609.mcm.Customer]
      )

      val mcc_data = dboToMcc(
        googleMccCollection.findOne(
          DBObject("_id" -> new ObjectId(adgroup_change.parent.mccObjId.get))
        ).get
      )

      val adWordsHelper = new AdWordsHelper(
        clientId=mcc_data.oAuthClientId,
        clientSecret=mcc_data.oAuthClientSecret,
        refreshToken=mcc_data.oAuthRefreshToken,
        developerToken=mcc_data.developerToken,
        customerId=Some(account_data.getCustomerId.toString)
      )

      val adGroupHelper = new AdGroupHelper(adWordsHelper, log)

      var adgroups = ListBuffer[AdGroup]()

      if (cache.changeType != ChangeType.NEW) {
        adgroups = adgroups ++ adGroupHelper
          .getAdGroups(
            List(AdGroupField.Id, AdGroupField.Name),
            None,
            Some(
              (new SelectorBuilder)
                .equals(AdGroupField.Id, adgroup_change.apiId.getOrElse("").toString)
                .build
            )
          )
      }

      cache.changeType match {
        case ChangeType.DELETE =>
          adgroups.map(adgroup => adGroupHelper.deleteAdGroup(adgroup))
        case ChangeType.NEW =>
          val adgroup = new AdGroup()
          adgroup.setName(adgroup_change.name)
          adgroup.setCampaignId(adgroup_change.parent.campaignApiId.get)
          if(adgroup_change.contentBidCriterionTypeGroup.nonEmpty) {
            adgroup.setContentBidCriterionTypeGroup(CriterionTypeGroup.fromString(adgroup_change.contentBidCriterionTypeGroup.getOrElse("UNKNOWN")))
          }
          adgroup.setStatus(AdGroupStatus.fromString(adgroup_change.status))
          adGroupHelper.createAdGroup(adgroup)
        case ChangeType.UPDATE =>
          adgroups.foreach{adgroup =>
            adgroup.setName(adgroup_change.name)
            adgroup.setStatus(AdGroupStatus.fromString(adgroup_change.status))
            if(adgroup_change.contentBidCriterionTypeGroup.nonEmpty) {
              adgroup.setContentBidCriterionTypeGroup(CriterionTypeGroup.fromString(adgroup_change.contentBidCriterionTypeGroup.getOrElse("UNKNOWN")))
            }
            adGroupHelper.updateAdGroup(adgroup)
          }
      }*/
  }
}