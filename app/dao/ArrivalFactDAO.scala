package dao

import com.github.tototoshi.slick.MySQLJodaSupport._
import models.mysql._
import org.joda.time.DateTime
import play.api.db.slick.HasDatabaseConfig
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.Future

class ArrivalFactDAO(protected val dbConfig: DatabaseConfig[JdbcProfile]) extends HasDatabaseConfig[JdbcProfile] with Controller {
  import driver.api._

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
    db.run(ArrivalFacts.filter(fact => fact.created_at >= start_date || fact.created_at <= end_date).result)
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
      ).shaped[ArrivalFactTupleType]
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
        /*traffic_source = ArrivalFactTrafficSource(
        adgroupid = arrivalFactTuple._2._1,
        gclid = arrivalFactTuple._2._2,
        lp_conv_u = arrivalFactTuple._2._3,
        keyword = arrivalFactTuple._2._4,
        g_network = arrivalFactTuple._2._5,
        g_device = arrivalFactTuple._2._6,
        g_location = arrivalFactTuple._2._7,
        conf = arrivalFactTuple._2._8,
        conu = arrivalFactTuple._2._9,
        conversion = arrivalFactTuple._2._10,
        conversion_count = arrivalFactTuple._2._11,
        conversion_page = arrivalFactTuple._2._12,
        lp_ctc = arrivalFactTuple._2._13,
        u_lp_ctc = arrivalFactTuple._2._14,
        u_lp_content_engage = arrivalFactTuple._2._15,
        utm_source = arrivalFactTuple._2._16,
        utm_campaign = arrivalFactTuple._2._17,
        utm_medium = arrivalFactTuple._2._18
      ),
      device = ArrivalFactDevice(
        device_name = arrivalFactTuple._3._1,
        device_type = arrivalFactTuple._3._2,
        device_brand = arrivalFactTuple._3._3,
        device_model = arrivalFactTuple._3._4
      ),
      browser = ArrivalFactBrowser(
        browser = arrivalFactTuple._4._1,
        browser_id = arrivalFactTuple._4._2,
        browser_version = arrivalFactTuple._4._3,
        arrivals = arrivalFactTuple._4._4,
        bounce = arrivalFactTuple._4._5,
        robot_id = arrivalFactTuple._4._6
      ),
      session = ArrivalFactSession(
        session_id = arrivalFactTuple._5._1,
        ip_address = arrivalFactTuple._5._2,
        ip_blacklisted = arrivalFactTuple._5._3,
        is_ip_blacklisted = arrivalFactTuple._5._4,
        maxmind_zip = arrivalFactTuple._5._5,
        name_capture = arrivalFactTuple._5._6,
        new_ip = arrivalFactTuple._5._7,
        new_session = arrivalFactTuple._5._8,
        os_name = arrivalFactTuple._5._9,
        os_version = arrivalFactTuple._5._10,
        ab_tests = arrivalFactTuple._5._11,
        git_hash = arrivalFactTuple._5._12,
        created_at = arrivalFactTuple._5._13,
        day_of_week = arrivalFactTuple._5._14,
        duration = arrivalFactTuple._5._15,
        entry_page = arrivalFactTuple._5._16,
        entry_url = arrivalFactTuple._5._17,
        exit_url = arrivalFactTuple._5._18,
        last_activity = arrivalFactTuple._5._19,
        local_hour = arrivalFactTuple._5._20,
        page_views = arrivalFactTuple._5._21,
        revenue = arrivalFactTuple._5._22
      ),
      form = ArrivalFactForm(
        email = arrivalFactTuple._6._1,
        form_city = arrivalFactTuple._6._2,
        form_state = arrivalFactTuple._6._3,
        form_zip = arrivalFactTuple._6._4,
        electric_bill = arrivalFactTuple._6._5,
        prop_own = arrivalFactTuple._6._6
      ),
      event_1 = ArrivalFactEvent1(
        maxmind_failure_on_page_load = arrivalFactTuple._7._1,
        landing_page_completed = arrivalFactTuple._7._2,
        address_completed = arrivalFactTuple._7._3,
        ownership_completed = arrivalFactTuple._7._4,
        power_bill_completed = arrivalFactTuple._7._5,
        power_company_completed = arrivalFactTuple._7._6,
        name_completed = arrivalFactTuple._7._7,
        email_completed = arrivalFactTuple._7._8,
        phone_completed = arrivalFactTuple._7._9,
        email_modal_completed = arrivalFactTuple._7._10,
        credit_score_completed = arrivalFactTuple._7._11,
        creative = arrivalFactTuple._7._12,
        page_focus = arrivalFactTuple._7._13,
        page_blur = arrivalFactTuple._7._14,
        event_category = arrivalFactTuple._7._15,
        events_count = arrivalFactTuple._7._16,
        form_step_1 = arrivalFactTuple._7._17,
        form_step_2 = arrivalFactTuple._7._18,
        form_step_3 = arrivalFactTuple._7._19,
        form_step_2b = arrivalFactTuple._7._20,
        page_rendered = arrivalFactTuple._7._21,
        form_step_4 = arrivalFactTuple._7._22
      ),
      event_2 = ArrivalFactEvent2(
        form_step_7 = arrivalFactTuple._8._1,
        form_step_8 = arrivalFactTuple._8._2,
        lp_content_engage = arrivalFactTuple._8._3,
        form_complete = arrivalFactTuple._8._4,
        page_closed = arrivalFactTuple._8._5,
        page_loaded = arrivalFactTuple._8._6,
        form_step_5 = arrivalFactTuple._8._7,
        form_step_6 = arrivalFactTuple._8._8
      ),
      u_event_1 = ArrivalFactUEvent1(
        u_page_closed = arrivalFactTuple._9._1,
        u_page_loaded = arrivalFactTuple._9._2,
        u_form_step_2b = arrivalFactTuple._9._3,
        u_page_rendered = arrivalFactTuple._9._4,
        u_form_step_1 = arrivalFactTuple._9._5,
        u_form_step_2 = arrivalFactTuple._9._6,
        u_form_step_3 = arrivalFactTuple._9._7,
        u_form_step_4 = arrivalFactTuple._9._8,
        u_form_step_5 = arrivalFactTuple._9._9,
        u_maxmind_failure_on_page_load = arrivalFactTuple._9._10,
        u_landing_page_completed = arrivalFactTuple._9._11,
        u_address_completed = arrivalFactTuple._9._12,
        u_ownership_completed = arrivalFactTuple._9._13,
        u_power_bill_completed = arrivalFactTuple._9._14,
        u_power_company_completed = arrivalFactTuple._9._15,
        u_name_completed = arrivalFactTuple._9._16,
        u_email_completed = arrivalFactTuple._9._17,
        u_phone_completed = arrivalFactTuple._9._18,
        u_email_modal_completed = arrivalFactTuple._9._19,
        u_credit_score_completed = arrivalFactTuple._9._20,
        u_page_focus = arrivalFactTuple._9._21,
        u_page_blur = arrivalFactTuple._9._22
      ),
      u_event_2 = ArrivalFactUEvent2(
        u_form_step_6 = arrivalFactTuple._10._1,
        u_form_step_7 = arrivalFactTuple._10._2,
        u_form_step_8 = arrivalFactTuple._10._3,
        u_form_complete = arrivalFactTuple._10._4
      )*/
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

          /*(
          arrival_fact.traffic_source.adgroupid,
          arrival_fact.traffic_source.gclid,
          arrival_fact.traffic_source.lp_conv_u,
          arrival_fact.traffic_source.keyword,
          arrival_fact.traffic_source.g_network,
          arrival_fact.traffic_source.g_device,
          arrival_fact.traffic_source.g_location,
          arrival_fact.traffic_source.conf,
          arrival_fact.traffic_source.conu,
          arrival_fact.traffic_source.conversion,
          arrival_fact.traffic_source.conversion_count,
          arrival_fact.traffic_source.conversion_page,
          arrival_fact.traffic_source.lp_ctc,
          arrival_fact.traffic_source.u_lp_ctc,
          arrival_fact.traffic_source.u_lp_content_engage,
          arrival_fact.traffic_source.utm_source,
          arrival_fact.traffic_source.utm_campaign,
          arrival_fact.traffic_source.utm_medium
        ),
        (
          arrival_fact.device.device_name,
          arrival_fact.device.device_type,
          arrival_fact.device.device_brand,
          arrival_fact.device.device_model
        ),
        (
          arrival_fact.browser.browser,
          arrival_fact.browser.browser_id,
          arrival_fact.browser.browser_version,
          arrival_fact.browser.arrivals,
          arrival_fact.browser.bounce,
          arrival_fact.browser.robot_id
        ),
        (
          arrival_fact.session.session_id,
          arrival_fact.session.ip_address,
          arrival_fact.session.ip_blacklisted,
          arrival_fact.session.is_ip_blacklisted,
          arrival_fact.session.maxmind_zip,
          arrival_fact.session.name_capture,
          arrival_fact.session.new_ip,
          arrival_fact.session.new_session,
          arrival_fact.session.os_name,
          arrival_fact.session.os_version,
          arrival_fact.session.ab_tests,
          arrival_fact.session.git_hash,
          arrival_fact.session.created_at: Option[DateTime],
          arrival_fact.session.day_of_week,
          arrival_fact.session.duration,
          arrival_fact.session.entry_page,
          arrival_fact.session.entry_url,
          arrival_fact.session.exit_url,
          arrival_fact.session.last_activity: Option[DateTime],
          arrival_fact.session.local_hour,
          arrival_fact.session.page_views,
          arrival_fact.session.revenue
        ),
        (
          arrival_fact.form.email,
          arrival_fact.form.form_city,
          arrival_fact.form.form_state,
          arrival_fact.form.form_zip,
          arrival_fact.form.electric_bill,
          arrival_fact.form.prop_own
        ),
        (
          arrival_fact.event_1.maxmind_failure_on_page_load,
          arrival_fact.event_1.landing_page_completed,
          arrival_fact.event_1.address_completed,
          arrival_fact.event_1.ownership_completed,
          arrival_fact.event_1.power_bill_completed,
          arrival_fact.event_1.power_company_completed,
          arrival_fact.event_1.name_completed,
          arrival_fact.event_1.email_completed,
          arrival_fact.event_1.phone_completed,
          arrival_fact.event_1.email_modal_completed,
          arrival_fact.event_1.credit_score_completed,
          arrival_fact.event_1.creative,
          arrival_fact.event_1.page_focus,
          arrival_fact.event_1.page_blur,
          arrival_fact.event_1.event_category,
          arrival_fact.event_1.events_count,
          arrival_fact.event_1.form_step_1,
          arrival_fact.event_1.form_step_2,
          arrival_fact.event_1.form_step_3,
          arrival_fact.event_1.form_step_2b,
          arrival_fact.event_1.page_rendered,
          arrival_fact.event_1.form_step_4
        ),
        (
          arrival_fact.event_2.form_step_7,
          arrival_fact.event_2.form_step_8,
          arrival_fact.event_2.lp_content_engage,
          arrival_fact.event_2.form_complete,
          arrival_fact.event_2.page_closed,
          arrival_fact.event_2.page_loaded,
          arrival_fact.event_2.form_step_5,
          arrival_fact.event_2.form_step_6
        ),
        (
          arrival_fact.u_event_1.u_page_closed,
          arrival_fact.u_event_1.u_page_loaded,
          arrival_fact.u_event_1.u_form_step_2b,
          arrival_fact.u_event_1.u_page_rendered,
          arrival_fact.u_event_1.u_form_step_1,
          arrival_fact.u_event_1.u_form_step_2,
          arrival_fact.u_event_1.u_form_step_3,
          arrival_fact.u_event_1.u_form_step_4,
          arrival_fact.u_event_1.u_form_step_5,
          arrival_fact.u_event_1.u_maxmind_failure_on_page_load,
          arrival_fact.u_event_1.u_landing_page_completed,
          arrival_fact.u_event_1.u_address_completed,
          arrival_fact.u_event_1.u_ownership_completed,
          arrival_fact.u_event_1.u_power_bill_completed,
          arrival_fact.u_event_1.u_power_company_completed,
          arrival_fact.u_event_1.u_name_completed,
          arrival_fact.u_event_1.u_email_completed,
          arrival_fact.u_event_1.u_phone_completed,
          arrival_fact.u_event_1.u_email_modal_completed,
          arrival_fact.u_event_1.u_credit_score_completed,
          arrival_fact.u_event_1.u_page_focus,
          arrival_fact.u_event_1.u_page_blur
        ),
        (
          arrival_fact.u_event_2.u_form_step_6,
          arrival_fact.u_event_2.u_form_step_7,
          arrival_fact.u_event_2.u_form_step_8,
          arrival_fact.u_event_2.u_form_complete
        )*/
        )
      }
    }

    def * = formShapedValue <> (toModel, toTuple)
  }

}