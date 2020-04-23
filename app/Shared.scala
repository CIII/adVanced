package Shared

import java.io.File
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Calendar

import _root_.util._
import akka.actor.{ActorSystem, Props}
import akka.util.ByteString
import be.objectify.deadbolt.scala.AuthenticatedRequest
import com.google.api.ads.adwords.axis.v201609.mcm.{Customer, ManagedCustomer}
import com.google.gson.GsonBuilder
import com.microsoft.bingads.customermanagement.{Account, AccountInfoWithCustomerData}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import models.mongodb.google.Google._
import models.mongodb.msn.Msn._
import models.mongodb.yahoo.Yahoo._
import models.mongodb.{PermissionGroup, SecurityRole, Utilities}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.api.{Logger, Play}
import redis.protocol.{Bulk, RedisReply}
import redis.{ByteStringFormatter, RedisClient, RedisReplyDeserializer}
import sync.facebook.business.FacebookBusinessActor
import sync.facebook.process.reporting._
import sync.google.process.management.mcc.MccActor
import sync.google.process.reporting.GoogleReportActor
import sync.lynx.process.LynxReportActor
import sync.msn.process.reporting.MsnReportActor
import sync.yahoo.reporting.YahooReportActor
import util.charts.{ChartMetaData, _}

import scala.collection.immutable.List
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.reflect.runtime.universe._

object Shared extends Controller {
  implicit val akkaSystem = akka.actor.ActorSystem()
  var redisClient = RedisClient(Play.current.configuration.getString("redis.host").get, Play.current.configuration.getInt("redis.port").get)
  val futurePong: Future[String] = redisClient.ping()
  Logger.debug("Ping sent!")
  futurePong.map(pong => {
    Logger.debug(s"Redis replied with a $pong")
  })
  Await.result(futurePong, 5 seconds)

  // Google GSON object for serializing/deserializing API objs
  val gson = new GsonBuilder()
    .addSerializationExclusionStrategy(new PrimaryExclusionStrategy)
    .addDeserializationExclusionStrategy(new PrimaryExclusionStrategy)
    .registerTypeAdapter(classOf[com.google.api.ads.adwords.axis.v201609.cm.Criterion], new CriterionAdapter)
    .registerTypeAdapter(classOf[com.google.api.ads.adwords.axis.v201609.cm.BiddingScheme], new BiddingSchemeAdapter)
    .registerTypeAdapter(classOf[com.google.api.ads.adwords.axis.v201609.cm.Setting], new SettingAdapter)
    .registerTypeAdapter(classOf[com.google.api.ads.adwords.axis.v201609.cm.Ad], new AdAdapter)
    .registerTypeAdapter(classOf[com.google.api.ads.adwords.axis.v201609.cm.Bids], new BidsAdapter)
    .registerTypeAdapter(classOf[com.google.api.ads.adwords.axis.v201609.cm.ComparableValue], new ComparableValueAdapter)
    .registerTypeAdapter(classOf[com.google.api.ads.adwords.axis.v201609.cm.Address], new AddressAdapter)
    .registerTypeAdapter(classOf[PendingCacheStructure], new PendingCacheStructureAdapter)
    .registerTypeAdapter(classOf[TaskStructure], new TaskStructureAdapter)
    .registerTypeAdapter(classOf[ChartMetaData], new ChartMetaData.ChartMetaDataAdapter)
    .registerTypeAdapter(classOf[ChartData], new ChartData.ChartDataAdapter)
    .registerTypeAdapter(classOf[PerformanceGraph], new PerformanceGraph.PerformanceGraphAdapter)
    .registerTypeAdapter(classOf[PerformanceEntityFilter], new PerformanceEntityFilter.PerformanceEntityFilterAdapter)
    .registerTypeAdapter(classOf[PerformanceField], new PerformanceField.PerformanceFieldAdapter)
    .create()

  // connection to mongodb
  val mongoClient = MongoClient(
    Play.current.configuration.getString("mongodb.host").getOrElse(
      throw new Exception("MongoDB application host missing")
    ),
    Play.current.configuration.getInt("mongodb.port").getOrElse(
      throw new Exception("MongoDB application port missing")
    )
  )

  // retrieve advanced db
  val advancedDB = mongoClient(
    Play.current.configuration.getString("mongodb.db").getOrElse(
      throw new Exception("MongoDB Database name missing")
    )
  )

  // create collection when one doesn't already exist
  def advancedCollection(col_name: String): MongoCollection = {
    if (!advancedDB.collectionExists(col_name))
      advancedDB.createCollection(col_name, DBObject())
    advancedDB(col_name)
  }

  // typesafe comparator
  def compare[T](a: Option[T], b: Option[T]) = (a, b) match {
    case (Some(a), Some(b)) => if(a.equals(b)) true else false
    case (None, None)       => true
    case (_, _)          => false
  }


  implicit class MyInstanceOf[U: TypeTag](that: U) {
    def myIsInstanceOf[T: TypeTag] =
      typeOf[U] <:< typeOf[T]
  }

  val cache_ext = "_cache"
  val task_ext = "_task"

  val advantage_google_entities = List(
    ""
  )

