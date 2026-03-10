package models.mysql

import org.joda.time.DateTime

case class ArrivalFactDevice(
  device_name: Option[String],
  device_type: Option[String],
  device_brand: Option[String],
  device_model: Option[String]
)

case class ArrivalFactBrowser(
  browser: Option[String],
  browser_id: Option[String],
  browser_version: Option[String],
  arrivals: Int,
  bounce: Int,
  robot_id: Option[String],
  user_agent: Option[String]
)

case class ArrivalFactSession(
  session_id: Option[Long],
  ip_address: Option[String],
  ip_blacklisted: Option[Int],
  is_ip_blacklisted: Option[Int],
  maxmind_zip: Option[String],
  name_capture: Int,
  new_ip: Int,
  new_session: Int,
  os_name: Option[String],
  os_version: Option[String],
  ab_tests: Option[String],
  git_hash: Option[String],
  created_at: Option[DateTime],
  day_of_week: Option[String],
  duration: Int,
  entry_page: Option[String],
  entry_url: Option[String],
  exit_url: Option[String],
  last_activity: Option[DateTime],
  local_hour: Option[Int],
  page_views: Option[Int],
  revenue: Option[Double]
)

case class ArrivalFactForm(
  email: Option[String],
  form_city: Option[String],
  form_state: Option[String],
  form_zip: Option[String],
  electric_bill: Option[String],
  prop_own: Option[String]
)

case class ArrivalFactUEvent1(
  u_page_closed: Int,
  u_page_loaded: Int,
  u_form_step_2b: Int,
  u_page_rendered: Int,
  u_form_step_1: Int,
  u_form_step_2: Int,
  u_form_step_3: Int,
  u_form_step_4: Int,
  u_form_step_5: Int,
  u_maxmind_failure_on_page_load: Int,
  u_landing_page_completed: Int,
  u_address_completed: Int,
  u_ownership_completed: Int,
  u_power_bill_completed: Int,
  u_power_company_completed: Int,
  u_name_completed: Int,
  u_email_completed: Int,
  u_phone_completed: Int,
  u_email_modal_completed: Int,
  u_credit_score_completed: Int,
  u_page_focus: Int,
  u_page_blur: Int
)

case class ArrivalFactUEvent2(
  u_form_step_6: Int,
  u_form_step_7: Int,
  u_form_step_8: Int,
  u_form_complete: Int
)

case class ArrivalFactEvent1(
  maxmind_failure_on_page_load: Int,
  landing_page_completed: Int,
  address_completed: Int,
  ownership_completed: Int,
  power_bill_completed: Int,
  power_company_completed: Int,
  name_completed: Int,
  email_completed: Int,
  phone_completed: Int,
  email_modal_completed: Int,
  credit_score_completed: Int,
  creative: Option[String],
  page_focus: Int,
  page_blur: Int,
  event_category: Option[String],
  events_count: Int,
  form_step_1: Int,
  form_step_2: Int,
  form_step_3: Int,
  form_step_2b: Int,
  page_rendered: Int,
  form_step_4: Int
)

case class ArrivalFactEvent2(
  form_step_7: Int,
  form_step_8: Int,
  lp_content_engage: Int,
  form_complete: Int,
  page_closed: Int,
  page_loaded: Int,
  form_step_5: Int,
  form_step_6: Int
)


case class ArrivalFactTrafficSource(
  adgroupid: Option[String],
  gclid: Option[String],
  lp_conv_u: Option[Int],
  keyword: Option[String],
  g_network: Option[String],
  g_device: Option[String],
  g_location: Option[String],
  conf: Option[Int],
  conu: Option[Int],
  conversion: Option[Int],
  conversion_count: Option[Int],
  conversion_page: Option[String],
  lp_ctc: Int,
  u_lp_ctc: Int,
  u_lp_content_engage: Int,
  utm_source: Option[String],
  utm_campaign: Option[String],
  utm_medium: Option[String]
)



case class ArrivalFact(
  id: Option[Long],
  traffic_source: ArrivalFactTrafficSource,
  device: ArrivalFactDevice,
  browser: ArrivalFactBrowser,
  session: ArrivalFactSession,
  form: ArrivalFactForm,
  event_1: ArrivalFactEvent1,
  event_2: ArrivalFactEvent2,
  u_event_1: ArrivalFactUEvent1,
  u_event_2: ArrivalFactUEvent2
)

case class ABTestVariations(
  id: Option[Long],
  test: String,
  test_id: Int,
  variable: String,
  variable_id: Int,
  variation: String,
  encoding: String
)