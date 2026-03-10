package sync.google.adwords

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logging}

/**
 * Google Ads API service.
 *
 * TODO: Re-implement with Google Ads API v18 (com.google.ads.googleads).
 * The old AdWords API (v201609) has been sunset. Key changes:
 * - SOAP/XML → gRPC/protobuf
 * - AdWordsSession → GoogleAdsClient
 * - AdWordsServices.get() → GoogleAdsClient.getLatestVersion().createXxxServiceClient()
 * - Selector-based queries → GAQL (Google Ads Query Language)
 * - Report downloads → GoogleAdsServiceClient.searchStream()
 */
@Singleton
class GoogleAdsService @Inject()(configuration: Configuration) extends Logging {

  private val clientId = configuration.get[String]("google_auth.client_id")
  private val clientSecret = configuration.get[String]("google_auth.client_secret")

  val PAGE_SIZE = 10000

  val PLATFORMS: Map[String, Int] = Map(
    "mobile" -> 30000,
    "HighEndMobile" -> 30001,
    "Tablet" -> 30002
  )

  // TODO: Initialize GoogleAdsClient from configuration
  // val googleAdsClient: GoogleAdsClient = GoogleAdsClient.newBuilder()
  //   .setDeveloperToken(developerToken)
  //   .setOAuth2Credential(credential)
  //   .build()
}
