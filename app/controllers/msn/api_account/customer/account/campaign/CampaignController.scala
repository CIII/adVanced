package controllers.msn.api_account.customer.account.campaign

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.microsoft.bingads.v11.campaignmanagement.Campaign
import com.mongodb.casbah.Imports._
import helpers.msn.api_account.customer.account.campaign.CampaignControllerHelper
import models.mongodb._
import models.mongodb.msn.Msn._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import security.HandlerKeys

import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class CampaignController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {
  import CampaignControllerHelper._

  def campaigns(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name=PermissionGroup.MSNRead.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.msn.api_account.customer.account_info.campaign.campaigns(
        msnCampaignCollection.find().skip(page * pageSize).limit(pageSize).toList.map(dboToMsnEntity[Campaign](_, "campaign", None)),
        page,
        pageSize,
        orderBy,
        filter,
        msnCampaignCollection.count(),
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.MSN && x.changeCategory == ChangeCategory.CAMPAIGN
          )
      )))
  }


  def newCampaign = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request => Future(Ok(views.html.msn.api_account.customer.account_info.campaign.new_campaign(campaignForm, List())))
  }


  def editCampaign(id: String) = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      msnCampaignCollection.findOne(DBObject("campaignApiId" -> id)) match {
        case Some(x) =>
          val campaign = dboToMsnEntity[Campaign](x, "campaign", None)
          Future(Ok(views.html.msn.api_account.customer.account_info.campaign.edit_campaign(
            id,
            campaignForm.fill(
              CampaignForm(
                name = campaign.getName,
                description = Some(campaign.getDescription),
                budgetType = Some(campaign.getBudgetType.toString),
                dailyBudget = Some(campaign.getDailyBudget),
                status = campaign.getStatus.toString,
                timeZone = campaign.getTimeZone
              )
            )
          )))
        case None =>
          Future(BadRequest("Not Found"))
      }
  }


  def createCampaign = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(views.html.msn.api_account.customer.account_info.campaign.new_campaign(formWithErrors, List()))),
        campaign => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.MSN,
              changeCategory = ChangeCategory.CAMPAIGN,
              changeData = campaignFormToDbo(campaign)
            )
          )
          Future(Redirect(controllers.msn.api_account.customer.account.campaign.routes.CampaignController.campaigns()))
        }
      )
  }

  def saveCampaign(id: String) = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      campaignForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.msn.api_account.customer.account_info.campaign.edit_campaign(
              id,
              formWithErrors
            )
          ))
        },
        campaign => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.MSN,
              changeCategory = ChangeCategory.CAMPAIGN,
              changeData = campaignFormToDbo(campaign)
            )
          )
          Future(Redirect(controllers.msn.api_account.customer.account.campaign.routes.CampaignController.campaigns()))
        }
      )
  }


  def deleteCampaign(id: String) = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
          id = pendingCache(Left(request)).length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.MSN,
          changeCategory = ChangeCategory.CAMPAIGN,
          changeData = DBObject("apiId" -> id)
        )
      )
      Future(Redirect(controllers.msn.api_account.customer.account.campaign.routes.CampaignController.campaigns()))
  }

  def bulkNewCampaign = deadbolt.Dynamic(name = PermissionGroup.MSNWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[com.microsoft.bingads.v11.campaignmanagement.Campaign]
          val campaign_data_list = Utilities.bulkImport(bulk, field_names)
          for (((campaign_data, action), index) <- campaign_data_list.zipWithIndex) {
            campaignForm.bind(campaign_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              campaign => {
                setPendingCache(
                  Left(request),
                  pendingCache(Left(request)) :+ PendingCacheStructure(
                    id = pendingCache(Left(request)).length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.MSN,
                    changeCategory = ChangeCategory.CAMPAIGN,
                    changeData = campaignFormToDbo(campaign)
                  )
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.msn.api_account.customer.account_info.campaign.new_campaign(
          campaignForm,
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.msn.api_account.customer.account.campaign.routes.CampaignController.campaigns()))
      }
    }
  }
}


