package sync.google.adwords.account

import akka.event.LoggingAdapter
import com.google.api.ads.adwords.axis.utils.v201609.SelectorBuilder
import com.google.api.ads.adwords.axis.v201609.cm._
import com.google.api.ads.adwords.lib.selectorfields.v201609.cm.AdGroupAdField
import sync.google.adwords.AdWordsHelper

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

class AdGroupAdHelper(AdWords: AdWordsHelper, log: LoggingAdapter) {

  private val adGroupAdService = AdWords.adWordsServices.get(AdWords.adWordsSession, classOf[AdGroupAdServiceInterface])

  def getAdGroupAds(fields: List[AdGroupAdField], offset: Int=0, predicates: Option[Array[Predicate]]=None, sel: Option[Selector]=None): AdGroupAdPage = {
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
    adGroupAdService.get(selector)
  }

  def createAdGroupAd(adGroupAd: AdGroupAd): AdGroupAdReturnValue  = {
    val adGroupAdOperation = new AdGroupAdOperation
    adGroupAdOperation.setOperator(Operator.ADD)
    adGroupAdOperation.setOperand(adGroupAd)
    adGroupAdService.mutate(Array(adGroupAdOperation))
  }

  def deleteAdGroupAd(adGroupAd: AdGroupAd): AdGroupAdReturnValue = {
    val adGroupAdOperation = new AdGroupAdOperation
    adGroupAdOperation.setOperator(Operator.REMOVE)
    adGroupAdOperation.setOperand(adGroupAd)
    adGroupAdService.mutate(Array(adGroupAdOperation))
  }

  def updateAdGroupAd(adGroupAd: AdGroupAd): AdGroupAdReturnValue = {
    val adGroupAdOperation = new AdGroupAdOperation
    adGroupAdOperation.setOperator(Operator.SET)
    adGroupAdOperation.setOperand(adGroupAd)
    adGroupAdService.mutate(Array(adGroupAdOperation))
  }
}