  // standard user definition
  lazy val normalUserID = SecurityRole.securityRoleCollection.findOne(DBObject("RoleName" -> "Standard")) match {
      case Some(role) =>
        role._id.get
      case _ =>
        val _id = new ObjectId
        SecurityRole.securityRoleCollection.insert(
          SecurityRole.securityRoleToDbo(
            SecurityRole(
              Some(_id),
              "Standard",
              Array(
                PermissionGroup.GoogleRead,
                PermissionGroup.GoogleCharts,
                PermissionGroup.MSNRead,
                PermissionGroup.MSNCharts,
                PermissionGroup.FacebookRead,
                PermissionGroup.FacebookCharts,
                PermissionGroup.YahooRead,
                PermissionGroup.YahooCharts
              )
            )
          )
        )
        _id
    }

  // admin user definition
  lazy val adminUserID = SecurityRole.securityRoleCollection.findOne(DBObject("roleName" -> "Admin")) match {
    case Some(role) =>
      role._id.get
    case _ =>
      val _id = new ObjectId
      SecurityRole.securityRoleCollection.insert(
        SecurityRole.securityRoleToDbo(
          SecurityRole(
            Some(_id),
            "Standard",
            Array(
              PermissionGroup.GoogleWrite,
              PermissionGroup.GoogleRead,
              PermissionGroup.GoogleCharts,
              PermissionGroup.MSNWrite,
              PermissionGroup.MSNRead,
              PermissionGroup.MSNCharts,
              PermissionGroup.FacebookWrite,
              PermissionGroup.FacebookRead,
              PermissionGroup.FacebookCharts,
              PermissionGroup.YahooWrite,
              PermissionGroup.YahooRead,
              PermissionGroup.YahooCharts
            )
          )
        )
      )
      _id
  }

  // sync change categories
  object ChangeCategory extends Enumeration {
    type ChangeCategory = Value

    val
    ALERT, MCC, API_ACCOUNT, ACCOUNT_INFO, ADVERTISER, CUSTOMER, ACCOUNT, CAMPAIGN,
    CAMPAIGN_AD_SCHEDULE, CAMPAIGN_KEYWORD, CAMPAIGN_PROXIMITY, AD_SCHEDULE, AD_GROUP,
    AD_SET, AD, IMAGE_AD, MOBILE_AD, TEXT_AD, SITE_LINK, SITE_PLACEMENT, KEYWORD, BUDGET,
    BIDDING_STRATEGY = Value
  }

  // sync traffic sources
  object TrafficSource extends Enumeration {
    type TrafficSource = Value
    val GOOGLE, MSN, YAHOO, FACEBOOK = Value
  }

  // sync change types
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

  // defines lynx report request obj
  case class LynxReportRequest (
    reportType: LynxReportType.Value,
    startDate: DateTime,
    endDate: DateTime,
    reportFormat: ReportFormat.Value=ReportFormat.CSV
  )

  // defines msn report request obj
  case class MsnReportRequest (
    apiAccount: models.mongodb.msn.Msn.ApiAccount,
    customerId: Long,
    customerAccountId: Long,
    reportType: MsnReportType.Value,
    startDate: DateTime,
    endDate: DateTime,
    reportFormat: ReportFormat.Value=ReportFormat.CSV
  )

  // defines an adwords report request obj
  case class GoogleReportRequest (
    reportType: com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType,
    fields: Option[List[String]],
    predicates: Option[List[com.google.api.ads.adwords.lib.jaxb.v201609.Predicate]],
    dateRange: com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionDateRangeType,
    downloadFormat: com.google.api.ads.adwords.lib.jaxb.v201609.DownloadFormat =
      com.google.api.ads.adwords.lib.jaxb.v201609.DownloadFormat.CSV,
    oAuthData: models.mongodb.google.Google.Mcc,
    customerId: Option[String],
    enableZeroImpressions: Boolean,
    accountName: String
  )

  // list of attribution types
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

  // list of yahoo gemini report types
  object GeminiReportType extends Enumeration {
    type GeminiReportType = Value
    val keyword_stats, adjustment_stats, performance_stats, search_stats, ad_extension_details, conversion_rules_stats, audience  = Value
  }

  // list of yahoo gemini report fields
  object GeminiReportField extends Enumeration {
    type GeminiReportField = Value
    val Advertiser_ID,
    Campaign_ID,
    Ad_Group_ID,
    Ad_ID,
    Month,
    Week,
    Day,
    Hour,
    Pricing_Type,
    Source,
    Impressions,
    Clicks,
    Post_Click_Conversions,
    Conversions,
    Total_Conversions,
    Spend,
    Average_Position,
    Max_Bid,
    Ad_Extn_Impressions,
    Ad_Extn_Clicks,
    Ad_Extn_Conversions,
    Ad_Extn_Spend,
    Average_CPC,
    Average_CPM,
    CTR,
    Is_Adjustment,
    Adjustment_Type,
    Keyword_ID,
    Destination_URL,
    Device_Type,
    Post_Impression_Conversions,
    Delivered_Match_Type,
    Search_Term,
    Device,
    Audience_Type,
    Audience_ID,
    Product_Group_ID,
    Category_ID,
    Category_Name,
    Product_Type,
    Brand,
    Item_ID,
    Rule_ID,
    Rule_Name,
    Conversion_Device,
    Price_Type,
    Ad_Extn_ID,
    Keyword_Value,
    Post_View_Conversions,
    Conversion_Value = Value
  }

