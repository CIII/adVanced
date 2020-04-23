package controllers.google.mcc.account.campaign.adgroup.criterion

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.google.api.ads.adwords.axis.v201609.cm._
import com.mongodb.casbah.Imports._
import helpers.google.mcc.account.campaign.adgroup.AdGroupControllerHelper
import helpers.google.mcc.account.campaign.adgroup.criterion.KeywordControllerHelper._
import models.mongodb._
import models.mongodb.google.Google._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import security.HandlerKeys

import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class KeywordController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {
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
          "adGroupObjId",
          "adGroupApiId",
          "criterionObjId",
          "criterionApiId"
        ),
        "criterion",
        googleCriterionCollection,
        Some("criterionType" -> "adGroupCriterion")
      )))
  }

  def keywords(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.campaign.adgroup.criterion.keyword.keywords(
        googleCriterionCollection.find(
          DBObject("criterionType" -> "adGroupCriterion")
        ).skip(page * pageSize).limit(pageSize).toList.map(dboToGoogleEntity[BiddableAdGroupCriterion](_, "criterion", None)),
        page,
        pageSize,
        orderBy,
        filter,
        googleCriterionCollection.count(DBObject("criterionType" -> "adGroupCriterion")),
        pendingCache(Left(request))
          .filter(x =>
            x.trafficSource == TrafficSource.GOOGLE && x.changeCategory == ChangeCategory.KEYWORD
          )
      )))
  }

  def newKeyword = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.campaign.adgroup.criterion.keyword.new_keyword(
        keywordForm,
        googleAdGroupCollection.find(DBObject()).toList.map(dboToGoogleEntity[AdGroup](_, "adGroup", None)),
        pendingCache(Left(request))
          .filter(x =>
            x.changeType == ChangeType.NEW
              && x.trafficSource == TrafficSource.GOOGLE
              && x.changeCategory == ChangeCategory.AD_GROUP
          )
          .map(x => AdGroupControllerHelper.dboToAdGroupForm(x.changeData.asDBObject)),
        List()
      )))
  }

  def createKeyword = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      keywordForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(views.html.google.mcc.account.campaign.adgroup.criterion.keyword.new_keyword(
            formWithErrors,
            googleAdGroupCollection.find(DBObject()).toList.map(dboToGoogleEntity[AdGroup](_, "adGroup", None)),
            pendingCache(Left(request))
              .filter(x =>
                x.changeType == ChangeType.NEW
                  && x.trafficSource == TrafficSource.GOOGLE
                  && x.changeCategory == ChangeCategory.AD_GROUP
              )
              .map(x => AdGroupControllerHelper.dboToAdGroupForm(x.changeData.asDBObject)),
            List()
          )))
        },
        keyword => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.KEYWORD,
              changeData = adGroupKeywordFormToDbo(keyword)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.adgroup.criterion.routes.KeywordController.keywords()))
        }
      )
  }

  def bulkNewKeyword = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request =>
      var error = false
      var error_list = new ListBuffer[String]()
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[KeywordForm]
          val keyword_data_list = Utilities.bulkImport(bulk, field_names)
          for (((keyword_data, action), index) <- keyword_data_list.zipWithIndex) {
            //todo: This needs to make accomodations for the custom parameter field.
            keywordForm.bind(keyword_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error = true
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              keyword =>
                setPendingCache(
                  Left(request),
                  pendingCache(Left(request)) :+ PendingCacheStructure(
                    id = pendingCache(Left(request)).length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.GOOGLE,
                    changeCategory = ChangeCategory.KEYWORD,
                    changeData = adGroupKeywordFormToDbo(keyword)
                  )
                )
            )
          }
        }
      }
      if (error) {
        Future(BadRequest(views.html.google.mcc.account.campaign.adgroup.criterion.keyword.new_keyword(
          keywordForm,
          googleAdGroupCollection.find(DBObject()).toList.map(dboToGoogleEntity[AdGroup](_, "adGroup", None)),
          pendingCache(Left(request))
            .filter(x =>
              x.changeType == ChangeType.NEW
                && x.trafficSource == TrafficSource.GOOGLE
                && x.changeCategory == ChangeCategory.AD_GROUP
            )
            .map(x => AdGroupControllerHelper.dboToAdGroupForm(x.changeData.asDBObject)),
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.google.mcc.account.campaign.adgroup.criterion.routes.KeywordController.keywords()))
      }
  }

  def editKeyword(id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      googleCriterionCollection.findOne(DBObject("criterionApiId" -> id)) match {
        case None => Future(Redirect(controllers.routes.DashboardController.dashboard()))
        case Some(keyword) =>
          val ad_groups = googleAdGroupCollection.find(DBObject()).toList
          val adGroupCriterion = dboToGoogleEntity[AdGroupCriterion](keyword, "criterion", None)
          adGroupCriterion match {
            case b: BiddableAdGroupCriterion =>
              Future(Ok(views.html.google.mcc.account.campaign.adgroup.criterion.keyword.edit_keyword(
                id,
                keywordForm.fill(KeywordForm(
                  controllers.Google.AdGroupCriterionParent(
                    mccObjId = keyword.getAsOrElse[Option[String]]("mccObjId", None),
                    customerApiId = keyword.getAsOrElse[Option[Long]]("customerApiId", None),
                    campaignApiId = keyword.getAsOrElse[Option[Long]]("campaignApiId", None),
                    adGroupApiId = keyword.getAsOrElse[Option[Long]]("adGroupApiId", None)
                  ),
                  apiId = Some(adGroupCriterion.getCriterion.getId),
                  criterionUse = adGroupCriterion.getCriterionUse.toString,
                  text = adGroupCriterion.getCriterion.asInstanceOf[Keyword].getText,
                  matchType = adGroupCriterion.getCriterion.asInstanceOf[Keyword].getMatchType.toString,
                  userStatus = Some(b.getUserStatus.toString),
                  systemServingStatus = Some(b.getSystemServingStatus.toString),
                  approvalStatus = Some(b.getApprovalStatus.toString),
                  disapprovalReasons = Some(b.getDisapprovalReasons.toList),
                  destinationUrl = Some(b.getDestinationUrl),
                  finalUrl = if (b.getFinalUrls.getUrls.nonEmpty) Some(b.getFinalUrls.getUrls(1)) else None,
                  finalMobileUrl = if (b.getFinalMobileUrls.getUrls.nonEmpty) Some(b.getFinalMobileUrls.getUrls(1)) else None,
                  customParameters = Some(b.getUrlCustomParameters.getParameters.map(p =>
                    controllers.Google.CustomParameter(key = p.getKey, value = Some(p.getValue))
                  ).toList),
                  firstPageCpcAmount = Some(b.getFirstPageCpc.getAmount.getMicroAmount),
                  topOfPageCpcAmount = Some(b.getTopOfPageCpc.getAmount.getMicroAmount),
                  bidModifier = Some(b.getBidModifier)
                )),
                ad_groups.map(dboToGoogleEntity[AdGroup](_, "adGroup", None)),
                pendingCache(Left(request))
                  .filter(x =>
                    x.changeType == ChangeType.NEW
                      && x.trafficSource == TrafficSource.GOOGLE
                      && x.changeCategory == ChangeCategory.AD_GROUP
                  )
                  .map(x => AdGroupControllerHelper.dboToAdGroupForm(x.changeData.asDBObject))
              )))
            case c: NegativeAdGroupCriterion =>
              Future(Ok(views.html.google.mcc.account.campaign.adgroup.criterion.keyword.edit_keyword(
                id,
                keywordForm.fill(KeywordForm(
                  controllers.Google.AdGroupCriterionParent(
                    mccObjId = keyword.getAsOrElse[Option[String]]("mccObjId", None),
                    customerApiId = keyword.getAsOrElse[Option[Long]]("customerApiId", None),
                    campaignApiId = keyword.getAsOrElse[Option[Long]]("campaignApiId", None),
                    adGroupApiId = keyword.getAsOrElse[Option[Long]]("adGroupApiId", None)
                  ),
                  apiId = Some(adGroupCriterion.getCriterion.getId),
                  criterionUse = adGroupCriterion.getCriterionUse.toString,
                  text = adGroupCriterion.getCriterion.asInstanceOf[Keyword].getText,
                  matchType = adGroupCriterion.getCriterion.asInstanceOf[Keyword].getMatchType.toString,
                  None, None, None, None, None, None, None, None, None, None, None
                )),
                ad_groups.map(dboToGoogleEntity[AdGroup](_, "adGroup", None)),
                pendingCache(Left(request))
                  .filter(x =>
                    x.changeType == ChangeType.NEW
                      && x.trafficSource == TrafficSource.GOOGLE
                      && x.changeCategory == ChangeCategory.AD_GROUP
                  )
                  .map(x => AdGroupControllerHelper.dboToAdGroupForm(x.changeData.asDBObject))
              )))
          }
      }
  }

  def saveKeyword(id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      keywordForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(views.html.google.mcc.account.campaign.adgroup.criterion.keyword.edit_keyword(
          id,
          formWithErrors,
          googleAdGroupCollection.find(DBObject()).toList.map(dboToGoogleEntity[AdGroup](_, "adGroup", None)),
          pendingCache(Left(request))
            .filter(x =>
              x.changeType == ChangeType.NEW
                && x.trafficSource == TrafficSource.GOOGLE
                && x.changeCategory == ChangeCategory.AD_GROUP
            )
            .map(x => AdGroupControllerHelper.dboToAdGroupForm(x.changeData.asDBObject))
        ))),
        keyword => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.KEYWORD,
              changeData = adGroupKeywordFormToDbo(keyword)
            )
          )
          Future(Redirect(controllers.google.mcc.account.campaign.adgroup.criterion.routes.KeywordController.keywords()))
        }
      )
  }

  def deleteKeyword(id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
          id = pendingCache(Left(request)).length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.GOOGLE,
          changeCategory = ChangeCategory.KEYWORD,
          changeData = DBObject("apiId" -> id)
        )
      )
      Future(Redirect(controllers.google.mcc.account.campaign.adgroup.criterion.routes.KeywordController.keywords()))
  }
}

