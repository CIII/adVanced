package controllers.google.mcc.account.campaign.criterion

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.google.api.ads.adwords.axis.v201609.cm.{Campaign, CampaignCriterion, Proximity}
import com.mongodb.casbah.Imports._
import helpers.google.mcc.account.campaign.CampaignControllerHelper._
import helpers.google.mcc.account.campaign.criterion.CampaignProximityControllerHelper._
import models.mongodb._
import models.mongodb.google.Google._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import security.HandlerKeys

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class CampaignProximityController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {

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
        googleCriterionCollection,
        Some("criterionType" -> "Proximity")
      )))
  }

  def campaignProximity(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.campaign.criterion.proximity.campaign_proximity(
        googleCriterionCollection.find(DBObject("criterionType" -> "CampaignProximity")).skip(page * pageSize).limit(pageSize).toList.map(dboToGoogleEntity[CampaignCriterion](_, "criterion", None)),
        page,
        pageSize,
        orderBy,
        filter,
        googleCriterionCollection.count(DBObject("criterionType" -> "Proximity")),
        pendingCache(Left(request))
          .filter(x =>
            x.trafficSource == TrafficSource.GOOGLE
              && x.changeCategory == ChangeCategory.CAMPAIGN_PROXIMITY
          )
      )))
  }

  def newCampaignProximity = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.campaign.criterion.proximity.new_campaign_proximity(
        campaignProximityForm,
        googleCampaignCollection.find().toList.map(dboToGoogleEntity[Campaign](_, "campaign", None)),
        pendingCache(Left(request))
          .filter(x =>
            x.changeType == ChangeType.NEW
              && x.trafficSource == TrafficSource.GOOGLE
              && x.changeCategory == ChangeCategory.CAMPAIGN
          )
          .map(x => dboToCampaignForm(x.changeData.asDBObject)),
        List()
      )))
  }

  def createCampaignProximity = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignProximityForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(views.html.google.mcc.account.campaign.criterion.proximity.new_campaign_proximity(
            formWithErrors,
            googleCampaignCollection.find().toList.map(dboToGoogleEntity[Campaign](_, "campaign", None)),
            pendingCache(Left(request))
              .filter(x =>
                x.changeType == ChangeType.NEW &&
                  x.trafficSource == TrafficSource.GOOGLE &&
                  x.changeCategory == ChangeCategory.CAMPAIGN
              )
              .map(x => dboToCampaignForm(x.changeData.asDBObject)),
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
              changeData = campaignProximityFormToDbo(campaign_proximity)
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
            val field_names = Utilities.getCaseClassParameter[CampaignCriterion]
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
                      changeData = campaignProximityFormToDbo(campaign_proximity)
                    )
                  )
              )
            }
          }
        }
        if (error_list.nonEmpty) {
          Future(BadRequest(views.html.google.mcc.account.campaign.criterion.proximity.new_campaign_proximity(
            campaignProximityForm,
            googleCampaignCollection.find().toList.map(dboToGoogleEntity[Campaign](_, "campaign", None)),
            pendingCache(Left(request))
              .filter(x =>
                x.changeType == ChangeType.NEW
                  && x.trafficSource == TrafficSource.GOOGLE
                  && x.changeCategory == ChangeCategory.CAMPAIGN
              )
              .map(x => dboToCampaignForm(x.changeData.asDBObject)),
            error_list.toList
          )))
        } else {
          Future(Redirect(controllers.google.mcc.account.campaign.criterion.routes.CampaignProximityController.campaignProximity()))
        }
  }

  def editCampaignProximity(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      googleCriterionCollection.findOne(DBObject("criterionApiId" -> api_id, "criterionType" -> "Proximity")) match {
        case None => Future(Redirect(controllers.routes.DashboardController.dashboard()))
        case Some(campaign_criterion_obj) =>
          val campaigns = googleCampaignCollection.find().toList
          def campaign_proximity = dboToGoogleEntity[CampaignCriterion](campaign_criterion_obj, "criterion", None)
          Future(Ok(views.html.google.mcc.account.campaign.criterion.proximity.edit_campaign_proximity(
            api_id,
            campaignProximityForm.fill(
              CampaignProximityForm(
                parent = controllers.Google.CampaignCriterionParent(
                  mccObjId = campaign_criterion_obj.getAsOrElse[Option[String]]("mccObjId", None),
                  customerApiId = campaign_criterion_obj.getAsOrElse[Option[Long]]("customerApiId", None),
                  campaignApiId = campaign_criterion_obj.getAsOrElse[Option[Long]]("campaignApiId", None)
                ),
                apiId = Some(campaign_proximity.getCriterion.getId),
                isNegative = Some(campaign_proximity.getIsNegative),
                latitudeInMicroDegrees = Some(campaign_proximity.getCriterion.asInstanceOf[Proximity].getGeoPoint.getLatitudeInMicroDegrees),
                longitudeInMicroDegrees = Some(campaign_proximity.getCriterion.asInstanceOf[Proximity].getGeoPoint.getLongitudeInMicroDegrees),
                radiusDistanceUnits = campaign_proximity.getCriterion.asInstanceOf[Proximity].getRadiusDistanceUnits.toString,
                radiusInUnits = campaign_proximity.getCriterion.asInstanceOf[Proximity].getRadiusInUnits,
                streetAddress = Some(campaign_proximity.getCriterion.asInstanceOf[Proximity].getAddress.getStreetAddress),
                streetAddress2 = Some(campaign_proximity.getCriterion.asInstanceOf[Proximity].getAddress.getStreetAddress2),
                cityName = Some(campaign_proximity.getCriterion.asInstanceOf[Proximity].getAddress.getCityName),
                provinceCode = Some(campaign_proximity.getCriterion.asInstanceOf[Proximity].getAddress.getProvinceCode),
                provinceName = Some(campaign_proximity.getCriterion.asInstanceOf[Proximity].getAddress.getProvinceName),
                postalCode = Some(campaign_proximity.getCriterion.asInstanceOf[Proximity].getAddress.getPostalCode),
                countryCode = Some(campaign_proximity.getCriterion.asInstanceOf[Proximity].getAddress.getCountryCode),
                bidModifier = Some(campaign_proximity.getBidModifier)
              )
            ),
            campaigns.map(dboToGoogleEntity[Campaign](_, "campaign", None)),
            pendingCache(Left(request))
              .filter(x =>
                x.changeType == ChangeType.NEW
                  && x.trafficSource == TrafficSource.GOOGLE
                  && x.changeCategory == ChangeCategory.CAMPAIGN
              )
              .map(x => dboToCampaignForm(x.changeData.asDBObject))
          )))
      }
  }

  def saveCampaignProximity(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignProximityForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(views.html.google.mcc.account.campaign.criterion.proximity.edit_campaign_proximity(
          api_id,
          formWithErrors,
          googleCampaignCollection.find().toList.map(dboToGoogleEntity[Campaign](_, "campaign", None)),
          pendingCache(Left(request))
            .filter(x =>
              x.changeType == ChangeType.NEW
                && x.trafficSource == TrafficSource.GOOGLE
                && x.changeCategory == ChangeCategory.CAMPAIGN
            )
            .map(x => dboToCampaignForm(x.changeData.asDBObject))
        ))),
        campaign_proximity => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.CAMPAIGN_PROXIMITY,
              changeData = campaignProximityFormToDbo(campaign_proximity)
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
          changeData = DBObject("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.google.mcc.account.campaign.criterion.routes.CampaignProximityController.campaignProximity()))
  }
}
