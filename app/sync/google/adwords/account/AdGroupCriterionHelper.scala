package sync.google.adwords.account

import akka.event.LoggingAdapter
import com.google.api.ads.adwords.axis.utils.v201609.SelectorBuilder
import com.google.api.ads.adwords.axis.v201609.cm._
import com.google.api.ads.adwords.axis.v201609.o.{AttributeType, IdeaType, LanguageSearchParameter, RelatedToQuerySearchParameter, RequestType, SearchParameter, SeedAdGroupIdSearchParameter, TargetingIdea, TargetingIdeaPage, TargetingIdeaSelector, TargetingIdeaServiceInterface}
import com.google.api.ads.adwords.lib.selectorfields.v201609.cm.AdGroupCriterionField
import sync.google.adwords.AdWordsHelper

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._


class AdGroupCriterionHelper(AdWords: AdWordsHelper, log: LoggingAdapter) {

  private val dataService: DataServiceInterface = AdWords.adWordsServices.get(AdWords.adWordsSession, classOf[DataServiceInterface])
  private val targetingIdeaService: TargetingIdeaServiceInterface = AdWords.adWordsServices.get(AdWords.adWordsSession, classOf[TargetingIdeaServiceInterface])
  private val adGroupCriterionService: AdGroupCriterionServiceInterface = AdWords.adWordsServices.get(AdWords.adWordsSession, classOf[AdGroupCriterionServiceInterface])


  def getKeywordBidSimulations(fields: List[AdGroupCriterionField], offset: Int=0, predicates: Option[Array[Predicate]]=None, sel: Option[Selector]=None): CriterionBidLandscapePage = {
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
    dataService.getCriterionBidLandscape(selector)
  }

  def getKeywords(fields: List[AdGroupCriterionField], offset: Int=0, predicates: Option[Array[Predicate]]=None, sel: Option[Selector]=None): AdGroupCriterionPage = {
    val selector = sel match {
      case Some(s) => s
      case _ =>
        (new SelectorBuilder)
          .offset(offset)
          .fields(fields: _*)
          .limit(AdWords.PAGE_SIZE)
          .build()
    }

    val paging = new Paging()

    paging.setNumberResults(AdWords.PAGE_SIZE)
    paging.setStartIndex(offset)

    predicates match {
      case Some(p) =>
        selector.setPredicates(p)
      case _ =>
    }
    selector.setPaging(paging)
    adGroupCriterionService.get(selector)
  }

  def getAdGroupCriterion(fields: List[AdGroupCriterionField], offset: Int=0, predicates: Option[Array[Predicate]]=None, sel: Option[Selector]=None): AdGroupCriterionPage = {
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
    adGroupCriterionService.get(selector)
  }

  def createAdGroupCriterion(criterion: AdGroupCriterion): AdGroupCriterionReturnValue = {
    val adGroupCriterionOperation = new AdGroupCriterionOperation
    adGroupCriterionOperation.setOperator(Operator.ADD)
    adGroupCriterionOperation.setOperand(criterion)
    adGroupCriterionService.mutate(Array(adGroupCriterionOperation))
  }

  def deleteAdGroupCriterion(criterion: AdGroupCriterion): AdGroupCriterionReturnValue = {
    val adGroupCriterionOperation = new AdGroupCriterionOperation
    adGroupCriterionOperation.setOperator(Operator.REMOVE)
    adGroupCriterionOperation.setOperand(criterion)
    adGroupCriterionService.mutate(Array(adGroupCriterionOperation))
  }

  def updateAdGroupCriterion(criterion: AdGroupCriterion): AdGroupCriterionReturnValue = {
    val adGroupCriterionOperation = new AdGroupCriterionOperation
    adGroupCriterionOperation.setOperator(Operator.SET)
    adGroupCriterionOperation.setOperand(criterion)
    adGroupCriterionService.mutate(Array(adGroupCriterionOperation))
  }
}