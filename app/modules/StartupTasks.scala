package modules

import javax.inject.{Inject, Singleton}
import org.apache.pekko.actor.ActorSystem
import play.api.{Configuration, Logging}
import play.api.libs.ws.WSClient
import services.{MongoService, RedisService}

import scala.concurrent.ExecutionContext

/**
 * Replaces Global.onStart. All daemon scheduling and startup
 * initialization should be done here.
 *
 * TODO: Re-enable daemon scheduling once Google Ads API,
 * MSN, Lynx, and Facebook integrations are migrated to new SDK versions.
 */
@Singleton
class StartupTasks @Inject()(
  actorSystem: ActorSystem,
  configuration: Configuration,
  mongoService: MongoService,
  redisService: RedisService,
  ws: WSClient
)(implicit ec: ExecutionContext) extends Logging {

  logger.info("adVanced application starting up")

  // Wire MongoDB collections into model companion objects
  import models.mongodb.UserAccount
  import models.mongodb.{Task, ChartTemplate, SecurityRole}
  import models.mongodb.google.Google
  import models.mongodb.facebook.Facebook
  import models.mongodb.msn.Msn
  import models.mongodb.lynx.TQReporting

  UserAccount.userAccountCollection = mongoService.collection("userAccounts")
  UserAccount.securityRoleCollection = mongoService.collection("securityRoles")
  SecurityRole.securityRoleCollection = mongoService.collection("securityRoles")
  Task.taskCollection = mongoService.collection("tasks")
  ChartTemplate.chartTemplateCollection = mongoService.collection("chartTemplates")
  TQReporting.arrivalFactCollection = mongoService.collection("arrivalFacts")

  Google.googleMccCollection = mongoService.collection("googleMccs")
  Google.googleCustomerCollection = mongoService.collection("googleCustomers")
  Google.googleCampaignCollection = mongoService.collection("googleCampaigns")
  Google.googleAdGroupCollection = mongoService.collection("googleAdGroups")
  Google.googleAdCollection = mongoService.collection("googleAds")
  Google.googleCriterionCollection = mongoService.collection("googleCriteria")
  Google.googleBudgetCollection = mongoService.collection("googleBudgets")
  Google.googleBiddingStrategyCollection = mongoService.collection("googleBiddingStrategies")
  Google.setMongoService(mongoService)

  Facebook.facebookApiAccountCollection = mongoService.collection("facebookApiAccounts")
  Facebook.facebookCampaignCollection = mongoService.collection("facebookCampaigns")
  Facebook.facebookAdSetCollection = mongoService.collection("facebookAdSets")
  Facebook.facebookAdCollection = mongoService.collection("facebookAds")
  Facebook.facebookSplitTestCollection = mongoService.collection("facebookSplitTests")
  Facebook.facebookBusinessAccountCollection = mongoService.collection("facebookBusinessAccounts")
  Facebook.configuration = configuration
  Facebook.ws = ws

  Msn.msnCustomerCollection = mongoService.collection("msnCustomers")
  Msn.msnReportCollection = mongoService.collection("msnReports")
  Msn.msnAccountInfoCollection = mongoService.collection("msnAccountInfo")
  Msn.msnApiAccountCollection = mongoService.collection("msnApiAccounts")

  logger.info("MongoDB collections initialized")

  // TODO: Schedule Google reporting daemons
  // TODO: Schedule MSN reporting daemon
  // TODO: Schedule Lynx reporting daemon
  // TODO: Schedule Google management daemon
  // TODO: Schedule Facebook business daemon
  // TODO: Create admin recovery account if necessary

  logger.info("adVanced startup tasks initialized")
}