  // yahoo gemini report filter operations
  object GeminiReportFilterOperation extends Enumeration {
    type GeminiFilterOperations = Value
    val `=`, IN, between = Value
  }

  // yahoo gemini report -> field mapping
  def GeminiReportFieldMapping = Map(
    GeminiReportType.performance_stats -> List(
      GeminiReportField.Advertiser_ID,
      GeminiReportField.Campaign_ID,
      GeminiReportField.Ad_Group_ID,
      GeminiReportField.Ad_ID,
      GeminiReportField.Month,
      GeminiReportField.Week,
      GeminiReportField.Day,
      GeminiReportField.Hour,
      GeminiReportField.Pricing_Type,
      GeminiReportField.Source,
      GeminiReportField.Impressions,
      GeminiReportField.Clicks,
      GeminiReportField.Post_Click_Conversions,
      GeminiReportField.Post_Impression_Conversions,
      GeminiReportField.Conversions,
      GeminiReportField.Total_Conversions,
      GeminiReportField.Spend,
      GeminiReportField.Average_Position,
      GeminiReportField.Max_Bid,
      GeminiReportField.Ad_Extn_Impressions,
      GeminiReportField.Ad_Extn_Clicks,
      GeminiReportField.Ad_Extn_Conversions,
      GeminiReportField.Ad_Extn_Spend,
      GeminiReportField.Average_CPC,
      GeminiReportField.Average_CPM,
      GeminiReportField.CTR
    ),
    GeminiReportType.adjustment_stats -> List(
      GeminiReportField.Advertiser_ID,
      GeminiReportField.Campaign_ID,
      GeminiReportField.Day,
      GeminiReportField.Pricing_Type,
      GeminiReportField.Source,
      GeminiReportField.Is_Adjustment,
      GeminiReportField.Adjustment_Type,
      GeminiReportField.Impressions,
      GeminiReportField.Clicks,
      GeminiReportField.Conversions,
      GeminiReportField.Spend,
      GeminiReportField.Average_Position
    ),
    GeminiReportType.keyword_stats -> List(
      GeminiReportField.Advertiser_ID,
      GeminiReportField.Campaign_ID,
      GeminiReportField.Ad_Group_ID,
      GeminiReportField.Ad_ID,
      GeminiReportField.Keyword_ID,
      GeminiReportField.Destination_URL,
      GeminiReportField.Day,
      GeminiReportField.Device_Type,
      GeminiReportField.Impressions,
      GeminiReportField.Clicks,
      GeminiReportField.Post_Click_Conversions,
      GeminiReportField.Post_Impression_Conversions,
      GeminiReportField.Conversions,
      GeminiReportField.Total_Conversions,
      GeminiReportField.Spend,
      GeminiReportField.Average_Position,
      GeminiReportField.Max_Bid,
      GeminiReportField.Average_CPC,
      GeminiReportField.Average_CPM,
      GeminiReportField.CTR
    ),
    GeminiReportType.search_stats -> List(
      GeminiReportField.Advertiser_ID,
      GeminiReportField.Campaign_ID,
      GeminiReportField.Ad_Group_ID,
      GeminiReportField.Ad_ID,
      GeminiReportField.Keyword_ID,
      GeminiReportField.Delivered_Match_Type,
      GeminiReportField.Search_Term,
      GeminiReportField.Device,
      GeminiReportField.Destination_URL,
      GeminiReportField.Day,
      GeminiReportField.Impressions,
      GeminiReportField.Clicks,
      GeminiReportField.Spend,
      GeminiReportField.Conversions,
      GeminiReportField.Post_Click_Conversions,
      GeminiReportField.Post_Impression_Conversions,
      GeminiReportField.Average_Position,
      GeminiReportField.Max_Bid,
      GeminiReportField.Average_CPC,
      GeminiReportField.CTR
    ),
    GeminiReportType.ad_extension_details -> List(
      GeminiReportField.Advertiser_ID,
      GeminiReportField.Campaign_ID,
      GeminiReportField.Ad_Group_ID,
      GeminiReportField.Ad_ID,
      GeminiReportField.Keyword_ID,
      GeminiReportField.Ad_Extn_ID,
      GeminiReportField.Device,
      GeminiReportField.Month,
      GeminiReportField.Week,
      GeminiReportField.Day,
      GeminiReportField.Pricing_Type,
      GeminiReportField.Destination_URL,
      GeminiReportField.Impressions,
      GeminiReportField.Clicks,
      GeminiReportField.Conversions,
      GeminiReportField.Spend,
      GeminiReportField.Average_CPC,
      GeminiReportField.CTR
    ),
    GeminiReportType.audience -> List(
      GeminiReportField.Advertiser_ID,
      GeminiReportField.Campaign_ID,
      GeminiReportField.Ad_Group_ID,
      GeminiReportField.Ad_ID,
      GeminiReportField.Audience_Type,
      GeminiReportField.Audience_ID,
      GeminiReportField.Day,
      GeminiReportField.Pricing_Type,
      GeminiReportField.Source,
      GeminiReportField.Impressions,
      GeminiReportField.Clicks,
      GeminiReportField.Post_Click_Conversions,
      GeminiReportField.Post_Impression_Conversions,
      GeminiReportField.Conversions,
      GeminiReportField.Total_Conversions,
      GeminiReportField.Spend
    ),
    GeminiReportType.conversion_rules_stats -> List(
      GeminiReportField.Advertiser_ID,
      GeminiReportField.Campaign_ID,
      GeminiReportField.Ad_Group_ID,
      GeminiReportField.Rule_ID,
      GeminiReportField.Rule_Name,
      GeminiReportField.Category_Name,
      GeminiReportField.Conversion_Device,
      GeminiReportField.Keyword_ID,
      GeminiReportField.Keyword_Value,
      GeminiReportField.Source,
      GeminiReportField.Price_Type,
      GeminiReportField.Day,
      GeminiReportField.Post_View_Conversions,
      GeminiReportField.Post_Click_Conversions,
      GeminiReportField.Conversion_Value,
      GeminiReportField.Conversions
    )
  )

