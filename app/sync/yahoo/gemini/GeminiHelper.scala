package sync.yahoo.gemini

import Shared.Shared._
import akka.event.LoggingAdapter
import com.mongodb.casbah.Imports._
import models.mongodb.yahoo.Yahoo._
import org.joda.time.DateTime
import play.api.Play
import play.api.Play.current
import play.api.libs.json.{JsNull, Json}
import play.api.libs.ws.{WS, WSResponse}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class GeminiHelper (
  client_id: String,
  client_secret: String,
  refresh_token: String,
  log: LoggingAdapter
) {

  val PAGESIZE = 300
  val baseEndPoint = Play.current.configuration.getString("yahoo.gemini.endpoint.baseurl").get
  val reportingEndPoint = Play.current.configuration.getString("yahoo.gemini.reporting.endpoint.url").get

  val advertiserEndPoint = baseEndPoint + "advertiser"
  val campaignEndPoint = baseEndPoint + "campaign"
  val adGroupEndPoint = baseEndPoint + "adgroup"
  val adEndPoint = baseEndPoint + "ad"
  val adExtensionEndPoint = baseEndPoint + "adextension"
  val keywordEndPoint = baseEndPoint + "keyword"


  object RequestType extends Enumeration {
    type RequestType = Value
    val FindAdvertiser,
    FindCampaign,
    UpdateCampaign,
    CreateCampaign,
    DeleteCampaign,
    FindAdGroup,
    UpdateAdGroup,
    CreateAdGroup,
    DeleteAdGroup,
    FindAd,
    UpdateAd,
    CreateAd,
    DeleteAd,
    FindAdExtension,
    UpdateAdExtension,
    CreateAdExtension,
    DeleteAdExtension,
    FindKeyword,
    UpdateKeyword,
    CreateKeyword,
    DeleteKeyword = Value
  }

  object RequestMethod extends Enumeration {
    type RequestMethod = Value
    val POST,
    GET,
    PUT = Value
  }

  case class RequestOptions(
    requestMethod: RequestMethod.Value,
    requestUrl: String
  )

  val requestFormat = Map(
    RequestType.FindAdvertiser -> RequestOptions(
      RequestMethod.GET,
      advertiserEndPoint
    ),
    RequestType.FindCampaign -> RequestOptions(
      RequestMethod.GET,
      campaignEndPoint
    ),
    RequestType.UpdateCampaign -> RequestOptions(
      RequestMethod.PUT,
      campaignEndPoint
    ),
    RequestType.CreateCampaign -> RequestOptions(
      RequestMethod.POST,
      campaignEndPoint
    ),
    RequestType.DeleteCampaign -> RequestOptions(
      RequestMethod.PUT,
      campaignEndPoint
    ),
    RequestType.FindAdGroup -> RequestOptions(
      RequestMethod.GET,
      adGroupEndPoint
    ),
    RequestType.UpdateAdGroup -> RequestOptions(
      RequestMethod.PUT,
      adGroupEndPoint
    ),
    RequestType.CreateAdGroup -> RequestOptions(
      RequestMethod.POST,
      adGroupEndPoint
    ),
    RequestType.DeleteAdGroup -> RequestOptions(
      RequestMethod.PUT,
      adGroupEndPoint
    ),
    RequestType.FindAd -> RequestOptions(
      RequestMethod.GET,
      adEndPoint
    ),
    RequestType.UpdateAd -> RequestOptions(
      RequestMethod.PUT,
      adEndPoint
    ),
    RequestType.CreateAd -> RequestOptions(
      RequestMethod.POST,
      adEndPoint
    ),
    RequestType.DeleteAd -> RequestOptions(
      RequestMethod.PUT,
      adEndPoint
    ),
    RequestType.FindAdExtension -> RequestOptions(
      RequestMethod.GET,
      adExtensionEndPoint
    ),
    RequestType.UpdateAdExtension -> RequestOptions(
      RequestMethod.PUT,
      adExtensionEndPoint
    ),
    RequestType.CreateAdExtension -> RequestOptions(
      RequestMethod.POST,
      adExtensionEndPoint
    ),
    RequestType.DeleteAdExtension -> RequestOptions(
      RequestMethod.PUT,
      adExtensionEndPoint
    ),
    RequestType.FindKeyword -> RequestOptions(
      RequestMethod.GET,
      keywordEndPoint
    ),
    RequestType.UpdateKeyword -> RequestOptions(
      RequestMethod.PUT,
      keywordEndPoint
    ),
    RequestType.CreateKeyword -> RequestOptions(
      RequestMethod.POST,
      keywordEndPoint
    ),
    RequestType.DeleteKeyword -> RequestOptions(
      RequestMethod.PUT,
      keywordEndPoint
    )
  )

  var accessToken: Option[String] = None
  var refreshToken: Option[String] = None
  var tokenExpiration: Option[DateTime] = None

  resetTokens()

  if(!refresh_token.equals(refreshToken.getOrElse(refresh_token)))
    yahooApiAccountCollection.update(
      DBObject(
        "client_id" -> client_id,
        "client_secret" -> client_secret,
        "refresh_token" -> refresh_token
      ),
      DBObject(
        "refresh_Token" -> refreshToken
      ),
      upsert = true
    )

  def resetTokens() = {
    val tokens = Await.result(WS.url(
      Play.current.configuration.getString("yahoo.gemini.oauth.url").get
    ).post(Map(
      "client_id" -> Seq(client_id),
      "client_secret" -> Seq(client_secret),
      "redirect_uri" -> Seq("oob"),
      "refresh_token" -> Seq(refresh_token),
      "grant_type" -> Seq("refresh_token")
    )), Duration.Inf)

    tokenExpiration = Some(new DateTime((System.currentTimeMillis() / 1000) + tokens.json.\("expires_in").as[Long]))
    accessToken = Some(tokens.json.\("access_token").as[String])
    refreshToken = Some(tokens.json.\("refresh_token").as[String])
  }

  lazy val authHeader = "Authorization" -> "Bearer %s".format(accessToken.getOrElse(""))
  lazy val acceptHeader = "Accept" -> "application/json"
  lazy val contentTypeHeader = "Content-Type" -> "application/json"

  def apiRequest(requestType: RequestType.Value, params: Map[String, String]): WSResponse = {
    val format = requestFormat(requestType)
    val updateParams = params.map(x => (x._1, Seq(x._2)))
    format.requestMethod match {
      case RequestMethod.GET =>
        Await.result(WS.url(format.requestUrl).withHeaders(authHeader, acceptHeader, contentTypeHeader).withQueryString(params.toSeq: _*).get(), Duration.Inf)
      case RequestMethod.POST =>
        Await.result(WS.url(format.requestUrl).withHeaders(authHeader, acceptHeader, contentTypeHeader).post(updateParams), Duration.Inf)
      case RequestMethod.PUT =>
        Await.result(WS.url(format.requestUrl).withHeaders(authHeader, acceptHeader, contentTypeHeader).put(updateParams), Duration.Inf)
    }
  }

  def reportRequest(reportRequest: GeminiReportRequest): Option[String] = {
    try {
      val requestParam = Json.obj(
        "cube" -> reportRequest.reportType.toString,
        "fields" -> reportRequest.fields.map(x => Json.obj("field" -> x.toString.replace('_', ' '))),
        "filters" -> reportRequest.filters.getOrElse(List()).map(x =>
          Json.obj(
            "field" -> x.field.toString.replace('_', ' '),
            "operator" -> (if (x.operator == GeminiReportFilterOperation.`=`) "=" else x.operator.toString)
          ) ++ (x.operator match {
            case GeminiReportFilterOperation.`=` => Json.obj("value" -> Json.toJson(x.value.getOrElse("")))
            case GeminiReportFilterOperation.between => Json.obj("from" -> Json.toJson(x.from.getOrElse("")), "to" -> Json.toJson(x.to.getOrElse("")))
            case GeminiReportFilterOperation.IN => Json.obj("values" -> Json.toJson(x.values.getOrElse(List())))
            case _ => Json.obj()
          })
        ))

      var url = "%s?reportFormat=%s".format(reportingEndPoint, reportRequest.reportFormat.toString.toLowerCase)
      val res = Await.result(
        WS.url(url)
          .withHeaders(authHeader, acceptHeader, contentTypeHeader).post(requestParam), Duration.Inf
      ).json

      log.info("%s - %s".format(url, res))
      var status = res.\("response").get.\("status").get.as[String]

      while ((status == GeminiReportJobStatus.submitted.toString || status == GeminiReportJobStatus.running.toString) && res.\("errors").get == JsNull) {
        url = "%s/%s?advertiserId=%s".format(
          reportingEndPoint,
          res.\("response").get.\("jobId").get.as[String],
          reportRequest.filters.get.filter(_.field == GeminiReportField.Advertiser_ID).head.value.get
        )
        val qry = Await.result(
          WS.url(url).withHeaders(authHeader, acceptHeader, contentTypeHeader).get(),
          Duration.Inf
        ).json

        log.info("%s - %s".format(url, qry))

        status = qry.\("response").get.\("status").get.as[String]
        if (status == GeminiReportJobStatus.completed.toString) {
          val csvData = Await.result(WS.url(qry.\("response").get.\("jobResponse").get.as[String]).get(), Duration.Inf)
          return Some(csvData.body)
        }
        Thread.sleep(30000)
      }
      None
    } catch {
      case e: Exception =>
        e.printStackTrace()
        None
    }
  }
}
