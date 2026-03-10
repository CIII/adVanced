package controllers.google.mcc.account.campaign.adgroup.criterion

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import helpers.google.mcc.account.campaign.adgroup.AdGroupControllerHelper
import helpers.google.mcc.account.campaign.adgroup.criterion.KeywordControllerHelper._
import models.mongodb._
import models.mongodb.MongoExtensions._
import models.mongodb.google.Google._
import play.api.i18n.I18nSupport
import play.api.mvc._
import security.HandlerKeys

import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class KeywordController @Inject()(val controllerComponents: ControllerComponents, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {
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
        googleCriterionCollection.namespace.getCollectionName,
        Some("criterionType" -> "adGroupCriterion")
      )))
  }

  def keywords(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[BiddableAdGroupCriterion] with Document-based access
      val criterionDocs = googleCriterionCollection.find(
        Document("criterionType" -> "adGroupCriterion")
      ).skip(page * pageSize).limit(pageSize).toList
      Future(Ok(views.html.google.mcc.account.campaign.adgroup.criterion.keyword.keywords(
        criterionDocs,
        page,
        pageSize,
        orderBy,
        filter,
        googleCriterionCollection.count(Document("criterionType" -> "adGroupCriterion")).toInt,
        pendingCache(Left(request))
          .filter(x =>
            x.trafficSource == TrafficSource.GOOGLE && x.changeCategory == ChangeCategory.KEYWORD
          )
      )))
  }

  def newKeyword = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[AdGroup] with Document-based access
      val adGroupDocs = googleAdGroupCollection.find(Document()).toList
      Future(Ok(views.html.google.mcc.account.campaign.adgroup.criterion.keyword.new_keyword(
        keywordForm,
        adGroupDocs,
        pendingCache(Left(request))
          .filter(x =>
            x.changeType == ChangeType.NEW
              && x.trafficSource == TrafficSource.GOOGLE
              && x.changeCategory == ChangeCategory.AD_GROUP
          )
          .map(x => AdGroupControllerHelper.documentToAdGroupForm(x.changeData)),
        List()
      )))
  }

  def createKeyword = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      keywordForm.bindFromRequest.fold(
        formWithErrors => {
          // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[AdGroup] with Document-based access
          val adGroupDocs = googleAdGroupCollection.find(Document()).toList
          Future(BadRequest(views.html.google.mcc.account.campaign.adgroup.criterion.keyword.new_keyword(
            formWithErrors,
            adGroupDocs,
            pendingCache(Left(request))
              .filter(x =>
                x.changeType == ChangeType.NEW
                  && x.trafficSource == TrafficSource.GOOGLE
                  && x.changeCategory == ChangeCategory.AD_GROUP
              )
              .map(x => AdGroupControllerHelper.documentToAdGroupForm(x.changeData)),
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
              changeData = adGroupKeywordFormToDocument(keyword)
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
                    changeData = adGroupKeywordFormToDocument(keyword)
                  )
                )
            )
          }
        }
      }
      if (error) {
        // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[AdGroup] with Document-based access
        val adGroupDocs = googleAdGroupCollection.find(Document()).toList
        Future(BadRequest(views.html.google.mcc.account.campaign.adgroup.criterion.keyword.new_keyword(
          keywordForm,
          adGroupDocs,
          pendingCache(Left(request))
            .filter(x =>
              x.changeType == ChangeType.NEW
                && x.trafficSource == TrafficSource.GOOGLE
                && x.changeCategory == ChangeCategory.AD_GROUP
            )
            .map(x => AdGroupControllerHelper.documentToAdGroupForm(x.changeData)),
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.google.mcc.account.campaign.adgroup.criterion.routes.KeywordController.keywords()))
      }
  }

  def editKeyword(id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      googleCriterionCollection.findOne(Document("criterionApiId" -> id)) match {
        case None => Future(Redirect(controllers.routes.DashboardController.dashboard))
        case Some(keyword) =>
          // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[AdGroupCriterion]/BiddableAdGroupCriterion/NegativeAdGroupCriterion with Document-based access
          val ad_groups = googleAdGroupCollection.find(Document()).toList
          val criterionDoc = Option(keyword.toBsonDocument.get("criterion")).map(v => Document(v.asDocument())).flatMap(d => Option(d.toBsonDocument.get("object")).map(v => Document(v.asDocument())))
          val innerCriterionDoc = criterionDoc.flatMap(d => Option(d.toBsonDocument.get("criterion")).map(v => Document(v.asDocument())))
          val criterionUse = criterionDoc.flatMap(d => Option(d.getString("criterionUse"))).getOrElse("BIDDABLE")
          val criterionId = innerCriterionDoc.flatMap(d => Option(d.getLong("id")).map(_.toLong))
          val text = innerCriterionDoc.flatMap(d => Option(d.getString("text"))).getOrElse("")
          val matchType = innerCriterionDoc.flatMap(d => Option(d.getString("matchType"))).getOrElse("BROAD")

          val keywordFormData = if (criterionUse == "BIDDABLE") {
            KeywordForm(
              controllers.Google.AdGroupCriterionParent(
                mccObjId = Option(keyword.getString("mccObjId")),
                customerApiId = Option(keyword.getLong("customerApiId")).map(_.toLong),
                campaignApiId = Option(keyword.getLong("campaignApiId")).map(_.toLong),
                adGroupApiId = Option(keyword.getLong("adGroupApiId")).map(_.toLong)
              ),
              apiId = criterionId,
              criterionUse = criterionUse,
              text = text,
              matchType = matchType,
              userStatus = criterionDoc.flatMap(d => Option(d.getString("userStatus"))),
              systemServingStatus = criterionDoc.flatMap(d => Option(d.getString("systemServingStatus"))),
              approvalStatus = criterionDoc.flatMap(d => Option(d.getString("approvalStatus"))),
              disapprovalReasons = criterionDoc.flatMap(d => Option(d.getString("disapprovalReasons")).map(_.split(",").toList)),
              destinationUrl = criterionDoc.flatMap(d => Option(d.getString("destinationUrl"))),
              finalUrl = criterionDoc.flatMap { d =>
                Option(d.toBsonDocument.get("finalUrls")).flatMap { finalUrlsBson =>
                  val finalUrlsDoc = Document(finalUrlsBson.asDocument())
                  Option(finalUrlsDoc.toBsonDocument.get("urls")).flatMap { urlsBson =>
                    import scala.jdk.CollectionConverters._
                    val urlsArr = urlsBson.asArray().asScala.toList
                    urlsArr.lift(1).map(_.asString().getValue)
                  }
                }
              },
              finalMobileUrl = criterionDoc.flatMap { d =>
                Option(d.toBsonDocument.get("finalMobileUrls")).flatMap { finalMobileUrlsBson =>
                  val finalMobileUrlsDoc = Document(finalMobileUrlsBson.asDocument())
                  Option(finalMobileUrlsDoc.toBsonDocument.get("urls")).flatMap { urlsBson =>
                    import scala.jdk.CollectionConverters._
                    val urlsArr = urlsBson.asArray().asScala.toList
                    urlsArr.lift(1).map(_.asString().getValue)
                  }
                }
              },
              customParameters = criterionDoc.flatMap { d =>
                Option(d.toBsonDocument.get("urlCustomParameters")).flatMap { customParamsBson =>
                  val customParamsDoc = Document(customParamsBson.asDocument())
                  Option(customParamsDoc.toBsonDocument.get("parameters")).map { paramsBson =>
                    import scala.jdk.CollectionConverters._
                    paramsBson.asArray().asScala.map { p =>
                      val paramDoc = Document(p.asDocument())
                      controllers.Google.CustomParameter(key = paramDoc.getString("key"), value = Option(paramDoc.getString("value")))
                    }.toList
                  }
                }
              },
              firstPageCpcAmount = criterionDoc.flatMap(d => Option(d.toBsonDocument.get("firstPageCpc")).map(v => Document(v.asDocument())).flatMap(fc => Option(fc.toBsonDocument.get("amount")).map(v => Document(v.asDocument()))).flatMap(a => Option(a.getLong("microAmount")).map(_.toLong))),
              topOfPageCpcAmount = criterionDoc.flatMap(d => Option(d.toBsonDocument.get("topOfPageCpc")).map(v => Document(v.asDocument())).flatMap(fc => Option(fc.toBsonDocument.get("amount")).map(v => Document(v.asDocument()))).flatMap(a => Option(a.getLong("microAmount")).map(_.toLong))),
              bidModifier = criterionDoc.flatMap(d => Option(d.getDouble("bidModifier")).map(_.toDouble))
            )
          } else {
            KeywordForm(
              controllers.Google.AdGroupCriterionParent(
                mccObjId = Option(keyword.getString("mccObjId")),
                customerApiId = Option(keyword.getLong("customerApiId")).map(_.toLong),
                campaignApiId = Option(keyword.getLong("campaignApiId")).map(_.toLong),
                adGroupApiId = Option(keyword.getLong("adGroupApiId")).map(_.toLong)
              ),
              apiId = criterionId,
              criterionUse = criterionUse,
              text = text,
              matchType = matchType,
              None, None, None, None, None, None, None, None, None, None, None
            )
          }

          Future(Ok(views.html.google.mcc.account.campaign.adgroup.criterion.keyword.edit_keyword(
            id,
            keywordForm.fill(keywordFormData),
            ad_groups,
            pendingCache(Left(request))
              .filter(x =>
                x.changeType == ChangeType.NEW
                  && x.trafficSource == TrafficSource.GOOGLE
                  && x.changeCategory == ChangeCategory.AD_GROUP
              )
              .map(x => AdGroupControllerHelper.documentToAdGroupForm(x.changeData))
          )))
      }
  }

  def saveKeyword(id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      keywordForm.bindFromRequest.fold(
        formWithErrors => {
          // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[AdGroup] with Document-based access
          val adGroupDocs = googleAdGroupCollection.find(Document()).toList
          Future(BadRequest(views.html.google.mcc.account.campaign.adgroup.criterion.keyword.edit_keyword(
            id,
            formWithErrors,
            adGroupDocs,
            pendingCache(Left(request))
              .filter(x =>
                x.changeType == ChangeType.NEW
                  && x.trafficSource == TrafficSource.GOOGLE
                  && x.changeCategory == ChangeCategory.AD_GROUP
              )
              .map(x => AdGroupControllerHelper.documentToAdGroupForm(x.changeData))
          )))
        },
        keyword => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.KEYWORD,
              changeData = adGroupKeywordFormToDocument(keyword)
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
          changeData = Document("apiId" -> id)
        )
      )
      Future(Redirect(controllers.google.mcc.account.campaign.adgroup.criterion.routes.KeywordController.keywords()))
  }
}