  // yahoo gemini report job statuses
  object GeminiReportJobStatus extends Enumeration {
    type GeminiReportJobStatus = Value
    val submitted, running, failed, completed, killed = Value
  }

  // defines yahoo gemini report filter obj
  case class GeminiReportFilter(
    field: GeminiReportField.Value,
    operator: GeminiReportFilterOperation.Value,
    value: Option[String] = None,
    values: Option[List[String]] = None,
    from: Option[String] = None,
    to: Option[String] = None
  )

  // yahoo gemini report filter obj -> mongodb obj
  def geminiReportFilterToDBObject(grf: GeminiReportFilter) = DBObject(
    "field" -> grf.field.toString,
    "operator" -> grf.operator.toString,
    "value" -> grf.value.getOrElse(""),
    "values" -> (grf.values match {case Some(v) => MongoDBList(v: _*) case _ => MongoDBList()}),
    "from" -> grf.from.getOrElse(""),
    "to" -> grf.to.getOrElse("")
  )

  // mongodb object -> yahoo gemini report filter obj
  def dboToGeminiReportFilter(dbo: DBObject) = GeminiReportFilter(
    field = GeminiReportField.withName(dbo.as[String]("field")),
    operator = GeminiReportFilterOperation.withName(dbo.as[String]("operator")),
    value = if(dbo.as[String]("value") == "") None else Some(dbo.as[String]("value")),
    values = if(dbo.as[MongoDBList]("values").isEmpty) None else Some(dbo.as[MongoDBList]("values").asInstanceOf[List[String]]),
    from = if(dbo.as[String]("from") == "") None else Some(dbo.as[String]("from")),
    to = if(dbo.as[String]("to") == "") None else Some(dbo.as[String]("to"))
  )

  // defines yahoo gemini report request obj
  case class GeminiReportRequest (
    reportType: GeminiReportType.Value,
    fields: List[GeminiReportField.Value],
    var filters: Option[List[GeminiReportFilter]],
    oAuthData: models.mongodb.yahoo.Yahoo.ApiAccount,
    reportFormat: ReportFormat.Value=ReportFormat.CSV
  )

  lazy val GeminiReportDateFormat = new SimpleDateFormat("yyyy-MM-dd")

  lazy val ChartDateDimension = List(
    "day",
    "week",
    "month",
    "year"
  )

  case class PendingCacheStructure (
    var id: Long,
    var changeCategory: ChangeCategory,
    var trafficSource: TrafficSource,
    var changeType: ChangeType,
    var changeData: DBObject
  )

  object AlertType extends Enumeration {
    type AlertType = Value

    val BUDGET, CPC = Value
  }


  case class AlertCriteria(
    var alertType: AlertType.Value
  )

  case class NotificationConfiguration(
    var email: Boolean,
    var sms: Boolean
  )

  case class Schedule(
    var sunday: Boolean,
    var monday: Boolean,
    var tuesday: Boolean,
    var wednesday: Boolean,
    var thursdsay: Boolean,
    var friday: Boolean,
    var saturday: Boolean,
    var delayBetweenChecks: Duration
  )

  case class AlertConfigurationMessage (
    var userId: ObjectId,
    var alertCriteria: AlertCriteria,
    var notificationConfiguration: NotificationConfiguration,
    var schedule: Schedule
  )

  object PendingCacheStructure {
    implicit val byteStringFormatter = new ByteStringFormatter[PendingCacheStructure] {
      def serialize(data: PendingCacheStructure): ByteString = {
        ByteString(gson.toJson(data))
      }

      def deserialize(bs: ByteString): PendingCacheStructure = {
        gson.fromJson(bs.utf8String, classOf[PendingCacheStructure])
      }
    }
  }

  // pending cache structure obj -> mongodb obj
  def pendingCacheStructureToDbo(pcs: PendingCacheStructure): DBObject = {
    DBObject(
      "id" -> pcs.id,
      "changeCategory" -> pcs.changeCategory.toString,
      "trafficSource" -> pcs.trafficSource.toString,
      "changeType" -> pcs.changeType.toString,
      "changeData" -> pcs.changeData
    )
  }

  // mongodb obj -> pending cache structure
  def dboToPendingCacheStructure(dbo: DBObject): PendingCacheStructure = {
    PendingCacheStructure(
      id=dbo.getAs[Long]("id").get,
      changeCategory=ChangeCategory.withName(dbo.getAs[String]("changeCategory").get),
      trafficSource=TrafficSource.withName(dbo.getAs[String]("trafficSource").get),
      changeType=ChangeType.withName(dbo.getAs[String]("changeType").get),
      changeData=dbo.getAs[DBObject]("changeData").get
    )
  }

