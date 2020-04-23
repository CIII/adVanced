package controllers.advertisers

import be.objectify.deadbolt.scala.DeadboltActions
import play.api.mvc.Controller
import play.api.i18n.I18nSupport
import javax.inject.Inject
import scala.concurrent.Future
import play.api.mvc.Results._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.i18n.MessagesApi
import play.api.libs.json.JsObject
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import play.api.libs.json.Json
import play.api.Logger
import be.objectify.deadbolt.scala.cache.HandlerCache
import play.api.data.Form
import play.api.data.Forms._
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime

class AdvertiserController @Inject()(
  val messagesApi: MessagesApi, 
  deadbolt: DeadboltActions, 
  handler: HandlerCache, 
  implicit val environment: play.api.Environment, 
  implicit val configuration: play.api.Configuration, 
  protected val dbConfigProvider: DatabaseConfigProvider
) extends Controller with I18nSupport with HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._
  
  val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  val timeout = Duration(configuration.getString("defaultMySQLTimeout").getOrElse(throw new Exception("Missing config parameter for default MySQL timeout: defaultMySQLTimeout")) + "seconds")
  
  def advertiser(advertiserId: Int) = deadbolt.SubjectPresent()() {
    implicit request =>
      var advertiserName = request.getQueryString("advertiserName").getOrElse(getAdvertiserNameForId(advertiserId))
      var startDate = request.getQueryString("startDate").getOrElse(dateTimeFormat.print(DateTime.now().minusDays(30)))
      var endDate = request.getQueryString("endDate").getOrElse(dateTimeFormat.print(DateTime.now()))
      Future(Ok(views.html.advertiser("", advertiserId, advertiserName, startDate, endDate, getAdvertiserJson(advertiserId, startDate, endDate))))
  }
  
  def getAdvertiserJson(adv_id: Int, start: String, end: String): JsObject ={
    Logger.debug("Fetching data for advertiser: %d between %s and %s".format(adv_id, start, end))
    Json.parse(s"""{"start": "$start", "end": "$end", "adv_id": "$adv_id", "results": [${Await.result(db.run(sql"""
        SELECT JSON_OBJECT(
            'date', date(created_at),
            'count', count(*),
            'total_price', sum(price)
          ) AS json
          FROM
            (SELECT lead_match_id, max(created_at) AS created from advertiser_dispositions WHERE advertiser_id = $adv_id and created_at BETWEEN $start and $end and status = 1 and pony_phase IN (3,6) group by lead_match_id) last
            JOIN advertiser_dispositions ad ON last.lead_match_id = ad.lead_match_id AND last.created = ad.created_at
            where ad.pony_phase IN (3,6) and advertiser_id = $adv_id
            group by date(created_at);""".as[(String)]), timeout
      ).mkString(",")}]}""").as[JsObject]
  }

  def getAdvertiserNameForId(adv_id: Int): String = {
    Logger.debug("Retrieving advertiser name for id: %d".format(adv_id))
    Await.result(
        db.run(sql"""SELECT name FROM advertisers WHERE id=$adv_id""".as[(String)]), 
        Duration.Inf
    ).mkString
  }
}