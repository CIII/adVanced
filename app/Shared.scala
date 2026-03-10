package Shared

import java.io.File
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._

import org.apache.pekko.actor.ActorSystem
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.mindrot.jbcrypt.BCrypt
import org.mongodb.scala.bson.Document
import play.api.Logging
import play.api.data.Mapping
import play.api.data.Forms._
import play.api.libs.json._
import play.api.mvc.RequestHeader

import scala.concurrent.duration._

/**
 * Shared constants, enumerations, data structures, and utility functions.
 *
 * NOTE: This object has been cleaned up from the original monolithic singleton.
 * MongoDB, Redis, and actor system management have been moved to injectable services:
 * - services.MongoService
 * - services.RedisService
 * - modules.StartupTasks (daemon scheduling)
 *
 * Data structures and enumerations remain here since they are referenced
 * throughout the codebase as value types.
 */
object Shared extends Logging {

  val cache_ext = "_cache"
  val task_ext = "_task"

  // -------------------------------------------------------------------
  // Enumerations
  // -------------------------------------------------------------------

  object ChangeCategory extends Enumeration {
    type ChangeCategory = Value
    val ALERT, MCC, API_ACCOUNT, ACCOUNT_INFO, ADVERTISER, CUSTOMER, ACCOUNT,
      CAMPAIGN, CAMPAIGN_AD_SCHEDULE, CAMPAIGN_KEYWORD, CAMPAIGN_PROXIMITY,
      AD_SCHEDULE, AD_GROUP, AD_SET, AD, AD_STUDY, IMAGE_AD, MOBILE_AD, TEXT_AD,
      SITE_LINK, SITE_PLACEMENT, KEYWORD, BUDGET, BIDDING_STRATEGY = Value
  }

  object TrafficSource extends Enumeration {
    type TrafficSource = Value
    val GOOGLE, MSN, FACEBOOK = Value
  }

  object ChangeType extends Enumeration {
    type ChangeType = Value
    val DELETE, NEW, UPDATE = Value
  }

  import Shared.ChangeCategory.ChangeCategory
  import Shared.ChangeType.ChangeType
  import Shared.TrafficSource.TrafficSource

  object ReportFormat extends Enumeration {
    type ReportFormat = Value
    val CSV, JSON, TSV = Value
  }

  object Attribution extends Enumeration {
    type Attribution = Value
    val click = Value
  }

  object MsnReportType extends Enumeration {
    type MsnReportType = Value
    val AccountPerformanceReportRequest,
      AdDynamicTextPerformanceReportRequest,
      AdExtensionByAdReportRequest,
      AdExtensionByKeywordReportRequest,
      AdExtensionDetailReportRequest,
      AdExtensionDimensionReportRequest,
      AdGroupPerformanceReportRequest,
      AdPerformanceReportRequest,
      AgeGenderDemographicReportRequest,
      BrandZonePerformanceReportRequest,
      BudgetSummaryReportRequest,
      CallDetailReportRequest,
      CampaignPerformanceReportRequest,
      ConversionPerformanceReportRequest,
      DestinationUrlPerformanceReportRequest,
      GeoLocationPerformanceReportRequest,
      GeographicalLocationReportRequest,
      GoalsAndFunnelsReportRequest,
      KeywordPerformanceReportRequest,
      NegativeKeywordConflictReportRequest,
      PollGenerateReportRequest,
      ProductOfferPerformanceReportRequest,
      ProductPartitionPerformanceReportRequest,
      ProductTargetPerformanceReportRequest,
      SearchQueryPerformanceReportRequest,
      ShareOfVoiceReportRequest,
      SitePerformanceReportRequest,
      SubmitGenerateReportRequest,
      TacticChannelReportRequest,
      TrafficSourcesReportRequest = Value
  }

  object LynxReportType extends Enumeration {
    type LynxReportType = Value
    val TQReportingArrivalFactsReportRequest = Value
  }


  object AlertType extends Enumeration {
    type AlertType = Value
    val BUDGET, CPC = Value
  }


  // -------------------------------------------------------------------
  // Data Structures (immutable case classes)
  // -------------------------------------------------------------------

  case class LynxReportRequest(
    reportType: LynxReportType.Value,
    startDate: DateTime,
    endDate: DateTime,
    reportFormat: ReportFormat.Value = ReportFormat.CSV
  )