  case class TaskStructure (
    var id: Long,
    var user: String,
    var data: List[PendingCacheStructure],
    var startTime: DateTime,
    var complete: Boolean,
    var completeTime: Option[DateTime],
    var processes: List[Process]
  )

  object TaskStructure {
    implicit val byteStringFormatter = new ByteStringFormatter[TaskStructure] {
      def serialize(data: TaskStructure): ByteString = {
        ByteString(gson.toJson(data))
      }

      def deserialize(bs: ByteString): TaskStructure = {
        gson.fromJson(bs.utf8String, classOf[TaskStructure])
      }
    }

    implicit val redisReplyDeserializer =  new RedisReplyDeserializer[TaskStructure] {
      override def deserialize: PartialFunction[RedisReply, TaskStructure] = {
        case Bulk(Some(bs)) => byteStringFormatter.deserialize(bs)
      }
    }
  }

  // task structure obj -> mongodb obj
  def taskStructureToDbo(ts: TaskStructure) = DBObject(
    "id" -> ts.id,
    "user" -> ts.user,
    "data" -> ts.data.map(pendingCacheStructureToDbo),
    "startTime" -> ts.startTime.getMillis,
    "complete" -> ts.complete,
    "completeTime" -> (if(ts.completeTime.isEmpty) None else ts.completeTime.get.getMillis),
    "processes" -> ts.processes.map(processToDbo)
  )

  // mongodb obj -> task structure obj
  def dboToTaskStructure(dbo: DBObject) = TaskStructure(
    id=dbo.as[Long]("task.id"),
    user=dbo.getAsOrElse[String]("user", "SYSTEM"),
    data=dbo.getAsOrElse[List[DBObject]]("task.data", List()).map(dboToPendingCacheStructure),
    startTime=new DateTime(dbo.as[Long]("task.startTime")),
    complete=dbo.getAsOrElse[Boolean]("task.complete", false),
    completeTime=Some(new DateTime(dbo.getAsOrElse[Option[Long]]("task.completeTime", None).getOrElse(0L))),
    processes=dbo.getAsOrElse[List[DBObject]]("task.processes", List()).map(dboToProcess)
  )

  case class Process (
    changeDataId: Long,
    var subProcesses: Int,
    var completedSubProcesses: Int
  )

  // process obj -> mongodb obj
  def processToDbo(p: Process): DBObject = {
    DBObject(
      "changeDataId" -> p.changeDataId,
      "subProcesses" -> p.subProcesses,
      "completedSubProcesses" -> p.completedSubProcesses
    )
  }

  // mongodb obj -> process obj
  def dboToProcess(dbo: DBObject) = Process(
    changeDataId = dbo.getAs[Long]("changeDataId").get,
    subProcesses=dbo.getAsOrElse[Int]("subProcesses", 0),
    completedSubProcesses = dbo.getAsOrElse[Int]("completedSubProcesses", 0)
  )

  case class AlertStructure (
    id: Long,
    var startTime: DateTime,
    var progress: Int,
    var complete: Boolean,
    var completeTime: Option[DateTime]
  )

  // alert structure obj -> mongodb obj
  def alertStructureToDbo(as: AlertStructure) = DBObject(
    "id" -> as.id,
    "startTime" -> as.startTime,
    "progress" -> as.progress,
    "complete" -> as.complete,
    "completeTime" -> as.completeTime
  )

  // mongodb obj -> alert structure obj
  def dboToAlertStructure(dbo: DBObject) = AlertStructure(
    id=dbo.as[Long]("id"),
    startTime=dbo.as[DateTime]("startTime"),
    progress=dbo.getAsOrElse[Int]("progress", 0),
    complete=dbo.getAsOrElse[Boolean]("complete", false),
    completeTime=Some(dbo.getAsOrElse[DateTime]("completeTime", new DateTime))
  )


  case class PendingCacheMessage(
    cache: Option[PendingCacheStructure],
    request: Option[AuthenticatedRequest[AnyContent]]
  )

  // Akka Async system definitions for reporting and sync processes
  // --------------------------------------------------
  val userActorSystem = ActorSystem("UserActorSystem")
  val googleReportingActorSystem = ActorSystem("GoogleReportingActorSystem")
  val googleManagementActorSystem = ActorSystem("GoogleManagementActorSystem")

  val yahooReportingActorSystem = ActorSystem("YahooReportingActorSystem")
  val yahooManagementActorSystem = ActorSystem("YahooManagementActorSystem")

  val facebookReportingActorSystem = ActorSystem("FacebookReportingActorSystem")
  val facebookManagementActorSystem = ActorSystem("FacebookManagementActorSystem")
  val facebookBusinessActorSystem = ActorSystem("FacebookBusinessActorSystem")

  val msnReportingActorSystem = ActorSystem("MsnReportingActorSystem")
  val msnManagementActorSystem = ActorSystem("MsnManagementActorSystem")

  val lynxReportingActorSystem = ActorSystem("LynxReportingActorSystem")


