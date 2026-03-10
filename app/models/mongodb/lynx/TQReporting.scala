package models.mongodb.lynx

import Shared.Shared._
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.MongoExtensions._
import models.mysql._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

object TQReporting {
  // Initialized by StartupTasks from MongoService
  var arrivalFactCollection: MongoCollection[Document] = _

  val dtf: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

  def arrivalFactToDocument(arrivalFact: ArrivalFact): Document = {
    Document(
      "id" -> arrivalFact.id,
      "ab_tests" -> arrivalFact.session.ab_tests,
      "adgroupid" -> arrivalFact.traffic_source.adgroupid,
      "git_hash" -> arrivalFact.session.git_hash,
      "arrivals" -> arrivalFact.browser.arrivals,
      "browser" -> arrivalFact.browser.browser,
      "browser_id" -> arrivalFact.browser.browser_id,
      "browser_version" -> arrivalFact.browser.browser_version,
      "bounce" -> arrivalFact.browser.bounce,
      "conf" -> arrivalFact.traffic_source.conf,
      "conu" -> arrivalFact.traffic_source.conu,
      "conversion" -> arrivalFact.traffic_source.conversion,
      "conversion_count" -> arrivalFact.traffic_source.conversion_count,
      "conversion_page" -> arrivalFact.traffic_source.conversion_page,
      "created_at" -> dtOptToStrOpt(arrivalFact.session.created_at),
      "day_of_week" -> arrivalFact.session.day_of_week,
      "device_brand" -> arrivalFact.device.device_brand,
      "device_model" -> arrivalFact.device.device_model,
      "duration" -> arrivalFact.session.duration,
      "device_name" -> arrivalFact.device.device_name,
      "device_type" -> arrivalFact.device.device_type,
      "electric_bill" -> arrivalFact.form.electric_bill,
      "email" -> arrivalFact.form.email,
      "entry_page" -> arrivalFact.session.entry_page,
      "entry_url" -> arrivalFact.session.entry_url,
      "exit_url" -> arrivalFact.session.exit_url,
      "event_category" -> arrivalFact.event_1.event_category,
      "events_count" -> arrivalFact.event_1.events_count,
      "form_city" -> arrivalFact.form.form_city,
      "form_state" -> arrivalFact.form.form_state,
      "form_zip" -> arrivalFact.form.form_zip,
      "gclid" -> arrivalFact.traffic_source.gclid,
      "ip_address" -> arrivalFact.session.ip_address,
      "ip_blacklisted" -> arrivalFact.session.ip_blacklisted,
      "is_ip_blacklisted" -> arrivalFact.session.is_ip_blacklisted,
      "lp_conv_u" -> arrivalFact.traffic_source.lp_conv_u,
      "last_activity" -> dtOptToStrOpt(arrivalFact.session.last_activity),
      "local_hour" -> arrivalFact.session.local_hour,
      "maxmind_zip" -> arrivalFact.session.maxmind_zip,
      "name_capture" -> arrivalFact.session.name_capture,
      "new_ip" -> arrivalFact.session.new_ip,
      "new_session" -> arrivalFact.session.new_session,
      "os_name" -> arrivalFact.session.os_name,
      "os_version" -> arrivalFact.session.os_version,
      "page_views" -> arrivalFact.session.page_views,
      "prop_own" -> arrivalFact.form.prop_own,
      "revenue" -> arrivalFact.session.revenue,
      "robot_id" -> arrivalFact.browser.robot_id,
      "user_agent" -> arrivalFact.browser.user_agent,
      "session_id" -> arrivalFact.session.session_id,
      "utm_source" -> arrivalFact.traffic_source.utm_source,
      "utm_campaign" -> arrivalFact.traffic_source.utm_campaign,
      "utm_medium" -> arrivalFact.traffic_source.utm_medium,
      "lp_ctc" -> arrivalFact.traffic_source.lp_ctc,
      "form_step_1" -> arrivalFact.event_1.form_step_1,
      "form_step_2" -> arrivalFact.event_1.form_step_2,
      "form_step_3" -> arrivalFact.event_1.form_step_3,
      "form_complete" -> arrivalFact.event_2.form_complete,
      "lp_content_engage" -> arrivalFact.event_2.lp_content_engage,
      "page_closed" -> arrivalFact.event_2.page_closed,
      "page_loaded" -> arrivalFact.event_2.page_loaded,
      "form_step_2b" -> arrivalFact.event_1.form_step_2b,
      "page_rendered" -> arrivalFact.event_1.page_rendered,
      "form_step_4" -> arrivalFact.event_1.form_step_4,
      "form_step_5" -> arrivalFact.event_2.form_step_5,
      "form_step_6" -> arrivalFact.event_2.form_step_6,
      "form_step_7" -> arrivalFact.event_2.form_step_7,
      "form_step_8" -> arrivalFact.event_2.form_step_8,
      "maxmind_failure_on_page_load" -> arrivalFact.event_1.maxmind_failure_on_page_load,
      "landing_page_completed" -> arrivalFact.event_1.landing_page_completed,
      "address_completed" -> arrivalFact.event_1.address_completed,
      "ownership_completed" -> arrivalFact.event_1.ownership_completed,
      "power_bill_completed" -> arrivalFact.event_1.power_bill_completed,
      "power_company_completed" -> arrivalFact.event_1.power_company_completed,
      "name_completed" -> arrivalFact.event_1.name_completed,
      "email_completed" -> arrivalFact.event_1.email_completed,
      "phone_completed" -> arrivalFact.event_1.phone_completed,
      "email_modal_completed" -> arrivalFact.event_1.email_modal_completed,
      "credit_score_completed" -> arrivalFact.event_1.credit_score_completed,
      "creative" -> arrivalFact.event_1.creative,
      "page_focus" -> arrivalFact.event_1.page_focus,
      "page_blur" -> arrivalFact.event_1.page_blur,
      "u_lp_ctc" -> arrivalFact.traffic_source.u_lp_ctc,
      "u_form_step_1" -> arrivalFact.u_event_1.u_form_step_1,
      "u_form_step_2" -> arrivalFact.u_event_1.u_form_step_2,
      "u_form_step_3" -> arrivalFact.u_event_1.u_form_step_3,
      "u_form_complete" -> arrivalFact.u_event_2.u_form_complete,
      "u_lp_content_engage" -> arrivalFact.traffic_source.u_lp_content_engage,
      "u_page_closed" -> arrivalFact.u_event_1.u_page_closed,
      "u_page_loaded" -> arrivalFact.u_event_1.u_page_loaded,
      "u_form_step_2b" -> arrivalFact.u_event_1.u_form_step_2b,
      "u_page_rendered" -> arrivalFact.u_event_1.u_page_rendered,
      "u_form_step_4" -> arrivalFact.u_event_1.u_form_step_4,
      "u_form_step_5" -> arrivalFact.u_event_1.u_form_step_5,
      "u_form_step_6" -> arrivalFact.u_event_2.u_form_step_6,
      "u_form_step_7" -> arrivalFact.u_event_2.u_form_step_7,
      "u_form_step_8" -> arrivalFact.u_event_2.u_form_step_8,
      "u_maxmind_failure_on_page_load" -> arrivalFact.u_event_1.u_maxmind_failure_on_page_load,
      "u_landing_page_completed" -> arrivalFact.u_event_1.u_landing_page_completed,
      "u_address_completed" -> arrivalFact.u_event_1.u_address_completed,
      "u_ownership_completed" -> arrivalFact.u_event_1.u_ownership_completed,
      "u_power_bill_completed" -> arrivalFact.u_event_1.u_power_bill_completed,
      "u_power_company_completed" -> arrivalFact.u_event_1.u_power_company_completed,
      "u_name_completed" -> arrivalFact.u_event_1.u_name_completed,
      "u_email_completed" -> arrivalFact.u_event_1.u_email_completed,
      "u_phone_completed" -> arrivalFact.u_event_1.u_phone_completed,
      "u_email_modal_completed" -> arrivalFact.u_event_1.u_email_modal_completed,
      "u_credit_score_completed" -> arrivalFact.u_event_1.u_credit_score_completed,
      "u_page_focus" -> arrivalFact.u_event_1.u_page_focus,
      "u_page_blur" -> arrivalFact.u_event_1.u_page_blur,
      "keyword" -> arrivalFact.traffic_source.keyword,
      "g_network" -> arrivalFact.traffic_source.g_network,
      "g_device" -> arrivalFact.traffic_source.g_device,
      "g_location" -> arrivalFact.traffic_source.g_location
    )
  }