  case class MsnReportRequest(
    apiAccount: models.mongodb.msn.Msn.ApiAccount,
    customerId: Long,
    customerAccountId: Long,
    reportType: MsnReportType.Value,
    startDate: DateTime,
    endDate: DateTime,
    reportFormat: ReportFormat.Value = ReportFormat.CSV
  )

  // TODO: Update to use new Google Ads API types when migration is complete
  case class GoogleReportRequest(
    reportType: String,
    fields: Option[List[String]],
    predicates: Option[List[String]],
    dateRange: String,
    downloadFormat: String = "CSV",
    oAuthData: models.mongodb.google.Google.Mcc,
    customerId: Option[String],
    enableZeroImpressions: Boolean,
    accountName: String
  )

  case class PendingCacheStructure(
    id: Long,
    changeCategory: ChangeCategory,
    trafficSource: TrafficSource,
    changeType: ChangeType,
    changeData: Document
  )

  object PendingCacheStructure {
    implicit val documentFormat: Format[Document] = new Format[Document] {
      def reads(json: JsValue): JsResult[Document] =
        JsSuccess(Document(json.toString()))
      def writes(doc: Document): JsValue =
        Json.parse(doc.toJson())
    }

    implicit val format: Format[PendingCacheStructure] = new Format[PendingCacheStructure] {
      def reads(json: JsValue): JsResult[PendingCacheStructure] = for {
        id <- (json \ "id").validate[Long]
        cc <- (json \ "changeCategory").validate[String].map(ChangeCategory.withName)
        ts <- (json \ "trafficSource").validate[String].map(TrafficSource.withName)
        ct <- (json \ "changeType").validate[String].map(ChangeType.withName)
        cd <- (json \ "changeData").validate[Document]
      } yield PendingCacheStructure(id, cc, ts, ct, cd)

      def writes(pcs: PendingCacheStructure): JsValue = Json.obj(
        "id" -> pcs.id,
        "changeCategory" -> pcs.changeCategory.toString,
        "trafficSource" -> pcs.trafficSource.toString,
        "changeType" -> pcs.changeType.toString,
        "changeData" -> Json.parse(pcs.changeData.toJson())
      )
    }
  }

  case class AlertCriteria(alertType: AlertType.Value)

  case class NotificationConfiguration(email: Boolean, sms: Boolean)

  case class Schedule(
    sunday: Boolean,
    monday: Boolean,
    tuesday: Boolean,
    wednesday: Boolean,
    thursday: Boolean,
    friday: Boolean,
    saturday: Boolean,
    delayBetweenChecks: Duration
  )

  case class AlertConfigurationMessage(
    userId: String,
    alertCriteria: AlertCriteria,
    notificationConfiguration: NotificationConfiguration,
    schedule: Schedule
  )

  case class TaskStructure(
    id: Long,
    user: String,
    data: List[PendingCacheStructure],
    startTime: DateTime,
    complete: Boolean,
    completeTime: Option[DateTime],
    processes: List[Process]
  )

  object TaskStructure {
    implicit val format: Format[TaskStructure] = new Format[TaskStructure] {
      def reads(json: JsValue): JsResult[TaskStructure] = for {
        id <- (json \ "id").validate[Long]
        user <- (json \ "user").validate[String]
        data <- (json \ "data").validate[List[PendingCacheStructure]]
        startTime <- (json \ "startTime").validate[Long].map(new DateTime(_))
        complete <- (json \ "complete").validate[Boolean]
        completeTime <- (json \ "completeTime").validateOpt[Long].map(_.map(new DateTime(_)))
        processes <- (json \ "processes").validate[List[Process]]
      } yield TaskStructure(id, user, data, startTime, complete, completeTime, processes)

      def writes(ts: TaskStructure): JsValue = Json.obj(
        "id" -> ts.id,
        "user" -> ts.user,
        "data" -> Json.toJson(ts.data),
        "startTime" -> ts.startTime.getMillis,
        "complete" -> ts.complete,
        "completeTime" -> ts.completeTime.map(_.getMillis),
        "processes" -> Json.toJson(ts.processes)
      )
    }
  }

  case class Process(
    changeDataId: Long,
    subProcesses: Int,
    completedSubProcesses: Int
  )

  object Process {
    implicit val format: Format[Process] = Json.format[Process]
  }

  case class AlertStructure(
    id: Long,
    startTime: DateTime,
    progress: Int,
    complete: Boolean,
    completeTime: Option[DateTime]
  )

  case class PendingCacheMessage(
    cache: Option[PendingCacheStructure],
    requestUsername: Option[String]
  )