  // helper to mark a subprocess complete
  def complete_subprocess(task_key: String, cache: PendingCacheStructure, increment: Int=1): Unit = {
    val tasks = taskCache(Right(task_key))
    tasks.foreach(
      x =>
        x.processes.foreach(
          y =>
            if (y.changeDataId == cache.id)
              y.completedSubProcesses += increment
        )
    )
    setTaskCache(Right(task_key), tasks)
  }

  // helper to set number of subprocesses assigned to process
  def set_subprocess_count(task_key: String, cache: PendingCacheStructure, subprocess_count: Int=1): Unit = {
    setTaskCache(
      Right(task_key),
      taskCache(
        Right(task_key)
      ).map { x =>
        x.processes.foreach(y => y.changeDataId match {
          case _ if y.changeDataId == cache.id => y.subProcesses = subprocess_count
        })
        x
      }
    )
  }

  def mongoToCsv(dboList: List[DBObject]): String = {
    dboList.zipWithIndex.map{ case (row, idx) =>
      (if(idx == 0) {
        row.keys
      } else {
        row.values
      }).mkString(",")
    }.mkString("\n")
  }

  // Daemon for downloading facebook reporting Data
  /*def facebookReportingDaemon(
    initial_delay: FiniteDuration,
    interval: FiniteDuration
  ) = {
    Logger.info("Scheduling facebook reporting daemon (%s)".format(reportType.value))
    facebookApiAccountCollection.find().toList.map { accObj =>
      val mcc = dboToMcc(mccObj)
      googleCustomerCollection.find(
        DBObject("mccObjId" -> mcc._id),
        DBObject("customer" -> DBObject("$slice" -> -1))
      ).toList.map(acc =>
        dboToGoogleEntity[Customer](acc, "customer", None)
      ).map(account =>
        googleReportingActorSystem.scheduler.schedule(
          initial_delay,
          interval,
          googleReportActor,
          GoogleReportRequest(
            reportType = reportType,
            fields = fields,
            predicates = None,
            dateRange = com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionDateRangeType.YESTERDAY,
            oAuthData = mcc,
            customerId = Some(account.getCustomerId.toString),
            enableZeroImpressions = enableZeroImpressions
          )
        )
      )
    }
  }*/

  // Daemon for downloading google reporting Data
  def googleReportingDaemon(
    reportType: com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType,
    fields: Option[List[String]],
    enableZeroImpressions: Boolean,
    initial_delay: FiniteDuration,
    interval: FiniteDuration
   ) = {
    Logger.info("Scheduling google reporting daemon (%s)".format(reportType.value))
    googleMccCollection.find().toSeq.foreach { mccObj =>
      val mcc = dboToMcc(mccObj)
      googleCustomerCollection.find(DBObject("mccObjId" -> mcc._id)).toSeq.foreach { accObj =>
        val account = dboToGoogleEntity[ManagedCustomer](accObj, "customer", None)
        googleReportingActorSystem.scheduler.schedule(
          initial_delay,
          interval,
          googleReportingActorSystem.actorOf(Props(new GoogleReportActor)),
          GoogleReportRequest(
            reportType = reportType,
            fields = fields,
            predicates = None,
            dateRange = com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionDateRangeType.ALL_TIME,
            oAuthData = mcc,
            customerId = Some(account.getCustomerId.toString),
            enableZeroImpressions = enableZeroImpressions,
            accountName = account.getName
          )
        )
      }
    }
  }

  def msnReportingDaemon(
    reportType: MsnReportType.Value,
    initial_delay: FiniteDuration,
    interval: FiniteDuration
  ) = {
    Logger.info("Scheduling msn reporting daemon (%s)".format(reportType))
    msnApiAccountCollection.find().toList.foreach { apiAccountObj =>
      val apiAccount = models.mongodb.msn.Msn.dboToApiAccount(apiAccountObj)
      msnAccountInfoCollection.find(DBObject("apiAccountObjId" -> apiAccount._id)).toList.foreach { accountInfoObj =>
        val account = dboToMsnEntity[Account](accountInfoObj, "account", Some("accountObj"))
        val accountInfoWithCustomerData = dboToMsnEntity[AccountInfoWithCustomerData](accountInfoObj, "account", Some("accountInfoWithCustomerData"))
        try {
          msnReportingActorSystem.scheduler.schedule(
            initial_delay,
            interval,
            msnReportingActorSystem.actorOf(Props(new MsnReportActor)),
            MsnReportRequest(
              apiAccount,
              accountInfoWithCustomerData.getCustomerId,
              account.getId,
              MsnReportType.KeywordPerformanceReportRequest,
              startDate = DateTime.now.minusDays(1),
              endDate = DateTime.now
            )
          )
        } catch {
          case e: Exception =>
            //e.printStackTrace()
            Logger.info(s"Error Running report for ${apiAccount.name} - ${e.toString}")
        }
      }
    }
  }

