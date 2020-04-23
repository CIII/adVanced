package controllers.google.mcc

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.mongodb.casbah.Imports._
import helpers.google.mcc.MccControllerHelper._
import models.mongodb._
import models.mongodb.google.Google._
import play.api.Play.current
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import security.HandlerKeys
import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import util.charts._
import util.charts.client.ChartColumn.ColumnDataType._
import util.charts.client.ChartColumn.ColumnType._
import util.charts.client._
import util.charts.client.ActionColumn
import util.charts.client.ChartColumn

class MccController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {
  def json = Action.async {
    implicit request =>
      val mcc = googleMccCollection.find(DBObject()).toList
      Future(Ok(com.mongodb.util.JSON.serialize(mcc.toString())))
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
          googleMccCollection.find().toList.map(google.Google.dboToMcc)
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
      googleMccCollection.findOne(DBObject("_id" -> new ObjectId(id))) match {
        case Some(mcc_obj) =>
          def mcc = google.Google.dboToMcc(mcc_obj)
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
              changeData = mccToDBObject(mcc)
            )
          )
          Future(Redirect(controllers.google.mcc.routes.MccController.mcc()))
        }
      )
  }


  def saveMcc(id: String) = deadbolt.Dynamic(name=PermissionGroup.GoogleWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Await.result(Shared.Shared.redisClient.lrange[PendingCacheStructure](pending_cache_key, 0, -1), 5 seconds).toList
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
          Shared.Shared.redisClient.lpush(
            pending_cache_key,
            (current_cache :+ PendingCacheStructure(
              id = current_cache.length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.MCC,
              changeData = com.mongodb.util.JSON.serialize(mcc).asInstanceOf[DBObject]
            )): _*
          )
          Future(Redirect(controllers.google.mcc.routes.MccController.mcc()))
        }
      )
  }


  def deleteMcc(id: String) = deadbolt.Dynamic(name=PermissionGroup.GoogleWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Await.result(Shared.Shared.redisClient.lrange[PendingCacheStructure](pending_cache_key, 0, -1), 5 seconds)
      Shared.Shared.redisClient.lpush(
        pending_cache_key,
        (current_cache :+ PendingCacheStructure(
          id = current_cache.length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.GOOGLE,
          changeCategory = ChangeCategory.MCC,
          changeData = DBObject("mccObjId" -> id)
        )): _*
      )
      Future(Redirect(controllers.google.mcc.routes.MccController.mcc()))
  }

  def bulkNewMcc = deadbolt.Dynamic(name=PermissionGroup.GoogleWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      val pending_cache_key = request.session.get(Security.username).get + cache_ext
      val current_cache = Await.result(Shared.Shared.redisClient.lrange[PendingCacheStructure](pending_cache_key, 0, -1), 5 seconds)
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
                Shared.Shared.redisClient.lpush(
                  pending_cache_key,
                  (current_cache :+ PendingCacheStructure(
                    id = current_cache.length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.GOOGLE,
                    changeCategory = ChangeCategory.MCC,
                    changeData = mccToDBObject(mcc)
                  )): _*
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
        Future(Redirect(controllers.google.mcc.routes.MccController.mcc()))
      }
    }
  }
}