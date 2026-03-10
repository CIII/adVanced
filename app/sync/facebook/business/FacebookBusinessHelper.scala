package sync.facebook.business

import java.util.concurrent.TimeUnit

// TODO: Update to facebook-java-business-sdk v20
import com.facebook.ads.sdk.Business
import com.facebook.ads.sdk.APIContext
import models.mongodb.facebook.Facebook
import models.mongodb.facebook.Facebook._
import play.api.libs.ws.{WSRequest, WSResponse}

import scala.concurrent.duration._
import Shared.Shared._
import org.apache.pekko.event.LoggingAdapter

class FacebookBusinessHelper(
  fbBizAccount: FacebookBusinessAccount,
  log: LoggingAdapter
) {
  val fbGraphUrl: String = Facebook.configuration.get[String]("facebook.graph.baseUrl")
  val requestTimeout: scala.concurrent.duration.Duration = Duration.create(
    Facebook.configuration.get[String]("facebook.graph.requestTimeout").toDouble,
    TimeUnit.SECONDS
  )

  def requestWithTimeout(url: String): WSRequest = {
    log.debug("Creating request: " + url)
    Facebook.ws.url(url)
      .addQueryStringParameters("access_token" -> fbBizAccount.accessToken)
      .withRequestTimeout(requestTimeout)
      .addHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded")
  }
}
