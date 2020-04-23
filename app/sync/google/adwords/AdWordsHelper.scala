package sync.google.adwords

import akka.event.LoggingAdapter
import com.google.api.ads.adwords.axis.factory.AdWordsServices
import com.google.api.ads.adwords.axis.v201609.cm._
import com.google.api.ads.adwords.lib.client.AdWordsSession
import com.google.api.ads.common.lib.auth.OfflineCredentials
import com.google.api.ads.common.lib.auth.OfflineCredentials.Api
import Shared.Shared._

class AdWordsHelper(
  clientId: String,
  clientSecret: String,
  refreshToken: String,
  developerToken: String,
  customerId: Option[String],
  enablePartialFailure: Boolean = false
) {
  val PAGE_SIZE = 10000
  val PLATFORMS = Map(
    "mobile" -> 30000,
    "HighEndMobile" -> 30001,
    "Tablet" -> 30002
  )

  var oAuth2Credential = new OfflineCredentials.Builder()
      .forApi(Api.ADWORDS)
      .withClientSecrets(clientId, clientSecret)
      .withRefreshToken(refreshToken)
      .build()
      .generateCredential()

  var adWordsSession = new AdWordsSession.Builder()
      .withDeveloperToken(developerToken)
      .withOAuth2Credential(oAuth2Credential)
      .withUserAgent("adVanced")
      .build()

  customerId match {
    case Some(id) =>
      adWordsSession.setClientCustomerId(id)
    case None =>
  }

  adWordsSession.setPartialFailure(enablePartialFailure)


  var adWordsServices = new AdWordsServices()


  def sleepIfRequired(ae: ApiException, log: LoggingAdapter, referer: String) = {
    ae.getErrors.foreach{
      error =>
        if(error.myIsInstanceOf[RateExceededError]) {
          val ree = error.asInstanceOf[RateExceededError]
          log.info("RateExceededError (%s) - Sleeping for %s seconds".format(referer, ree.getRetryAfterSeconds))
          //todo: Needs to schedule future job rather than thread sleeping
          //Thread.sleep(ree.getRetryAfterSeconds * 1000)
        } else {
          log.info("FATAL EXCEPTION - %s".format(error.getErrorString))
        }
    }
  }

  def bulkMutate(operations: Array[BatchJobOperation]): BatchJobReturnValue = {
    val batchJobService = adWordsServices.get(adWordsSession, classOf[BatchJobServiceInterface])
    batchJobService.mutate(operations)
  }

  def getBulkMutateStatus(jobIds: Array[String]): Array[BatchJob] = {
    val batchJobService = adWordsServices.get(adWordsSession, classOf[BatchJobServiceInterface])
    val jobSelector = new Selector()
    jobSelector.setPredicates(Array(new Predicate("id", PredicateOperator.IN, jobIds)))
    batchJobService.get(jobSelector).getEntries
  }
}