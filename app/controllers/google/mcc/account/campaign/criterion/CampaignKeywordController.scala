package controllers.google.mcc.account.campaign.criterion

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.google.api.ads.adwords.axis.v201609.cm.{Campaign, CampaignCriterion, Keyword}
import com.mongodb.casbah.Imports._
import helpers.google.mcc.account.campaign.CampaignControllerHelper._
import helpers.google.mcc.account.campaign.criterion.CampaignKeywordControllerHelper._
import models.mongodb._
import models.mongodb.google.Google._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import security.HandlerKeys

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class CampaignKeywordController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {

  def json = Action.async {
    implicit request =>
      Future(Ok(controllers.json(
        request,
        List(
          "mccObjId",
          "customerObjId",
          "customerApiId",
          "campaignObjId",
          "campaignApiId",
          "tsecs"
        ),
        "criterion",
        googleCriterionCollection,
        Some("criterionType" -> "Keyword")
      )))
  }

  def campaignKeywords(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.campaign.criterion.keyword.campaign_keywords(
        googleCriterionCollection.find(DBObject("criterionType" -> "CampaignKeyword")).skip(page * pageSize).limit(pageSize).toList.map(dboToGoogleEntity[CampaignCriterion](_, "criterion", None)),
        page,
        pageSize,
        orderBy,
        filter,
        googleCriterionCollection.count(DBObject("criterionType" -> "CampaignKeyword")),
        pendingCache(Left(request))
          .filter(x =>
            x.trafficSource == TrafficSource.GOOGLE
              && x.changeCategory == ChangeCategory.KEYWORD
          )
      )))
  }

  def newCampaignKeyword = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
      implicit request =>
        Future(Ok(views.html.google.mcc.account.campaign.criterion.keyword.new_campaign_keyword(
          campaignKeywordForm,
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

  def createCampaignKeyword = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignKeywordForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(views.html.google.mcc.account.campaign.criterion.keyword.new_campaign_keyword(
            formWithErrors,
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
        },
        campaign_keyword => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.CAMPAIGN_KEYWORD,
              changeData = campaignKeywordFormToDbo(campaign_keyword)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.criterion.routes.CampaignKeywordController.campaignKeywords()))
        }
      )
  }


  def bulkNewCampaignKeyword = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request =>
      var error_list = new ListBuffer[String]()
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[CampaignKeywordForm]
          val keyword_data_list = Utilities.bulkImport(bulk, field_names)
          for (((keyword_data, action), index) <- keyword_data_list.zipWithIndex) {
            campaignKeywordForm.bind(keyword_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              campaign_keyword =>
                setPendingCache(
                  Left(request),
                  pendingCache(Left(request)) :+ PendingCacheStructure(
                    id = pendingCache(Left(request)).length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.GOOGLE,
                    changeCategory = ChangeCategory.CAMPAIGN_KEYWORD,
                    changeData = campaignKeywordFormToDbo(campaign_keyword)
                  )
                )
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.google.mcc.account.campaign.criterion.keyword.new_campaign_keyword(
          campaignKeywordForm,
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
        Future(Redirect(controllers.google.mcc.account.campaign.criterion.routes.CampaignKeywordController.campaignKeywords()))
      }
  }

  def editCampaignKeyword(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      googleCriterionCollection.findOne(DBObject("criterionApiId" -> api_id)) match {
        case None => Future(Redirect(controllers.routes.DashboardController.dashboard()))
        case Some(campaign_criterion_obj) =>
          val campaigns = googleCampaignCollection.find(
            DBObject(
              "mccObjId" -> campaign_criterion_obj.getAsOrElse[Option[String]]("mccObjId", None),
              "customerObjId" -> campaign_criterion_obj.getAsOrElse[Option[String]]("customerObjId", None)
            )).toList
          val campaign_keyword = dboToGoogleEntity[CampaignCriterion](campaign_criterion_obj, "criterion", None)
          Future(Ok(views.html.google.mcc.account.campaign.criterion.keyword.edit_campaign_keyword(
            api_id,
            campaignKeywordForm.fill(
              CampaignKeywordForm(
                parent = controllers.Google.CampaignCriterionParent(
                  mccObjId = campaign_criterion_obj.getAsOrElse[Option[String]]("mccObjId", None),
                  customerApiId = campaign_criterion_obj.getAsOrElse[Option[Long]]("customerApiId", None),
                  campaignApiId = campaign_criterion_obj.getAsOrElse[Option[Long]]("campaignApiId", None)
                ),
                apiId = Some(campaign_keyword.getCriterion.getId),
                isNegative = Some(campaign_keyword.getIsNegative),
                text = campaign_keyword.getCriterion.asInstanceOf[Keyword].getText,
                matchType = campaign_keyword.getCriterion.asInstanceOf[Keyword].getMatchType.toString,
                bidModifier = Some(campaign_keyword.getBidModifier)
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

  def saveCampaignKeyword(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignKeywordForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(views.html.google.mcc.account.campaign.criterion.keyword.edit_campaign_keyword(
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
        campaign_keyword => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.CAMPAIGN_KEYWORD,
              changeData = campaignKeywordFormToDbo(campaign_keyword)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.criterion.routes.CampaignKeywordController.campaignKeywords()))
        }
      )
  }

  def deleteCampaignKeyword(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
          id = pendingCache(Left(request)).length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.GOOGLE,
          changeCategory = ChangeCategory.CAMPAIGN_KEYWORD,
          changeData = DBObject("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.google.mcc.account.campaign.criterion.routes.CampaignKeywordController.campaignKeywords()))
  }
}
