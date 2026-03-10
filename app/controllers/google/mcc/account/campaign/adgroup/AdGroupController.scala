package controllers.google.mcc.account.campaign.adgroup

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import helpers.google.mcc.account.campaign.CampaignControllerHelper._
import helpers.google.mcc.account.campaign.adgroup.AdGroupControllerHelper._
import models.mongodb._
import models.mongodb.MongoExtensions._
import models.mongodb.google.Google._
import play.api.i18n.I18nSupport
import play.api.mvc._
import security.HandlerKeys
import util.charts.ChartMetaData.getMetaData
import util.charts._
import util.charts.client.{ActionColumn, ChartColumn}
import util.charts.client.ChartColumn.ColumnDataType._
import util.charts.client.ChartColumn.ColumnType._
import util.charts.performance.GooglePerformanceCharts._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import models.mongodb.google.GoogleAdGroupPerformance
import models.mongodb.google.GoogleCampaignPerformance
import models.mongodb.performance.PerformanceEntityFilter
import models.mongodb.google.GooglePerformance

class AdGroupController @Inject()(val controllerComponents: ControllerComponents, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {

  def json = Action {
    implicit request =>
      Ok(controllers.json(
        request,
        List(
          "mccObjId",
          "customerObjId",
          "customerApiId",
          "campaignObjId",
          "campaignApiId",
          "adGroupObjId",
          "adGroupApiId"
        ),
        "adGroup",
        googleAdGroupCollection.namespace.getCollectionName
      ))
  }

  def attribution = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.campaign.adgroup.adgroup_attribution(
        new GoogleAdGroupPerformanceChart(
          getMetaData(
            request,
            List(GoogleAdGroupPerformance.adGroupHtmlField,
              GoogleAdGroupPerformance.adGroupStateField,
              GooglePerformance.ctrField,
              GooglePerformance.cpcField,
              GooglePerformance.avgPosField,
              GooglePerformance.costPerConvField,
              GooglePerformance.cRateField,
              GooglePerformance.viewThroughConvField
            ),
            request.getQueryString("filterById") match {
              case Some(id) =>
                List(new PerformanceEntityFilter(GoogleCampaignPerformance.campaignIdField, "eq", List(id.toLong)))
              case _ => List()
            },
            defaultGoogleMetaData
          )
        )
      )))
  }

  def attributionCSV = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future.successful(Ok.sendFile(
        new GoogleAdGroupPerformanceChart(
          getMetaData(
            request,
            List(GoogleAdGroupPerformance.adGroupHtmlField),
            request.getQueryString("filterById") match {
              case Some(id) =>
                List(new PerformanceEntityFilter(GoogleCampaignPerformance.campaignIdField, "eq", List(id.toLong)))
              case _ => List()
            },
            defaultGoogleMetaData
          )
        ).exportCsv("AdGroupAttribution.csv")
      ))
  }
  def adGroups = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[AdGroup] with Document-based access
      val adGroupDocs = googleAdGroupCollection.find().toList
      Future(Ok(views.html.google.mcc.account.campaign.adgroup.ad_groups(
        new ClientChart(
          List(
            new ChartColumn("id", "", "Id", number, dimension),
            new ChartColumn("name", "", "Name", string, dimension),
            new ChartColumn("status", "", "Status", string, dimension),
            new ActionColumn((rowValues: List[Any]) => "/google/mcc/account/campaign/adgroup/%s/".format(rowValues(0).toString))
          ),
          adGroupDocs
        ),
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.GOOGLE
                && x.changeCategory == ChangeCategory.AD_GROUP
          )
      )))
  }

  def newAdGroup = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Campaign] with Document-based access
      val campaignDocs = googleCampaignCollection.find().toList
      Future(Ok(views.html.google.mcc.account.campaign.adgroup.new_ad_group(
        adGroupForm,
        campaignDocs,
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.GOOGLE
                && x.changeCategory == ChangeCategory.CAMPAIGN
                && x.changeType == ChangeType.NEW
          )
          .map(x => documentToCampaignForm(x.changeData)),
        List()
      )))
  }

  def createAdGroup = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      adGroupForm.bindFromRequest.fold(
        formWithErrors => {
          // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Campaign] with Document-based access
          val campaignDocs = googleCampaignCollection.find().toList
          Future(BadRequest(
            views.html.google.mcc.account.campaign.adgroup.new_ad_group(
              formWithErrors,
              campaignDocs,
              pendingCache(Left(request))
                .filter(x =>
                  x.trafficSource == TrafficSource.GOOGLE
                    && x.changeCategory == ChangeCategory.CAMPAIGN
                    && x.changeType == ChangeType.NEW
                )
                .map(x => documentToCampaignForm(x.changeData)),
              List()
            )
          ))
        },
        ad_group => {
          // TODO: Migrate to RedisService injection - redisClient.lpush removed
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.AD_GROUP,
              changeData = adGroupFormToDocument(ad_group)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.adgroup.routes.AdGroupController.adGroups))
        }
      )
  }


  def bulkNewAdGroup = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      request.body.file("bulk").foreach {
        bulk => {
          // TODO: Migrate to Google Ads API v18 - replaced Utilities.getCaseClassParameter[AdGroup] with AdGroupForm
          val field_names = Utilities.getCaseClassParameter[AdGroupForm]
          val ad_group_data_list = Utilities.bulkImport(bulk, field_names)
          for (((ad_group_data, action), index) <- ad_group_data_list.zipWithIndex) {
            adGroupForm.bind(ad_group_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              ad_group => {
                setPendingCache(
                  Left(request),
                  pendingCache(Left(request)) :+ PendingCacheStructure(
                    id = pendingCache(Left(request)).length + 1,
                    changeType = ChangeType.withName(action),
                    trafficSource = TrafficSource.GOOGLE,
                    changeCategory = ChangeCategory.AD_GROUP,
                    changeData = adGroupFormToDocument(ad_group)
                  )
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Campaign] with Document-based access
        val campaignDocs = googleCampaignCollection.find().toList
        Future(BadRequest(views.html.google.mcc.account.campaign.adgroup.new_ad_group(
          adGroupForm,
          campaignDocs,
          pendingCache(Left(request))
            .filter(x =>
              x.trafficSource == TrafficSource.GOOGLE
                && x.changeCategory == ChangeCategory.AD_GROUP
                && x.changeType == ChangeType.NEW
            )
            .map(x => documentToCampaignForm(x.changeData)),
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.google.mcc.account.campaign.adgroup.routes.AdGroupController.adGroups))
      }
    }
  }

  def editAdGroup(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      googleAdGroupCollection.findOne(Document("apiId" -> api_id)) match {
        case None => Future(Redirect(controllers.routes.DashboardController.dashboard))
        case Some(ad_group_obj) =>
          // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Campaign] with Document-based access
          val campaigns = googleCampaignCollection.find().toList
          val adGroupDoc = Option(ad_group_obj.toBsonDocument.get("adGroup")).map(v => Document(v.asDocument())).flatMap(d => Option(d.toBsonDocument.get("object")).map(v => Document(v.asDocument())))
          val adGroupId = adGroupDoc.flatMap(d => Option(d.getLong("id")).map(_.toLong)).getOrElse(0L)
          Future(Ok(views.html.google.mcc.account.campaign.adgroup.edit_ad_group(
            adGroupId,
            adGroupForm.fill(adGroupDocumentToAdGroupForm(ad_group_obj)),
            campaigns,
            pendingCache(Left(request)).filter(x =>
              x.trafficSource == TrafficSource.GOOGLE
                && x.changeCategory == ChangeCategory.CAMPAIGN
                && x.changeType == ChangeType.NEW
            ).map(x => documentToCampaignForm(x.changeData))
          )))
      }
  }

  // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[AdGroup] and AdWords getter calls with Document-based access
  def adGroupDocumentToAdGroupForm(adGroupObj: Document): AdGroupForm = {
    val adGroupDoc = Option(adGroupObj.toBsonDocument.get("adGroup")).map(v => Document(v.asDocument())).flatMap(d => Option(d.toBsonDocument.get("object")).map(v => Document(v.asDocument())))
    AdGroupForm(
      AdGroupParent(
        mccObjId = Option(adGroupObj.getString("mccObjId")),
        customerApiId = Option(adGroupObj.getLong("customerApiId")).map(_.toLong),
        campaignApiId = Option(adGroupObj.getLong("campaignApiId")).map(_.toLong)
      ),
      apiId = adGroupDoc.flatMap(d => Option(d.getLong("id")).map(_.toLong)),
      name = adGroupDoc.map(_.getString("name")).getOrElse(""),
      status = adGroupDoc.map(d => Option(d.getString("status")).getOrElse("ENABLED")).getOrElse("ENABLED"),
      maxCpc = adGroupDoc.flatMap(d =>
        Option(d.toBsonDocument.get("biddingStrategyConfiguration")).map(v => Document(v.asDocument())).flatMap(bsc =>
          Option(bsc.toBsonDocument.get("bids")).filter(_.isArray).flatMap { bidsVal =>
            import scala.jdk.CollectionConverters._
            bidsVal.asArray.getValues.asScala
              .collect { case v if v.isDocument => Document(v.asDocument()) }
              .find(b => Option(b.getString("bidsType")).exists(_.equalsIgnoreCase("CpcBid")))
              .flatMap(cpcBid =>
                Option(cpcBid.toBsonDocument.get("bid")).map(v => Document(v.asDocument())).flatMap(bid =>
                  Option(bid.getLong("microAmount")).map(m => microToDollars(m.toLong))
                )
              )
          }
        )
      ),
      contentBidCriterionTypeGroup = adGroupDoc.flatMap(d => Option(d.getString("contentBidCriterionTypeGroup")))
    )
  }

  def saveAdGroup(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      adGroupForm.bindFromRequest.fold(formWithErrors => {
        // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Campaign] with Document-based access
        val campaignDocs = googleCampaignCollection.find().toList
        Future(BadRequest(
          views.html.google.mcc.account.campaign.adgroup.edit_ad_group(
            api_id,
            formWithErrors,
            campaignDocs,
            pendingCache(Left(request))
              .filter(x =>
                x.trafficSource == TrafficSource.GOOGLE
                  && x.changeCategory == ChangeCategory.CAMPAIGN
                  && x.changeType == ChangeType.NEW
              )
              .map(x => documentToCampaignForm(x.changeData))
          )
        ))
      },
        ad_group => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.AD_GROUP,
              changeData = adGroupFormToDocument(ad_group)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.adgroup.routes.AdGroupController.adGroups))
        }
      )
  }

  def deleteAdGroup(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
          id = pendingCache(Left(request)).length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.GOOGLE,
          changeCategory = ChangeCategory.AD_GROUP,
          changeData = Document("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.google.mcc.account.campaign.adgroup.routes.AdGroupController.adGroups))
  }

  def maxCpc(adGroupId: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      request.getQueryString("dollarAmt") match {
        case Some(dollarAmtStr) =>
          val dollarAmt = dollarAmtStr.toDouble
          googleAdGroupCollection.findOne(Document("adGroup.object.id" -> adGroupId)) match {
            case Some(adGroupObj) =>
              val adGroupFormData = adGroupDocumentToAdGroupForm(adGroupObj).copy(maxCpc = Some(dollarAmt))
              setPendingCache(
                Left(request),
                pendingCache(Left(request)) :+ PendingCacheStructure(
                  id = pendingCache(Left(request)).length + 1,
                  changeType = ChangeType.UPDATE,
                  trafficSource = TrafficSource.GOOGLE,
                  changeCategory = ChangeCategory.AD_GROUP,
                  changeData = adGroupFormToDocument(adGroupFormData)
                )
              )

              Future(Ok(s"Successfully created update task for AdGroup $adGroupId"))

            case _ => Future(BadRequest("No AdGroup found for specified Id"))
          }

        case _ => Future(BadRequest("dollarAmt is a required field"))
      }
  }
}
