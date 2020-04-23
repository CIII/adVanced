package sync.google.adwords.account

import akka.event.LoggingAdapter
import com.google.api.ads.adwords.axis.utils.v201609.SelectorBuilder
import com.google.api.ads.adwords.axis.v201609.cm.{Selector, _}
import com.google.api.ads.adwords.lib.selectorfields.v201609.cm.{CampaignCriterionField, CampaignField}
import sync.google.adwords.AdWordsHelper

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

class CampaignHelper(AdWords: AdWordsHelper, log: LoggingAdapter) {
  private val campaignService = AdWords.adWordsServices.get(AdWords.adWordsSession, classOf[CampaignServiceInterface])
  private val campaignCriterionService = AdWords.adWordsServices.get(AdWords.adWordsSession, classOf[CampaignCriterionServiceInterface])

  def getCampaigns(fields: List[CampaignField], offset: Int=0, predicates: Option[Array[Predicate]]=None, sel: Option[Selector]=None): CampaignPage = {
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
    campaignService.get(selector)
  }

  def getCampaignCriterion(fields: List[CampaignCriterionField], offset: Int=0, predicates: Option[Array[Predicate]]=None, sel: Option[Selector]=None): CampaignCriterionPage = {
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
    campaignCriterionService.get(selector)
  }

  def createCampaign(campaign: Campaign): CampaignReturnValue = {
    val campaignOperation = new CampaignOperation
    campaignOperation.setOperator(Operator.ADD)
    campaignOperation.setOperand(campaign)
    campaignService.mutate(Array(campaignOperation))
  }

  def deleteCampaign(campaign: Campaign): CampaignReturnValue = {
    val campaignOperation = new CampaignOperation
    campaignOperation.setOperator(Operator.REMOVE)
    campaignOperation.setOperand(campaign)
    campaignService.mutate(Array(campaignOperation))
  }

  def updateCampaign(campaign: Campaign): CampaignReturnValue = {
    val campaignOperation = new CampaignOperation
    campaignOperation.setOperator(Operator.SET)
    campaignOperation.setOperand(campaign)
    campaignService.mutate(Array(campaignOperation))
  }
}
