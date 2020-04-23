package sync.google.adwords.account

import akka.event.LoggingAdapter
import com.google.api.ads.adwords.axis.utils.v201609.SelectorBuilder
import com.google.api.ads.adwords.axis.v201609.cm._
import com.google.api.ads.adwords.lib.selectorfields.v201609.cm.{AdGroupBidModifierField, AdGroupField}
import sync.google.adwords.AdWordsHelper

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

class AdGroupHelper(AdWords: AdWordsHelper, log: LoggingAdapter) {

  private val adGroupBidModifierService = AdWords.adWordsServices.get(AdWords.adWordsSession, classOf[AdGroupBidModifierServiceInterface])
  private val adGroupService = AdWords.adWordsServices.get(AdWords.adWordsSession, classOf[AdGroupServiceInterface])

  def getAdGroupBidModifiers(fields: List[AdGroupBidModifierField], offset: Int=0, predicates: Option[Array[Predicate]]=None, sel: Option[Selector]=None): AdGroupBidModifierPage = {
    val paging = new Paging()
    paging.setNumberResults(AdWords.PAGE_SIZE)
    paging.setStartIndex(offset)

    val selector = sel match {
      case Some(s) => s
      case _ =>
        (new SelectorBuilder)
          .offset(offset)
          .fields(fields: _*)
          .limit(AdWords.PAGE_SIZE)
          .build()
    }

    predicates match {
      case Some(p) =>
        selector.setPredicates(p)
      case _ =>
    }

    selector.setPaging(paging)
    adGroupBidModifierService.get(selector)
  }


  def modifyBidModifier(adGroupId: Long, bid_modifier: Double = 0, platformSelection: String = "desktop"): Boolean = {
    val platform: Platform = new Platform()
    platform.setId(AdWords.PLATFORMS.get(platformSelection).asInstanceOf[Long])

    val adGroupBidModifier: AdGroupBidModifier = new AdGroupBidModifier()
    adGroupBidModifier.setAdGroupId(adGroupId)
    adGroupBidModifier.setBidModifier(bid_modifier)
    adGroupBidModifier.setCriterion(platform)

    val operation = new AdGroupBidModifierOperation()
    operation.setOperand(adGroupBidModifier)
    operation.setOperator(Operator.SET)

    val result: AdGroupBidModifierReturnValue = adGroupBidModifierService.mutate(Array(operation))
    if (result == null) {
      return false
    }

    for (bidModifierResult: AdGroupBidModifier <- result.getValue) {
      log.info("Campaign ID '%d', AdGroup ID '%d' was updated with ad group level modifier: %.2f\n",
        bidModifierResult.getCampaignId, bidModifierResult.getAdGroupId,
        bidModifierResult.getBidModifier)
    }
    true
  }

  def addBidModifier(adGroupId: Long, bid_modifier: Double = 0, platformSelection: String = "desktop"): Boolean = {
    val platform: Platform = new Platform()
    platform.setId(AdWords.PLATFORMS(platformSelection).asInstanceOf[Long])

    val adGroupBidModifier = new AdGroupBidModifier()
    adGroupBidModifier.setAdGroupId(adGroupId)
    adGroupBidModifier.setBidModifier(bid_modifier)
    adGroupBidModifier.setCriterion(platform)

    val operation = new AdGroupBidModifierOperation()
    operation.setOperand(adGroupBidModifier)
    operation.setOperator(Operator.ADD)


    val result: AdGroupBidModifierReturnValue = adGroupBidModifierService.mutate(Array(operation))

    if (result == null) {
      return false
    }
    for (bidModifierResult: AdGroupBidModifier <- result.getValue) {
      log.info("Campaign ID '%s', AdGroup ID '%s' was updated with ad group level modifier: %s".format(
        bidModifierResult.getCampaignId,
        bidModifierResult.getAdGroupId,
        bidModifierResult.getBidModifier
      ))
    }
    true
  }

  def getAdGroups(fields: List[AdGroupField], offset: Int=0, predicates: Option[Array[Predicate]]=None, sel: Option[Selector]=None): AdGroupPage = {
    val paging = new Paging {
      setNumberResults(AdWords.PAGE_SIZE)
      setStartIndex(offset)
    }

    val selector = sel match {
      case Some(s) => s
      case _ =>
        (new SelectorBuilder)
          .offset(offset)
          .fields(fields: _*)
          .limit(AdWords.PAGE_SIZE)
          .build()
    }

    predicates match {
      case Some(p) =>
        selector.setPredicates(p)
      case _ =>
    }

    selector.setPaging(paging)
    adGroupService.get(selector)
  }

  def createAdGroup(adgroup: AdGroup): AdGroupReturnValue = {
    val adGroupOperation = new AdGroupOperation
    adGroupOperation.setOperator(Operator.ADD)
    adGroupOperation.setOperand(adgroup)
    adGroupService.mutate(Array(adGroupOperation))
  }

  def deleteAdGroup(adgroup: AdGroup): AdGroupReturnValue = {
    val adGroupOperation = new AdGroupOperation
    adGroupOperation.setOperator(Operator.REMOVE)
    adGroupOperation.setOperand(adgroup)
    adGroupService.mutate(Array(adGroupOperation))
  }

  def updateAdGroup(adgroup: AdGroup): AdGroupReturnValue = {
    val adGroupOperation = new AdGroupOperation
    adGroupOperation.setOperator(Operator.SET)
    adGroupOperation.setOperand(adgroup)
    adGroupService.mutate(Array(adGroupOperation))
  }
}