package controllers.msn.api_account.customer.account.campaign.adgroup

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.microsoft.bingads.v11.campaignmanagement.AdGroup
import com.mongodb.casbah.Imports._
import helpers.msn.api_account.customer.account.campaign.adgroup.AdGroupControllerHelper
import models.mongodb._
import models.mongodb.msn.Msn._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import security.HandlerKeys

import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AdGroupController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {
  import AdGroupControllerHelper._

  def adgroups(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name=PermissionGroup.MSNRead.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.msn.api_account.customer.account_info.campaign.adgroup.adgroups(
        msnAdGroupCollection.find().skip(page * pageSize).limit(pageSize).toList.map(dboToMsnEntity[AdGroup](_, "adGroup", None)),
        page,
        pageSize,
        orderBy,
        filter,
        msnAdGroupCollection.count(),
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.MSN && x.changeCategory == ChangeCategory.AD_GROUP
          )
      )))
  }


  def newAdGroup = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request => Future(Ok(views.html.msn.api_account.customer.account_info.campaign.adgroup.new_adgroup(adGroupForm, List())))
  }


  def getBid(bid: com.microsoft.bingads.v11.campaignmanagement.Bid): Option[Double] = bid match {
    case x =>
      Some(x.getAmount)
    case _ =>
      None
  }

  def editAdGroup(id: String) = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      msnAdGroupCollection.findOne(DBObject("adGroupApiId" -> id)) match {
        case Some(x) =>
          val adgroup = dboToMsnEntity[AdGroup](x, "adGroup", None)
          Future(Ok(views.html.msn.api_account.customer.account_info.campaign.adgroup.edit_adgroup(
            id,
            adGroupForm.fill(
              AdGroupForm(
                apiId = Some(adgroup.getId),
                name = adgroup.getName,
                startDate = Some(
                  new org.joda.time.DateTime(
                    adgroup.getStartDate.getYear,
                    adgroup.getStartDate.getMonth,
                    adgroup.getStartDate.getDay,
                    0,
                    0
                  )
                ),
                endDate = Some(
                  new org.joda.time.DateTime(
                    adgroup.getEndDate.getYear,
                    adgroup.getEndDate.getMonth,
                    adgroup.getEndDate.getDay,
                    0,
                    0
                  )
                ),
                adDistribution = adgroup.getAdDistribution.toArray.toList.asInstanceOf[List[String]],
                adRotationType = adgroup.getAdRotation.toString,
                biddingScheme = adgroup.getBiddingScheme.toString,
                contentMatchBid = getBid(adgroup.getContentMatchBid),
                network = Some(adgroup.getNetwork.toString),
                pricingModel = Some(adgroup.getPricingModel.toString),
                language = adgroup.getLanguage,
                status = adgroup.getStatus.toString
              )
            )
          )))
        case None =>
          Future(BadRequest("Not Found"))
      }
  }


  def createAdGroup = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      adGroupForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(views.html.msn.api_account.customer.account_info.campaign.adgroup.new_adgroup(formWithErrors, List()))),
        adgroup => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.MSN,
              changeCategory = ChangeCategory.AD_GROUP,
              changeData = adGroupFormToDbo(adgroup)
            )
          )
          Future(Redirect(controllers.msn.api_account.customer.account.campaign.routes.CampaignController.campaigns()))
        }
      )
  }


  def saveAdGroup(id: String) = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      adGroupForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.msn.api_account.customer.account_info.campaign.adgroup.edit_adgroup(
              id,
              formWithErrors
            )
          ))
        },
        adGroup => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.MSN,
              changeCategory = ChangeCategory.AD_GROUP,
              changeData = adGroupFormToDbo(adGroup)
            )
          )
          Future(Redirect(controllers.msn.api_account.customer.account.campaign.adgroup.routes.AdGroupController.adgroups()))
        }
      )
  }


  def deleteAdGroup(id: String) = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
          id = pendingCache(Left(request)).length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.MSN,
          changeCategory = ChangeCategory.AD_GROUP,
          changeData = DBObject("apiId" -> id)
        )
      )
      Future(Redirect(controllers.msn.api_account.customer.account.campaign.adgroup.routes.AdGroupController.adgroups()))
  }


  def bulkNewAdGroup = deadbolt.Dynamic(name = PermissionGroup.MSNWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[com.microsoft.bingads.v11.campaignmanagement.Campaign]
          val campaign_data_list = Utilities.bulkImport(bulk, field_names)
          for (((campaign_data, action), index) <- campaign_data_list.zipWithIndex) {
            adGroupForm.bind(campaign_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              adgroup => {
                setPendingCache(
                  Left(request),
                  pendingCache(Left(request)) :+ PendingCacheStructure(
                    id = pendingCache(Left(request)).length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.MSN,
                    changeCategory = ChangeCategory.AD_GROUP,
                    changeData = adGroupFormToDbo(adgroup)
                  )
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.msn.api_account.customer.account_info.campaign.adgroup.new_adgroup(
          adGroupForm,
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.msn.api_account.customer.account.campaign.adgroup.routes.AdGroupController.adgroups()))
      }
    }
  }
}


