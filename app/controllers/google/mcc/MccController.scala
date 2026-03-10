package controllers.google.mcc

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import helpers.google.mcc.MccControllerHelper._
import models.mongodb._
import models.mongodb.MongoExtensions._
import models.mongodb.google.Google._
import play.api.i18n.I18nSupport
import play.api.mvc._
import security.HandlerKeys
import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import util.charts._
import util.charts.client.ChartColumn.ColumnDataType._
import util.charts.client.ChartColumn.ColumnType._
import util.charts.client._
import util.charts.client.ActionColumn
import util.charts.client.ChartColumn

class MccController @Inject()(val controllerComponents: ControllerComponents, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {
  def json = Action.async {
    implicit request =>
      val mcc = googleMccCollection.find(Document()).toList
      Future(Ok(mcc.map(_.toJson()).mkString("[", ",", "]")))
  }

  def mcc = deadbolt.Dynamic(name=PermissionGroup.GoogleRead.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.mcc(
        new ClientChart(
          List(
            new ChartColumn("_id", "", "Object Id", string, dimension),
            new ChartColumn("name", "", "Name", string, dimension),
            new ChartColumn("developerToken", "", "Developer Token", string, dimension),
            new ChartColumn("oAuthClientId", "", "Client Id", string, dimension),
            new ChartColumn("oAuthClientSecret", "", "Client Secret", string, dimension),
            new ChartColumn("oAuthRefreshToken", "", "Refresh Token", string, dimension),
            new ActionColumn((rowValues: List[Any]) => "/google/mcc/%s/".format(rowValues(0).toString))  // rowValues(0) should correspond to _id
          ),
          googleMccCollection.find().toList.map(google.Google.documentToMcc)
        ),
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.GOOGLE && x.changeCategory == ChangeCategory.MCC
          )
      )))
  }


  def newMcc = deadbolt.Dynamic(name=PermissionGroup.GoogleWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit authRequest => Future(Ok(views.html.google.mcc.new_mcc(mccForm, List())))
  }


  def editMcc(id: String) = deadbolt.Dynamic(name=PermissionGroup.GoogleWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      googleMccCollection.findOne(Document("_id" -> new org.bson.types.ObjectId(id))) match {
        case Some(mcc_obj) =>
          def mcc = google.Google.documentToMcc(mcc_obj)
          Future(Ok(views.html.google.mcc.edit_mcc(
            id,
            mccForm.fill(
              Mcc(
                _id = mcc._id,
                name = mcc.name,
                developerToken = mcc.developerToken,
                oAuthClientId = mcc.oAuthClientId,
                oAuthClientSecret = mcc.oAuthClientSecret,
                oAuthRefreshToken = mcc.oAuthRefreshToken
              )
            )
          )))
        case None =>
          Future(BadRequest("Not Found"))
      }
  }


  def createMcc = deadbolt.Dynamic(name=PermissionGroup.GoogleWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      mccForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(views.html.google.mcc.new_mcc(formWithErrors, List()))),
        mcc => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.MCC,
              changeData = mccToDocument(mcc)
            )
          )
          Future(Redirect(controllers.google.mcc.routes.MccController.mcc))
        }
      )
  }


  def saveMcc(id: String) = deadbolt.Dynamic(name=PermissionGroup.GoogleWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      mccForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.google.mcc.edit_mcc(
              id,
              formWithErrors
            )
          ))
        },
        mcc => {
          // TODO: Migrate to RedisService injection - redisClient.lpush removed
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.MCC,
              changeData = mccToDocument(mcc)
            )
          )
          Future(Redirect(controllers.google.mcc.routes.MccController.mcc))
        }
      )
  }


  def deleteMcc(id: String) = deadbolt.Dynamic(name=PermissionGroup.GoogleWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      // TODO: Migrate to RedisService injection - redisClient.lpush removed
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
          id = pendingCache(Left(request)).length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.GOOGLE,
          changeCategory = ChangeCategory.MCC,
          changeData = Document("mccObjId" -> id)
        )
      )
      Future(Redirect(controllers.google.mcc.routes.MccController.mcc))
  }

  def bulkNewMcc = deadbolt.Dynamic(name=PermissionGroup.GoogleWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[Mcc]
          val mcc_data_list = Utilities.bulkImport(bulk, field_names)
          for (((mcc_data, action), index) <- mcc_data_list.zipWithIndex) {
            mccForm.bind(mcc_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              mcc => {
                // TODO: Migrate to RedisService injection - redisClient.lpush removed
                setPendingCache(
                  Left(request),
                  pendingCache(Left(request)) :+ PendingCacheStructure(
                    id = pendingCache(Left(request)).length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.GOOGLE,
                    changeCategory = ChangeCategory.MCC,
                    changeData = mccToDocument(mcc)
                  )
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.google.mcc.new_mcc(
          mccForm,
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.google.mcc.routes.MccController.mcc))
      }
    }
  }
}