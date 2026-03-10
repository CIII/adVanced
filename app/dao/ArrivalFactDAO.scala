package dao

import com.github.tototoshi.slick.MySQLJodaSupport._
import models.mysql._
import org.joda.time.DateTime
import play.api.db.slick.HasDatabaseConfig
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class ArrivalFactDAO(dbConfigIn: DatabaseConfig[JdbcProfile])(implicit ec: ExecutionContext) extends HasDatabaseConfig[JdbcProfile] {
  override protected lazy val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigIn
  import profile.api._

  private val ArrivalFacts = TableQuery[ArrivalFactTable]

  def all(): Future[Seq[ArrivalFact]] = db.run(ArrivalFacts.result)

  //def insert(arrival_fact: ArrivalFact): Future[Unit] = db.run(ArrivalFacts += arrival_fact).map { _ => () }

  def insert(arrival_fact: ArrivalFact): Future[ArrivalFact] =
    db.run(ArrivalFacts returning ArrivalFacts.map(_.id) into ((u, id) => u.copy(id = id)) += arrival_fact)

  def batchInsert(arrival_facts: Seq[ArrivalFact]): Future[Unit] =
    db.run(ArrivalFacts ++= arrival_facts).map { _ => () }

  def update(arrival_fact: ArrivalFact): Future[Unit] = {
    val formToUpdate = arrival_fact.copy(arrival_fact.id)
    db.run(ArrivalFacts.filter(_.id === arrival_fact.id).update(formToUpdate)).map(_ => ())
  }

  def delete(id: Long): Future[Unit] = db.run(ArrivalFacts.filter(_.id === id).delete).map(_ => ())

  def find(id: Long): Future[Option[ArrivalFact]] = {
    db.run(ArrivalFacts.filter(_.id.get === id).result.headOption)
  }

  def findBySession(session_id: Long): Future[Option[ArrivalFact]] = {
    db.run(ArrivalFacts.filter(_.session_id.get === session_id).result.headOption)
  }

  def findByDateRange(start_date: DateTime, end_date: DateTime): Future[Seq[ArrivalFact]] = {
    db.run(ArrivalFacts.filter(fact => fact.created_at >= start_date && fact.created_at <= end_date).result)
  }

  private class ArrivalFactTable(tag: Tag) extends Table[ArrivalFact](tag, "arrival_facts") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)

    def ab_tests = column[Option[String]]("ab_tests")

    def adgroupid = column[Option[String]]("adgroupid")

    def git_hash = column[Option[String]]("git_hash")

    def arrivals = column[Int]("arrivals")

    def browser = column[Option[String]]("browser")

    def browser_id = column[Option[String]]("browser_id")

    def browser_version = column[Option[String]]("browser_version")

    def bounce = column[Int]("bounce")

    def conf = column[Option[Int]]("conf")

    def conu = column[Option[Int]]("conu")

    def conversion = column[Option[Int]]("conversion")

    def conversion_count = column[Option[Int]]("conversion_count")

    def conversion_page = column[Option[String]]("conversion_page")

    def created_at = column[Option[DateTime]]("created_at")

    def day_of_week = column[Option[String]]("day_of_week")

    def device_brand = column[Option[String]]("device_brand")

    def device_model = column[Option[String]]("device_model")

    def duration = column[Int]("duration")

    def device_name = column[Option[String]]("device_name")

    def device_type = column[Option[String]]("device_type")

    def electric_bill = column[Option[String]]("electric_bill")

    def email = column[Option[String]]("email")

    def entry_page = column[Option[String]]("entry_page")

    def entry_url = column[Option[String]]("entry_url")

    def exit_url = column[Option[String]]("exit_url")

    def event_category = column[Option[String]]("event_category")

    def events_count = column[Int]("events_count")

    def form_city = column[Option[String]]("form_city")

    def form_state = column[Option[String]]("form_state")

    def form_zip = column[Option[String]]("form_zip")

    def gclid = column[Option[String]]("gclid")

    def ip_address = column[Option[String]]("ip_address")

    def ip_blacklisted = column[Option[Int]]("ip_blacklisted")

    def is_ip_blacklisted = column[Option[Int]]("is_ip_blacklisted")

    def lp_conv_u = column[Option[Int]]("lp_conv_u")

    def last_activity = column[Option[DateTime]]("last_activity")

    def local_hour = column[Option[Int]]("local_hour")

    def maxmind_zip = column[Option[String]]("maxmind_zip")

    def name_capture = column[Int]("name_capture")

    def new_ip = column[Int]("new_ip")

    def new_session = column[Int]("new_session")

    def os_name = column[Option[String]]("os_name")

    def os_version = column[Option[String]]("os_version")

    def page_views = column[Option[Int]]("page_views")

    def prop_own = column[Option[String]]("prop_own")

    def revenue = column[Option[Double]]("revenue")

    def robot_id = column[Option[String]]("robot_id")

    def session_id = column[Option[Long]]("session_id")

    def user_agent = column[Option[String]]("user_agent")

    def utm_source = column[Option[String]]("utm_source")

    def utm_campaign = column[Option[String]]("utm_campaign")

    def utm_medium = column[Option[String]]("utm_medium")

    def lp_ctc = column[Int]("lp_ctc")

    def form_step_1 = column[Int]("form_step_1")

    def form_step_2 = column[Int]("form_step_2")

    def form_step_3 = column[Int]("form_step_3")

    def form_complete = column[Int]("form_complete")

    def lp_content_engage = column[Int]("lp_content_engage")

    def page_closed = column[Int]("page_closed")

    def page_loaded = column[Int]("page_loaded")

    def form_step_2b = column[Int]("form_step_2b")

    def page_rendered = column[Int]("page_rendered")

    def form_step_4 = column[Int]("form_step_4")

    def form_step_5 = column[Int]("form_step_5")

    def form_step_6 = column[Int]("form_step_6")

    def form_step_7 = column[Int]("form_step_7")

    def form_step_8 = column[Int]("form_step_8")

    def maxmind_failure_on_page_load = column[Int]("maxmind_failure_on_page_load")

    def landing_page_completed = column[Int]("landing_page_completed")

    def address_completed = column[Int]("address_completed")

    def ownership_completed = column[Int]("ownership_completed")

    def power_bill_completed = column[Int]("power_bill_completed")

    def power_company_completed = column[Int]("power_company_completed")

    def name_completed = column[Int]("name_completed")

    def email_completed = column[Int]("email_completed")

    def phone_completed = column[Int]("phone_completed")

    def email_modal_completed = column[Int]("email_modal_completed")

    def credit_score_completed = column[Int]("credit_score_completed")

    def creative = column[Option[String]]("creative")

    def page_focus = column[Int]("page_focus")

    def page_blur = column[Int]("page_blur")

    def u_lp_ctc = column[Int]("u_lp_ctc")

    def u_form_step_1 = column[Int]("u_form_step_1")

    def u_form_step_2 = column[Int]("u_form_step_2")

    def u_form_step_3 = column[Int]("u_form_step_3")

    def u_form_complete = column[Int]("u_form_complete")

    def u_lp_content_engage = column[Int]("u_lp_content_engage")

    def u_page_closed = column[Int]("u_page_closed")

    def u_page_loaded = column[Int]("u_page_loaded")

    def u_form_step_2b = column[Int]("u_form_step_2b")

    def u_page_rendered = column[Int]("u_page_rendered")

    def u_form_step_4 = column[Int]("u_form_step_4")

    def u_form_step_5 = column[Int]("u_form_step_5")

    def u_form_step_6 = column[Int]("u_form_step_6")

    def u_form_step_7 = column[Int]("u_form_step_7")

    def u_form_step_8 = column[Int]("u_form_step_8")

    def u_maxmind_failure_on_page_load = column[Int]("u_maxmind_failure_on_page_load")

    def u_landing_page_completed = column[Int]("u_landing_page_completed")

    def u_address_completed = column[Int]("u_address_completed")

    def u_ownership_completed = column[Int]("u_ownership_completed")

    def u_power_bill_completed = column[Int]("u_power_bill_completed")

    def u_power_company_completed = column[Int]("u_power_company_completed")

    def u_name_completed = column[Int]("u_name_completed")

    def u_email_completed = column[Int]("u_email_completed")

    def u_phone_completed = column[Int]("u_phone_completed")

    def u_email_modal_completed = column[Int]("u_email_modal_completed")

    def u_credit_score_completed = column[Int]("u_credit_score_completed")

    def u_page_focus = column[Int]("u_page_focus")

    def u_page_blur = column[Int]("u_page_blur")

    def keyword = column[Option[String]]("keyword")

    def g_network = column[Option[String]]("g_network")

    def g_device = column[Option[String]]("g_device")

    def g_location = column[Option[String]]("g_location")

    private type ArrivalFactTrafficSourceType =
      (Option[String], Option[String], Option[Int], Option[String], Option[String], Option[String], Option[String], Option[Int], Option[Int], Option[Int], Option[Int], Option[String], Int, Int, Int, Option[String], Option[String], Option[String])

    private type ArrivalFactDeviceType =
      (Option[String], Option[String], Option[String], Option[String])

    private type ArrivalFactBrowserType =
      (Option[String], Option[String], Option[String], Int, Int, Option[String], Option[String])

    private type ArrivalFactSessionType =
      (Option[Long], Option[String], Option[Int], Option[Int], Option[String], Int, Int, Int, Option[String], Option[String], Option[String], Option[String], Option[DateTime], Option[String], Int, Option[String], Option[String], Option[String], Option[DateTime], Option[Int], Option[Int], Option[Double])

    private type ArrivalFactFormType =
      (Option[String], Option[String], Option[String], Option[String], Option[String], Option[String])

    private type ArrivalFactEvent1Type =
      (Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Option[String], Int, Int, Option[String], Int, Int, Int, Int, Int, Int, Int)

    private type ArrivalFactEvent2Type =
      (Int, Int, Int, Int, Int, Int, Int, Int)

    private type ArrivalFactUEvent1Type =
      (Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)

    private type ArrivalFactUEvent2Type =
      (Int, Int, Int, Int)

    private type ArrivalFactTupleType =
      (
        Option[Long],
        ArrivalFactTrafficSourceType,
        ArrivalFactDeviceType,
        ArrivalFactBrowserType,
        ArrivalFactSessionType,
        ArrivalFactFormType,
        ArrivalFactEvent1Type,
        ArrivalFactEvent2Type,
        ArrivalFactUEvent1Type,
        ArrivalFactUEvent2Type
      )

    private val formShapedValue =
      (
        id,
        (adgroupid, gclid, lp_conv_u, keyword, g_network, g_device, g_location, conf, conu, conversion, conversion_count, conversion_page, lp_ctc, u_lp_ctc, u_lp_content_engage, utm_source, utm_campaign, utm_medium),
        (device_name, device_type, device_brand, device_model),
        (browser, browser_id, browser_version, arrivals, bounce, robot_id, user_agent),
        (session_id, ip_address, ip_blacklisted, is_ip_blacklisted, maxmind_zip, name_capture, new_ip, new_session, os_name, os_version, ab_tests, git_hash, created_at, day_of_week, duration, entry_page, entry_url, exit_url, last_activity, local_hour, page_views, revenue),
        (email, form_city, form_state, form_zip, electric_bill, prop_own),
        (maxmind_failure_on_page_load, landing_page_completed, address_completed, ownership_completed, power_bill_completed, power_company_completed, name_completed, email_completed, phone_completed, email_modal_completed, credit_score_completed, creative, page_focus, page_blur, event_category, events_count, form_step_1, form_step_2, form_step_3, form_step_2b, page_rendered, form_step_4),
        (form_step_7, form_step_8, lp_content_engage, form_complete, page_closed, page_loaded, form_step_5, form_step_6),
        (u_page_closed, u_page_loaded, u_form_step_2b, u_page_rendered, u_form_step_1, u_form_step_2, u_form_step_3, u_form_step_4, u_form_step_5, u_maxmind_failure_on_page_load, u_landing_page_completed, u_address_completed, u_ownership_completed, u_power_bill_completed, u_power_company_completed, u_name_completed, u_email_completed, u_phone_completed, u_email_modal_completed, u_credit_score_completed, u_page_focus, u_page_blur),
        (u_form_step_6, u_form_step_7, u_form_step_8, u_form_complete)
      ).shaped
    //
    private val toModel: ArrivalFactTupleType => ArrivalFact = { arrivalFactTuple =>
      ArrivalFact(
        id = arrivalFactTuple._1,
        traffic_source = ArrivalFactTrafficSource.tupled.apply(arrivalFactTuple._2),
        device = ArrivalFactDevice.tupled.apply(arrivalFactTuple._3),
        browser = ArrivalFactBrowser.tupled.apply(arrivalFactTuple._4),
        session = ArrivalFactSession.tupled.apply(arrivalFactTuple._5),
        form = ArrivalFactForm.tupled.apply(arrivalFactTuple._6),
        event_1 = ArrivalFactEvent1.tupled.apply(arrivalFactTuple._7),
        event_2 = ArrivalFactEvent2.tupled.apply(arrivalFactTuple._8),
        u_event_1 = ArrivalFactUEvent1.tupled.apply(arrivalFactTuple._9),
        u_event_2 = ArrivalFactUEvent2.tupled.apply(arrivalFactTuple._10)
      )
    }
    private val toTuple: ArrivalFact => Option[ArrivalFactTupleType] = { arrival_fact =>
      Some {
        (
          arrival_fact.id,
          ArrivalFactTrafficSource.unapply(arrival_fact.traffic_source).get,
          ArrivalFactDevice.unapply(arrival_fact.device).get,
          ArrivalFactBrowser.unapply(arrival_fact.browser).get,
          ArrivalFactSession.unapply(arrival_fact.session).get,
          ArrivalFactForm.unapply(arrival_fact.form).get,
          ArrivalFactEvent1.unapply(arrival_fact.event_1).get,
          ArrivalFactEvent2.unapply(arrival_fact.event_2).get,
          ArrivalFactUEvent1.unapply(arrival_fact.u_event_1).get,
          ArrivalFactUEvent2.unapply(arrival_fact.u_event_2).get
        )
      }
    }

    def * = formShapedValue <> (toModel, toTuple)
  }

}