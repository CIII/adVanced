package models.mysql

import org.joda.time.DateTime

case class ArrivalFactDevice(
  var device_name: Option[String],
  var device_type: Option[String],
  var device_brand: Option[String],
  var device_model: Option[String]
)

case class ArrivalFactBrowser(
  var browser: Option[String],
  var browser_id: Option[String],
  var browser_version: Option[String],
  var arrivals: Int,
  var bounce: Int,
  var robot_id: Option[String],
  var user_agent: Option[String]
)

case class ArrivalFactSession(
  var session_id: Option[Long],
  var ip_address: Option[String],
  var ip_blacklisted: Option[Int],
  var is_ip_blacklisted: Option[Int],
  var maxmind_zip: Option[String],
  var name_capture: Int,
  var new_ip: Int,
  var new_session: Int,
  var os_name: Option[String],
  var os_version: Option[String],
  var ab_tests: Option[String],
  var git_hash: Option[String],
  var created_at: Option[DateTime],
  var day_of_week: Option[String],
  var duration: Int,
  var entry_page: Option[String],
  var entry_url: Option[String],
  var exit_url: Option[String],
  var last_activity: Option[DateTime],
  var local_hour: Option[Int],
  var page_views: Option[Int],
  var revenue: Option[Double]
)

case class ArrivalFactForm(
  var email: Option[String],
  var form_city: Option[String],
  var form_state: Option[String],
  var form_zip: Option[String],
  var electric_bill: Option[String],
  var prop_own: Option[String]
)

case class ArrivalFactUEvent1(
  var u_page_closed: Int,
  var u_page_loaded: Int,
  var u_form_step_2b: Int,
  var u_page_rendered: Int,
  var u_form_step_1: Int,
  var u_form_step_2: Int,
  var u_form_step_3: Int,
  var u_form_step_4: Int,
  var u_form_step_5: Int,
  var u_maxmind_failure_on_page_load: Int,
  var u_landing_page_completed: Int,
  var u_address_completed: Int,
  var u_ownership_completed: Int,
  var u_power_bill_completed: Int,
  var u_power_company_completed: Int,
  var u_name_completed: Int,
  var u_email_completed: Int,
  var u_phone_completed: Int,
  var u_email_modal_completed: Int,
  var u_credit_score_completed: Int,
  var u_page_focus: Int,
  var u_page_blur: Int
)

case class ArrivalFactUEvent2(
  var u_form_step_6: Int,
  var u_form_step_7: Int,
  var u_form_step_8: Int,
  var u_form_complete: Int
)

case class ArrivalFactEvent1(
  var maxmind_failure_on_page_load: Int,
  var landing_page_completed: Int,
  var address_completed: Int,
  var ownership_completed: Int,
  var power_bill_completed: Int,
  var power_company_completed: Int,
  var name_completed: Int,
  var email_completed: Int,
  var phone_completed: Int,
  var email_modal_completed: Int,
  var credit_score_completed: Int,
  var creative: Option[String],
  var page_focus: Int,
  var page_blur: Int,
  var event_category: Option[String],
  var events_count: Int,
  var form_step_1: Int,
  var form_step_2: Int,
  var form_step_3: Int,
  var form_step_2b: Int,
  var page_rendered: Int,
  var form_step_4: Int
)

case class ArrivalFactEvent2(
  var form_step_7: Int,
  var form_step_8: Int,
  var lp_content_engage: Int,
  var form_complete: Int,
  var page_closed: Int,
  var page_loaded: Int,
  var form_step_5: Int,
  var form_step_6: Int
)


case class ArrivalFactTrafficSource(
  var adgroupid: Option[String],
  var gclid: Option[String],
  var lp_conv_u: Option[Int],
  var keyword: Option[String],
  var g_network: Option[String],
  var g_device: Option[String],
  var g_location: Option[String],
  var conf: Option[Int],
  var conu: Option[Int],
  var conversion: Option[Int],
  var conversion_count: Option[Int],
  var conversion_page: Option[String],
  var lp_ctc: Int,
  var u_lp_ctc: Int,
  var u_lp_content_engage: Int,
  var utm_source: Option[String],
  var utm_campaign: Option[String],
  var utm_medium: Option[String]
)



case class ArrivalFact(
  id: Option[Long],
  var traffic_source: ArrivalFactTrafficSource,
  var device: ArrivalFactDevice,
  var browser: ArrivalFactBrowser,
  var session: ArrivalFactSession,
  var form: ArrivalFactForm,
  var event_1: ArrivalFactEvent1,
  var event_2: ArrivalFactEvent2,
  var u_event_1: ArrivalFactUEvent1,
  var u_event_2: ArrivalFactUEvent2
)

case class ABTestVariations(
  id: Option[Long],
  var test: String,
  var test_id: Int,
  var variable: String,
  var variable_id: Int,
  var variation: String,
  var encoding: String
)