  //Daemon for downloading google reporting Data
  def yahooReportingDaemon(
    reportType: Shared.GeminiReportType.Value,
    fields: List[Shared.GeminiReportField.Value],
    initial_delay: FiniteDuration,
    interval: FiniteDuration
  ) = {
    Logger.info(s"Scheduling yahoo reporting daemon ($reportType)")
    yahooApiAccountCollection.find().toList.map { apiAccountObj =>
      val apiAccount = models.mongodb.yahoo.Yahoo.dboToApiAccount(apiAccountObj)
      val yesterday = Calendar.getInstance
      yesterday.add(Calendar.DATE, -185)
      val today = Calendar.getInstance

      yahooAdvertiserCollection.find(DBObject("apiAccountObjId" -> apiAccount._id.get)).toList.map(adv =>
        yahooReportingActorSystem.scheduler.schedule(
          initial_delay,
          interval,
          yahooReportingActorSystem.actorOf(Props(new YahooReportActor)),
          GeminiReportRequest(
            reportType = reportType,
            fields = fields,
            filters = Some(List(GeminiReportFilter(
              field = GeminiReportField.Day,
              operator = GeminiReportFilterOperation.between,
              value = None,
              values = None,
              from = Some(GeminiReportDateFormat.format(yesterday.getTime)),
              to = Some(GeminiReportDateFormat.format(today.getTime))
            ))),
            oAuthData = apiAccount,
            reportFormat = ReportFormat.CSV
          )
        )
      )
    }
  }

  def lynxReportingDaemon(
    reportType: LynxReportType.Value,
    initial_delay: FiniteDuration,
    interval: FiniteDuration
  ) = {
    Logger.info(s"Scheduling lynx reporting daemon ($reportType)")
    try {
      lynxReportingActorSystem.scheduler.schedule(
        initial_delay,
        interval,
        lynxReportingActorSystem.actorOf(Props(new LynxReportActor)),
        LynxReportRequest(
          LynxReportType.TQReportingArrivalFactsReportRequest,
          startDate = new DateTime().minusDays(365),
          endDate = new DateTime()
        )
      )
    } catch {
      case e: Exception =>
        e.printStackTrace()
        Logger.info(s"Error Running lynx report - ${e.toString}")
    }
  }


  //Daemon for downloading google reporting Data
  def googleCustomerDaemon(
    reportType: com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionReportType,
    fields: Option[List[String]],
    enableZeroImpressions: Boolean,
    initial_delay: FiniteDuration,
    interval: FiniteDuration
  ) = {
    Logger.info("Scheduling reporting daemon (%s)".format(reportType.value))
    googleMccCollection.find().toList.map { mccObj =>
      val mcc = dboToMcc(mccObj)
      googleCustomerCollection.find(DBObject("mccObjId" -> mcc._id)).toList.map(dboToGoogleEntity[ManagedCustomer](_, "customer", None)).map(account =>
        googleReportingActorSystem.scheduler.schedule(
          initial_delay,
          interval,
          googleReportingActorSystem.actorOf(Props(new GoogleReportActor)),
          GoogleReportRequest(
            reportType = reportType,
            fields = fields,
            predicates = None,
            dateRange = com.google.api.ads.adwords.lib.jaxb.v201609.ReportDefinitionDateRangeType.ALL_TIME,
            oAuthData = mcc,
            customerId = Some(account.getCustomerId.toString),
            enableZeroImpressions = enableZeroImpressions,
            accountName = account.getName
          )
        )
      )
    }
  }

  // check if object is a case class
  def isCaseClass(obj: Any): Boolean = {
    val typeMirror = runtimeMirror(obj.getClass.getClassLoader)
    val instanceMirror = typeMirror.reflect(obj)
    val symbol = instanceMirror.symbol
    symbol.isCaseClass
  }

  def objectIdToFormString(objId: Option[ObjectId]) = objId match {case Some(id) => Some(id.toString) case None => None}

  def formStringToObjectId(objIdStr: Option[String]) = objIdStr match {case Some(id) => Some(new ObjectId(id)) case None => None}

  //todo: should be refactored for less redundancy should replace the individual *ToDbo methods
  def caseClassToDbo(c: Any): DBObject = c match {
    case x if isCaseClass(x) =>
      var dbo = DBObject.newBuilder
      val typeMirror = runtimeMirror(c.getClass.getClassLoader)
      val instanceMirror = typeMirror.reflect(c)
      for((name, idx) <- Utilities.getCaseClassParameter[instanceMirror.type].zipWithIndex) {
        dbo += (Utilities.getMethodName(name) -> (
          if(isCaseClass(c))
            caseClassToDbo(c.asInstanceOf[Product].productElement(idx))
          else
            c.asInstanceOf[Product].productElement(idx)
        ))
      }
      dbo.result
    case _ => throw new RuntimeException("Invalid Object passed to caseClassToDbo")
  }

  //Daemon for downloading Google MCC data (Campaign, Adgroup, Ad, Criterion...etc)
  def googleManagementDaemon(
    initial_delay: FiniteDuration,
    interval: FiniteDuration

  ) = {
      Logger.info("Scheduling MCC daemon (%s)")
      googleManagementActorSystem.scheduler.schedule(initial_delay, interval, googleManagementActorSystem.actorOf(Props(new MccActor)), None)
  }
  
  def facebookBusinessDaemon(initial_delay: FiniteDuration, interval: FiniteDuration) = {
    Logger.info("Scheduling Facebook Business Daemon")
    facebookBusinessActorSystem.scheduler.schedule(initial_delay, interval, facebookBusinessActorSystem.actorOf(Props(new FacebookBusinessActor)), None)
  }
 

  // get saved task key
  def taskKey(arg: Either[AuthenticatedRequest[Any], String]): String = {
    if(arg.isLeft)
      arg.left.get.session.get(Security.username).get + task_ext
    else
      arg.right.get + task_ext
  }