  val ChartDateDimension: List[String] = List("day", "week", "month", "year")

  // -------------------------------------------------------------------
  // Utility Functions
  // -------------------------------------------------------------------

  def compare[T](a: Option[T], b: Option[T]): Boolean = (a, b) match {
    case (Some(av), Some(bv)) => av.equals(bv)
    case (None, None) => true
    case _ => false
  }

  def titleCase(s: String): String =
    s(0).toUpper + s.substring(1).toLowerCase

  def deleteFiles(directory: String, filename: String): Unit = for {
    files <- Option(new File(directory).listFiles)
    file <- files if file.getName.contains(filename)
  } file.delete()

  def doubleToMoney(v: Double): Double =
    Math.round(v * 100.0) / 100.0

  def stringToDateTime(v: Option[String], dateFormat: String = "yyyy-MM-dd", defaultValue: String = "2000-01-01"): DateTime =
    DateTimeFormat.forPattern(dateFormat).parseDateTime(v.getOrElse(defaultValue))

  def microToDollars(microAmt: Long): Double = microAmt / 1000000.0
  def dollarsToMicro(dollarAmt: Double): Long = (dollarAmt * 1000000).toLong

  def generatePassword(): String = {
    val rand = new SecureRandom
    val passChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    val passLength = 8
    (0 until passLength).map(_ => passChars.charAt(rand.nextInt(passChars.length))).mkString
  }

  def encryptPassword(password: String): String =
    BCrypt.hashpw(password, BCrypt.gensalt)

  def checkPasswordsMatch(entered: String, actual: String): Boolean =
    BCrypt.checkpw(entered, actual)

  def using[A <: { def close(): Unit }, B](param: A)(f: A => B): B = {
    try { f(param) }
    finally { param.close() }
  }

  // Subprocess tracking stubs - TODO: implement with TaskStructure updates
  def complete_subprocess(key: String, cache: PendingCacheStructure): Unit = {
    logger.debug(s"complete_subprocess called for key=$key (stub)")
  }

  def set_subprocess_count(key: String, cache: PendingCacheStructure, count: Int): Unit = {
    logger.debug(s"set_subprocess_count called for key=$key count=$count (stub)")
  }

  // -------------------------------------------------------------------
  // Form helpers: ObjectId ↔ String conversions for Play form mappings
  // -------------------------------------------------------------------

  /** Convert an Option[String] from a form field to an Option[ObjectId]. */
  def formStringToObjectId(s: Option[String]): Option[ObjectId] =
    s.filter(_.nonEmpty).map(new ObjectId(_))

  /** Convert an Option[ObjectId] to an Option[String] for use in form unbind. */
  def objectIdToFormString(id: Option[ObjectId]): Option[String] =
    id.map(_.toHexString)

  /** A Play form Mapping[DateTime] backed by a text field.
   *  Stores and parses ISO 8601 strings (e.g. "2024-01-15T00:00:00.000Z"). */
  def jodaDate: Mapping[DateTime] =
    text.transform[DateTime](DateTime.parse, _.toString)

  // Key helpers for Redis cache operations
  def taskKey(username: String): String = username + task_ext
  def pendingCacheKey(username: String): String = username + cache_ext

  // -------------------------------------------------------------------
  // In-memory session caches (replacing Redis lrange/lpush)
  // TODO: Migrate these to RedisService injection for multi-instance support
  // -------------------------------------------------------------------

  private val pendingCacheStore = new ConcurrentHashMap[String, List[PendingCacheStructure]]().asScala
  private val taskCacheStore = new ConcurrentHashMap[String, List[TaskStructure]]().asScala

  private def usernameFromEither(key: Either[RequestHeader, String]): String =
    key.fold(req => req.session.get("username").getOrElse(""), identity)

  def pendingCache(key: Either[RequestHeader, String]): List[PendingCacheStructure] =
    pendingCacheStore.getOrElse(pendingCacheKey(usernameFromEither(key)), List())

  def setPendingCache(key: Either[RequestHeader, String], value: List[PendingCacheStructure]): Unit =
    pendingCacheStore.put(pendingCacheKey(usernameFromEither(key)), value)

  def taskCache(key: Either[RequestHeader, String]): List[TaskStructure] =
    taskCacheStore.getOrElse(taskKey(usernameFromEither(key)), List())

  def setTaskCache(key: Either[RequestHeader, String], value: List[TaskStructure]): Unit =
    taskCacheStore.put(taskKey(usernameFromEither(key)), value)
}
