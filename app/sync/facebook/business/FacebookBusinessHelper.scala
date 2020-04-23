package sync.facebook.business

import java.util.concurrent.TimeUnit

import com.facebook.ads.sdk.Business
import com.facebook.ads.sdk.APIContext
import models.mongodb.facebook.Facebook._
import play.api.Play
import play.api.Play._
import play.api.libs.ws.{WS, WSRequest, WSResponse}

import scala.concurrent.duration._
import Shared.Shared._
import akka.event.LoggingAdapter

class FacebookBusinessHelper(
  fbBizAccount: FacebookBusinessAccount,
  log: LoggingAdapter
) {
  val fbGraphUrl = Play.current.configuration.getString("facebook.graph.baseUrl").get
  val requestTimeout = Duration.create(
    Play.current.configuration.getString("facebook.graph.requestTimeout").get.toDouble,
    TimeUnit.SECONDS
  )
  
  
  def requestWithTimeout(url: String): WSRequest = {
    log.debug("Creating request: " + url)
    WS.url(url)
      .withQueryString("access_token" -> fbBizAccount.accessToken)
      .withRequestTimeout(requestTimeout)
      .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
  }
}