  // get pending cache key
  def pendingCacheKey(arg: Either[AuthenticatedRequest[Any], String]): String = {
    if(arg.isLeft)
      arg.left.get.subject.get.identifier + cache_ext
    else
      arg.right.get + cache_ext
  }

  // retrieve pending cache
  def pendingCache(arg: Either[AuthenticatedRequest[Any], String]): List[PendingCacheStructure] = {
    Await.result(redisClient.lrange(
      pendingCacheKey(
        arg.isLeft match {
          case true => Left(arg.left.get)
          case _ => Right(arg.right.get)
        }
      ), 0, -1
    ), 10 seconds).map(cacheStr =>
      gson.fromJson(cacheStr.utf8String, classOf[PendingCacheStructure])
    ).toList
  }

  // retrieve saved task cache
  def taskCache(arg: Either[AuthenticatedRequest[Any], String]): List[TaskStructure] = {
    Await.result(Shared.redisClient.lrange(
      taskKey(
        arg.isLeft match {
          case true => Left(arg.left.get)
          case _ => Right(arg.right.get)
        }
      ), 0, -1
    ), 10 seconds).map(cacheStr =>
      gson.fromJson(cacheStr.utf8String, classOf[TaskStructure])).toList
  }

  // set saved cache
  def setTaskCache(arg: Either[AuthenticatedRequest[Any], String], task: List[TaskStructure]) = {
    Shared.redisClient.lpush(
      taskKey(
        arg.isLeft match {
          case true => Left(arg.left.get)
          case _ => Right(arg.right.get)
        }
      ),
      task: _*
    )
  }

  // set pending cache
  def setPendingCache(arg: Either[AuthenticatedRequest[Any], String], pending_cache: List[PendingCacheStructure]) = {
    val key = pendingCacheKey(
      arg.isLeft match {
        case true => Left(arg.left.get)
        case _ => Right(arg.right.get)
      }
    )
    Shared.redisClient.del(key)
    Shared.redisClient.lpush(key, pending_cache: _*)
  }

  def titleCase(s: String): String = {
    s(0).toUpper + s.substring(1).toLowerCase
  }

  def deleteFiles(directory: String, filename: String) = for {
      files <- Option(new File(directory).listFiles)
      file <- files if file.getName.contains(filename)
    } file.delete()

  def doubleToMoney(v: Double): Double = {
    Math.round(v*100.0)/100.0
  }

  def stringToDateTime(v: Option[String], dateFormat: String="yyyy-MM-dd", defaultValue: String="2000-01-01"): DateTime = {
    DateTimeFormat.forPattern(dateFormat).parseDateTime(v.getOrElse(defaultValue))
  }
  
  /**
   * Iterate through users and look for admin accounts.  if there are no admins,
   * create a default one and print the credentials to the log so that the system
   * is recoverable.
   */
  def createAdminRecoveryAccountIfNecessary = {
    var hasAdminAccount: Boolean = false
    UserAccount.userAccountCollection.find().toArray.foreach(
        userAcctObj => {
          var userAccount = UserAccount.dboToUserAccount(userAcctObj)
          if(UserAccount.isAdministrator(userAccount)){
            hasAdminAccount = true
          }
        }
    )
    
    // No admin, create one
    if(!hasAdminAccount){
      
      // Create security role w/ admin privileges only.
      val securityRole = SecurityRole(
          _id = Some(new ObjectId),
          roleName = "TQadmin",
          permissions = List(PermissionGroup.Administrator).toArray
      )
      
      Logger.info("""Created new security role "admin" for recovery account""")
      SecurityRole.securityRoleCollection.insert(SecurityRole.securityRoleToDbo(securityRole))
      var newPassword = generatePassword()
      
      // Creat user
      val adminUser = UserAccount(
          _id = Some(new ObjectId),
          userName = "TQAdmin",
          password = encryptPassword(newPassword),
          email = Play.configuration.getString("admin_email").getOrElse(throw new Exception("Missing Administrator Email Configuration Setting")),
          advertiserIds = List(),
          securityRoles = Array(securityRole._id.get)
      )
      
      Logger.info(s"Created admin recovery account - Username: ${adminUser.userName} Password: $newPassword")
      UserAccount.userAccountCollection.insert(UserAccount.userAccountToDbo(adminUser))
    }
  }
  
  /**
   * Allows for the safe usage of a stream within a scope and closes upon completion
   * or termination. 
   */
  def using[A <: {def close(): Unit}, B](param: A)(f: A => B): B = {
    try {
      f(param)
    } finally {
      param.close()
    }
  }

  def generatePassword(): String = {
    val rand: SecureRandom = new SecureRandom
    val passChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    val passLength = 8
    var sb = new StringBuilder(passLength)
    for( i <- 0 until passLength){
      sb.append( passChars.charAt( rand.nextInt(passChars.length())))
    }

    sb.toString
  }

  def encryptPassword(password: String): String = {
    BCrypt.hashpw(password, BCrypt.gensalt)
  }

  def checkPasswordsMatch(entered: String, actual: String): Boolean = {
    BCrypt.checkpw(entered, actual)
  }
  
  def microToDollars(microAmt: Long): Double = microAmt / 1000000.0
  def dollarsToMicro(dollarAmt: Double): Long = (dollarAmt * 1000000).toLong
}