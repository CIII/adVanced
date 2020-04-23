package controllers.facebook.business.ad_study

import play.api.mvc._
import javax.inject.Inject
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json._
import sync.facebook.business.ad_study.FacebookSplitTestActor
import models.mongodb.facebook.Facebook._
import scala.concurrent.Future
import org.bson.types.ObjectId
import Shared.Shared._
import sync.shared.Facebook._
import sync.facebook.business.ad_study._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.mongodb.casbah.Imports.{ObjectId, _}
import models.mongodb._
import models.mongodb.facebook.Facebook._
import security.HandlerKeys
import play.api.libs.ws._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.i18n.MessagesApi
import play.api.i18n.I18nSupport
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import helpers.CommonMappings
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.Mapping


class FacebookSplitTestController @Inject() (
  val messagesApi: MessagesApi,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders
) extends Controller with I18nSupport {
  import FacebookSplitTestController._
  
  def splitTests(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name=PermissionGroup.FacebookRead.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.facebook.business.ad_study.split_test(
        facebookSplitTestCollection.find().skip(page * pageSize).limit(pageSize).toList.map(FacebookSplitTest.fromDBO),
        page,
        pageSize,
        orderBy,
        filter,
        facebookSplitTestCollection.count(),
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.FACEBOOK && x.changeCategory == ChangeCategory.AD_STUDY
          )
      )))
  }
  
  def newSplitTest = deadbolt.Dynamic(name=PermissionGroup.FacebookWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.facebook.business.ad_study.new_split_test(
          fbSplitTestForm.fill(
            FacebookSplitTest(
              Some(new ObjectId),
              None,
              None,
              "",
              "",
              DateTime.now,
              DateTime.now.plusDays(30),
              "",
              List()
            )
          )
      )))
  }
  
  def createSplitTest= deadbolt.Dynamic(name=PermissionGroup.FacebookWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      fbSplitTestForm.bindFromRequest.fold(
        formWithErrors => {
          Logger.info("Error saving split test" + formWithErrors.toString)
          Future(BadRequest(views.html.facebook.business.ad_study.new_split_test(formWithErrors)))
        },  
        splitTest => { 
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
                id = pendingCache(Left(request)).length + 1,
                changeType = ChangeType.NEW,
                trafficSource = TrafficSource.FACEBOOK,
                changeCategory = ChangeCategory.AD_STUDY,
                changeData = FacebookSplitTest.toDBO(splitTest))
          )
          
          Future(Redirect(controllers.facebook.business.ad_study.routes.FacebookSplitTestController.splitTests(0, 10, 2, "")))
        }     
      )
  }
  
  def editSplitTest(objectIdStr: String) = deadbolt.Dynamic(name=PermissionGroup.FacebookWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.facebook.business.ad_study.edit_split_test(
          fbSplitTestForm.fill(FacebookSplitTest.findById(new ObjectId(objectIdStr)).get)
      )))
  }
  
  def saveSplitTest(objectIdStr: String) = deadbolt.Dynamic(name=PermissionGroup.FacebookWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      fbSplitTestForm.bindFromRequest.fold(
        formWithErrors => {
          Logger.info("Error saving split test" + formWithErrors.toString)
          Future(BadRequest(views.html.facebook.business.ad_study.edit_split_test(formWithErrors)))
        },  
        splitTest => { 
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
                id = pendingCache(Left(request)).length + 1,
                changeType = ChangeType.NEW,
                trafficSource = TrafficSource.FACEBOOK,
                changeCategory = ChangeCategory.AD_STUDY,
                changeData = FacebookSplitTest.toDBO(splitTest))
          )
          
          Future(Redirect(controllers.facebook.business.ad_study.routes.FacebookSplitTestController.splitTests(0, 10, 2, "")))
        }
      )
  }
  
  def deleteSplitTest(objectIdStr: String) = deadbolt.Dynamic(name=PermissionGroup.FacebookWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      var splitTest = FacebookSplitTest.findById(new ObjectId(objectIdStr)).get
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
            id = pendingCache(Left(request)).length + 1,
            changeType = ChangeType.DELETE,
            trafficSource = TrafficSource.FACEBOOK,
            changeCategory = ChangeCategory.AD_STUDY,
            changeData = FacebookSplitTest.toDBO(splitTest))
      )
            
      Future(Redirect(controllers.facebook.business.ad_study.routes.FacebookSplitTestController.splitTests(0, 10, 2, "")))
  }
}

object FacebookSplitTestController extends CommonMappings{
  
  def fbSplitTestForm: Form[FacebookSplitTest] = Form(
    mapping(
      "_id" -> optional(objectIdMapping),
      "bizObjectId" -> optional(fbBizAccountNumberMapping),
      "adStudyId" -> optional(text),
      "name" -> nonEmptyText,
      "description" -> nonEmptyText,
      "startTime" -> dateTimeMapping.verifying("Start time must be today or later" , 
          startTime => startTime.isAfter(DateTime.now().minusDays(1))),
      "endTime" -> dateTimeMapping.verifying("End time must be after start time",
          endTime => endTime.isAfter(DateTime.now().minusDays(1))),
      "testType" -> text,
      "cells" -> list(fbSplitTestCellCreationForm.mapping).verifying("Total treatment percentage across all cells must be 100",
          cells => cells.map(cell => cell.treatmentPercentage).sum == 100 )
    )
    (FacebookSplitTest.apply)
    (FacebookSplitTest.unapply)
  )
  
  def fbSplitTestCellCreationForm: Form[FacebookSplitTestCell] = Form(
     mapping(
       "cellId" -> optional(text),
       "name" -> text,
       "treatmentPercentage" -> default(number, 50),
       "entityIds" -> commaSeparatedListMapping
     )
     (FacebookSplitTestCell.apply)
     (FacebookSplitTestCell.unapply)
  )
  
  def fbBizAccountNumberMapping: Mapping[ObjectId] = (
    mapping(
      "accountNumber" -> nonEmptyText.verifying("Invalid Business Account Number", 
          accountNumber => (!FacebookBusinessAccount.findByAccountNumber(accountNumber).isEmpty))
    )
    ((accountNumber: String) => FacebookBusinessAccount.findByAccountNumber(accountNumber).get._id.get)
    ((accountId: ObjectId) => Some(FacebookBusinessAccount.findById(accountId).get.accountNumber))
  )
}