  def documentToArrivalFact(doc: Document): ArrivalFact =
    ArrivalFact(
      id = Option(doc.getLong("id")),
      traffic_source = ArrivalFactTrafficSource(
        adgroupid = Option(doc.getString("adgroupid")),
        gclid = Option(doc.getString("gclid")),
        lp_conv_u = Option(doc.getInteger("lp_conv_u")),
        keyword = Option(doc.getString("keyword")),
        g_network = Option(doc.getString("g_network")),
        g_device = Option(doc.getString("g_device")),
        g_location = Option(doc.getString("g_location")),
        conf = Option(doc.getInteger("conf")),
        conu = Option(doc.getInteger("conu")),
        conversion = Option(doc.getInteger("conversion")),
        conversion_count = Option(doc.getInteger("conversion_count")),
        conversion_page = Option(doc.getString("conversion_page")),
        lp_ctc = doc.getInteger("lp_ctc"),
        u_lp_ctc = doc.getInteger("u_lp_ctc"),
        u_lp_content_engage = doc.getInteger("u_lp_content_engage"),
        utm_source = Option(doc.getString("utm_source")),
        utm_campaign = Option(doc.getString("utm_campaign")),
        utm_medium = Option(doc.getString("utm_medium"))
      ),
      device = ArrivalFactDevice(
        device_name = Option(doc.getString("device_name")),
        device_type = Option(doc.getString("device_type")),
        device_brand = Option(doc.getString("device_brand")),
        device_model = Option(doc.getString("device_model"))
      ),
      browser = ArrivalFactBrowser(
        browser = Option(doc.getString("browser")),
        browser_id = Option(doc.getString("browser_id")),
        browser_version = Option(doc.getString("browser_version")),
        arrivals = doc.getInteger("arrivals"),
        bounce = doc.getInteger("bounce"),
        robot_id = Option(doc.getString("robot_id")),
        user_agent = Option(doc.getString("user_agent"))
      ),
      session = ArrivalFactSession(
        session_id = Option(doc.getLong("session_id")),
        ip_address = Option(doc.getString("ip_address")),
        ip_blacklisted = Option(doc.getInteger("ip_blacklisted")),
        is_ip_blacklisted = Option(doc.getInteger("is_ip_blacklisted")),
        maxmind_zip = Option(doc.getString("maxmind_zip")),
        name_capture = doc.getInteger("name_capture"),
        new_ip = doc.getInteger("new_ip"),
        new_session = doc.getInteger("new_session"),
        os_name = Option(doc.getString("os_name")),
        os_version = Option(doc.getString("os_version")),
        ab_tests = Option(doc.getString("ab_tests")),
        git_hash = Option(doc.getString("git_hash")),
        created_at = strOptToDtOpt(Option(doc.getString("created_at"))),
        day_of_week = Option(doc.getString("day_of_week")),
        duration = doc.getInteger("duration"),
        entry_page = Option(doc.getString("entry_page")),
        entry_url = Option(doc.getString("entry_url")),
        exit_url = Option(doc.getString("exit_url")),
        last_activity = strOptToDtOpt(Option(doc.getString("last_activity"))),
        local_hour = Option(doc.getInteger("local_hour")),
        page_views = Option(doc.getInteger("page_views")),
        revenue = Option(doc.getDouble("revenue"))
      ),
      form = ArrivalFactForm(
        email = Option(doc.getString("email")),
        form_city = Option(doc.getString("form_city")),
        form_state = Option(doc.getString("form_state")),
        form_zip = Option(doc.getString("form_zip")),
        electric_bill = Option(doc.getString("electric_bill")),
        prop_own = Option(doc.getString("prop_own"))
      ),
      event_1 = ArrivalFactEvent1(
        maxmind_failure_on_page_load = doc.getInteger("maxmind_failure_on_page_load"),
        landing_page_completed = doc.getInteger("landing_page_completed"),
        address_completed = doc.getInteger("address_completed"),
        ownership_completed = doc.getInteger("ownership_completed"),
        power_bill_completed = doc.getInteger("power_bill_completed"),
        power_company_completed = doc.getInteger("power_company_completed"),
        name_completed = doc.getInteger("name_completed"),
        email_completed = doc.getInteger("email_completed"),
        phone_completed = doc.getInteger("phone_completed"),
        email_modal_completed = doc.getInteger("email_modal_completed"),
        credit_score_completed = doc.getInteger("credit_score_completed"),
        creative = Option(doc.getString("creative")),
        page_focus = doc.getInteger("page_focus"),
        page_blur = doc.getInteger("page_blur"),
        event_category = Option(doc.getString("event_category")),
        events_count = doc.getInteger("events_count"),
        form_step_1 = doc.getInteger("form_step_1"),
        form_step_2 = doc.getInteger("form_step_2"),
        form_step_3 = doc.getInteger("form_step_3"),
        form_step_2b = doc.getInteger("form_step_2b"),
        page_rendered = doc.getInteger("page_rendered"),
        form_step_4 = doc.getInteger("form_step_4")
      ),
      event_2 = ArrivalFactEvent2(
        form_step_7 = doc.getInteger("form_step_7"),
        form_step_8 = doc.getInteger("form_step_8"),
        lp_content_engage = doc.getInteger("lp_content_engage"),
        form_complete = doc.getInteger("form_complete"),
        page_closed = doc.getInteger("page_closed"),
        page_loaded = doc.getInteger("page_loaded"),
        form_step_5 = doc.getInteger("form_step_5"),
        form_step_6 = doc.getInteger("form_step_6")
      ),
      u_event_1 = ArrivalFactUEvent1(
        u_page_closed = doc.getInteger("u_page_closed"),
        u_page_loaded = doc.getInteger("u_page_loaded"),
        u_form_step_2b = doc.getInteger("u_form_step_2b"),
        u_page_rendered = doc.getInteger("u_form_rendered"),
        u_form_step_1 = doc.getInteger("u_form_step_1"),
        u_form_step_2 = doc.getInteger("u_form_step_2"),
        u_form_step_3 = doc.getInteger("u_form_step_3"),
        u_form_step_4 = doc.getInteger("u_form_step_4"),
        u_form_step_5 = doc.getInteger("u_form_step_5"),
        u_maxmind_failure_on_page_load = doc.getInteger("u_maxmind_failure_on_page_load"),
        u_landing_page_completed = doc.getInteger("u_landing_page_completed"),
        u_address_completed = doc.getInteger("u_address_completed"),
        u_ownership_completed = doc.getInteger("u_ownership_completed"),
        u_power_bill_completed = doc.getInteger("u_power_bill_completed"),
        u_power_company_completed = doc.getInteger("u_power_company_completed"),
        u_name_completed = doc.getInteger("u_name_completed"),
        u_email_completed = doc.getInteger("u_email_completed"),
        u_phone_completed = doc.getInteger("u_phone_completed"),
        u_email_modal_completed = doc.getInteger("u_email_modal_completed"),
        u_credit_score_completed = doc.getInteger("u_credit_score_completed"),
        u_page_focus = doc.getInteger("u_page_focus"),
        u_page_blur = doc.getInteger("u_page_blur")
      ),
      u_event_2 = ArrivalFactUEvent2(
        u_form_step_6 = doc.getInteger("u_form_step_6"),
        u_form_step_7 = doc.getInteger("u_form_step_7"),
        u_form_step_8 = doc.getInteger("u_form_step_8"),
        u_form_complete = doc.getInteger("u_form_complete")
      )
    )

    def dtOptToStrOpt(dtOpt: Option[DateTime]): Option[String] = dtOpt match {
      case Some(dt) => Some(dt.toString(dtf))
      case _ => None
    }

    def strOptToDtOpt(strOpt: Option[String]): Option[DateTime] = strOpt match {
      case Some(str) => Some(DateTime.parse(str, dtf))
      case _ => None
    }
}
