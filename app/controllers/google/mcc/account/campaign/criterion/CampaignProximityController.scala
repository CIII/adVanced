package controllers.google.mcc.account.campaign.criterion

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import helpers.google.mcc.account.campaign.CampaignControllerHelper._
import helpers.google.mcc.account.campaign.criterion.CampaignProximityControllerHelper._
import models.mongodb._
import models.mongodb.MongoExtensions._
import models.mongodb.google.Google._
import play.api.i18n.I18nSupport
import play.api.mvc._
import security.HandlerKeys

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class CampaignProximityController @Inject()(val controllerComponents: ControllerComponents, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {

  def json = Action.async {
    implicit request =>
      Future(Ok(controllers.json(
        request,
        List(
          "mccObjId",
          "mccApiId",
          "customerObjId",
          "customerApiId",
          "campaignObjId",
          "campaignApiId"
        ),
        "criterion",
        googleCriterionCollection.namespace.getCollectionName,
        Some("criterionType" -> "Proximity")
      )))
  }

  def campaignProximity(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[CampaignCriterion] with Document-based access
      val criterionDocs = googleCriterionCollection.find(Document("criterionType" -> "CampaignProximity")).skip(page * pageSize).limit(pageSize).toList
      Future(Ok(views.html.google.mcc.account.campaign.criterion.proximity.campaign_proximity(
        criterionDocs,
        page,
        pageSize,
        orderBy,
        filter,
        googleCriterionCollection.count(Document("criterionType" -> "Proximity")).toInt,
        pendingCache(Left(request))
          .filter(x =>
            x.trafficSource == TrafficSource.GOOGLE
              && x.changeCategory == ChangeCategory.CAMPAIGN_PROXIMITY
          )
      )))
  }

  def newCampaignProximity = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Campaign] with Document-based access
      val campaignDocs = googleCampaignCollection.find().toList
      Future(Ok(views.html.google.mcc.account.campaign.criterion.proximity.new_campaign_proximity(
        campaignProximityForm,
        campaignDocs,
        pendingCache(Left(request))
          .filter(x =>
            x.changeType == ChangeType.NEW
              && x.trafficSource == TrafficSource.GOOGLE
              && x.changeCategory == ChangeCategory.CAMPAIGN
          )
          .map(x => documentToCampaignForm(x.changeData)),
        List()
      )))
  }

  def createCampaignProximity = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignProximityForm.bindFromRequest.fold(
        formWithErrors => {
          // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Campaign] with Document-based access
          val campaignDocs = googleCampaignCollection.find().toList
          Future(BadRequest(views.html.google.mcc.account.campaign.criterion.proximity.new_campaign_proximity(
            formWithErrors,
            campaignDocs,
            pendingCache(Left(request))
              .filter(x =>
                x.changeType == ChangeType.NEW &&
                  x.trafficSource == TrafficSource.GOOGLE &&
                  x.changeCategory == ChangeCategory.CAMPAIGN
              )
              .map(x => documentToCampaignForm(x.changeData)),
            List()
          )))
        },
        campaign_proximity => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.CAMPAIGN_PROXIMITY,
              changeData = campaignProximityFormToDocument(campaign_proximity)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.criterion.routes.CampaignProximityController.campaignProximity()))
        }
      )
  }

  def bulkNewCampaignProximity = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
      implicit request =>
        var error_list = new ListBuffer[String]()
        request.body.file("bulk").foreach {
          bulk => {
            // TODO: Migrate to Google Ads API v18 - replaced Utilities.getCaseClassParameter[CampaignCriterion] with CampaignProximityForm
            val field_names = Utilities.getCaseClassParameter[CampaignProximityForm]
            val proximity_data_list = Utilities.bulkImport(bulk, field_names)
            for (((proximity_data, action), index) <- proximity_data_list.zipWithIndex) {
              campaignProximityForm.bind(proximity_data.map(kv => (kv._1, kv._2)).toMap).fold(
                formWithErrors => {
                  error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
                },
                campaign_proximity =>
                  setPendingCache(
                    Left(request),
                    pendingCache(Left(request)) :+ PendingCacheStructure(
                      id = pendingCache(Left(request)).length + 1,
                      changeType = ChangeType.withName(action.toUpperCase),
                      trafficSource = TrafficSource.GOOGLE,
                      changeCategory = ChangeCategory.CAMPAIGN_KEYWORD,
                      changeData = campaignProximityFormToDocument(campaign_proximity)
                    )
                  )
              )
            }
          }
        }
        if (error_list.nonEmpty) {
          // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Campaign] with Document-based access
          val campaignDocs = googleCampaignCollection.find().toList
          Future(BadRequest(views.html.google.mcc.account.campaign.criterion.proximity.new_campaign_proximity(
            campaignProximityForm,
            campaignDocs,
            pendingCache(Left(request))
              .filter(x =>
                x.changeType == ChangeType.NEW
                  && x.trafficSource == TrafficSource.GOOGLE
                  && x.changeCategory == ChangeCategory.CAMPAIGN
              )
              .map(x => documentToCampaignForm(x.changeData)),
            error_list.toList
          )))
        } else {
          Future(Redirect(controllers.google.mcc.account.campaign.criterion.routes.CampaignProximityController.campaignProximity()))
        }
  }

  def editCampaignProximity(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      googleCriterionCollection.findOne(Document("criterionApiId" -> api_id, "criterionType" -> "Proximity")) match {
        case None => Future(Redirect(controllers.routes.DashboardController.dashboard))
        case Some(campaign_criterion_obj) =>
          // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[CampaignCriterion]/Proximity with Document-based access
          val campaigns = googleCampaignCollection.find().toList
          val criterionDoc = Option(campaign_criterion_obj.toBsonDocument.get("criterion")).map(v => Document(v.asDocument())).flatMap(d => Option(d.toBsonDocument.get("object")).map(v => Document(v.asDocument())))
          val innerCriterionDoc = criterionDoc.flatMap(d => Option(d.toBsonDocument.get("criterion")).map(v => Document(v.asDocument())))
          val geoPointDoc = innerCriterionDoc.flatMap(d => Option(d.toBsonDocument.get("geoPoint")).map(v => Document(v.asDocument())))
          val addressDoc = innerCriterionDoc.flatMap(d => Option(d.toBsonDocument.get("address")).map(v => Document(v.asDocument())))
          Future(Ok(views.html.google.mcc.account.campaign.criterion.proximity.edit_campaign_proximity(
            api_id,
            campaignProximityForm.fill(
              CampaignProximityForm(
                parent = controllers.Google.CampaignCriterionParent(
                  mccObjId = Option(campaign_criterion_obj.getString("mccObjId")),
                  customerApiId = Option(campaign_criterion_obj.getLong("customerApiId")).map(_.toLong),
                  campaignApiId = Option(campaign_criterion_obj.getLong("campaignApiId")).map(_.toLong)
                ),
                apiId = innerCriterionDoc.flatMap(d => Option(d.getLong("id")).map(_.toLong)),
                isNegative = criterionDoc.flatMap(d => Option(d.getBoolean("isNegative")).map(_.booleanValue())),
                latitudeInMicroDegrees = geoPointDoc.flatMap(d => Option(d.getInteger("latitudeInMicroDegrees")).map(_.toInt)),
                longitudeInMicroDegrees = geoPointDoc.flatMap(d => Option(d.getInteger("longitudeInMicroDegrees")).map(_.toInt)),
                radiusDistanceUnits = innerCriterionDoc.flatMap(d => Option(d.getString("radiusDistanceUnits"))).getOrElse("MILES"),
                radiusInUnits = innerCriterionDoc.flatMap(d => Option(d.getDouble("radiusInUnits")).map(_.toDouble)).getOrElse(0.0),
                streetAddress = addressDoc.flatMap(d => Option(d.getString("streetAddress"))),
                streetAddress2 = addressDoc.flatMap(d => Option(d.getString("streetAddress2"))),
                cityName = addressDoc.flatMap(d => Option(d.getString("cityName"))),
                provinceCode = addressDoc.flatMap(d => Option(d.getString("provinceCode"))),
                provinceName = addressDoc.flatMap(d => Option(d.getString("provinceName"))),
                postalCode = addressDoc.flatMap(d => Option(d.getString("postalCode"))),
                countryCode = addressDoc.flatMap(d => Option(d.getString("countryCode"))),
                bidModifier = criterionDoc.flatMap(d => Option(d.getDouble("bidModifier")).map(_.toDouble))
              )
            ),
            campaigns,
            pendingCache(Left(request))
              .filter(x =>
                x.changeType == ChangeType.NEW
                  && x.trafficSource == TrafficSource.GOOGLE
                  && x.changeCategory == ChangeCategory.CAMPAIGN
              )
              .map(x => documentToCampaignForm(x.changeData))
          )))
      }
  }

  def saveCampaignProximity(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignProximityForm.bindFromRequest.fold(
        formWithErrors => {
          // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Campaign] with Document-based access
          val campaignDocs = googleCampaignCollection.find().toList
          Future(BadRequest(views.html.google.mcc.account.campaign.criterion.proximity.edit_campaign_proximity(
            api_id,
            formWithErrors,
            campaignDocs,
            pendingCache(Left(request))
              .filter(x =>
                x.changeType == ChangeType.NEW
                  && x.trafficSource == TrafficSource.GOOGLE
                  && x.changeCategory == ChangeCategory.CAMPAIGN
              )
              .map(x => documentToCampaignForm(x.changeData))
          )))
        },
        campaign_proximity => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.CAMPAIGN_PROXIMITY,
              changeData = campaignProximityFormToDocument(campaign_proximity)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.criterion.routes.CampaignProximityController.campaignProximity()))
        }
      )
  }

  def deleteCampaignProximity(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
          id = pendingCache(Left(request)).length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.GOOGLE,
          changeCategory = ChangeCategory.CAMPAIGN_PROXIMITY,
          changeData = Document("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.google.mcc.account.campaign.criterion.routes.CampaignProximityController.campaignProximity()))
  }
}
