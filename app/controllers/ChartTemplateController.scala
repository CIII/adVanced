package controllers

import java.net.URLEncoder
import javax.inject.Inject

import Shared.Shared._
import play.api.libs.json.Json
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import be.objectify.deadbolt.scala.cache.HandlerCache
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import com.mongodb.client.model.ReplaceOptions
import models.mongodb.MongoExtensions._
import models.mongodb.{ChartTemplate, PermissionGroup}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import security.HandlerKeys

import scala.concurrent.Future

class ChartTemplateController @Inject()(
  val controllerComponents: ControllerComponents,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders
) extends BaseController with I18nSupport {
  val logger = Logger(this.getClass)

  def saveChartTemplate = deadbolt.Dynamic(name = PermissionGroup.LynxCharts.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      request.body.asJson.get.toString match {
        case templateJson =>
          val template = Json.parse(templateJson).as[ChartTemplate]
          val templateDbo = ChartTemplate.toDocument(template)
          ChartTemplate.chartTemplateCollection.replaceOne(
            Document("userName" -> template.userName,
              "templateName" -> template.templateName,
              "metaData.reloadUri" -> template.metaData.reloadUri
            ),
            templateDbo,
            new ReplaceOptions().upsert(true)
          )
          
          Future.successful(Ok)
        case _ =>
          Future.successful(BadRequest)
      }
  }
  
  def loadTemplate(templateName: String) = deadbolt.Dynamic(name = PermissionGroup.LynxCharts.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      request.getQueryString("reloadUri") match {
        case Some(reloadUri) =>
          ChartTemplate.chartTemplateCollection.findOne(
          Document("metaData.reloadUri" -> reloadUri,
            "templateName" -> templateName
          )) match {
            case Some(templateDbo: Document) =>
              val template: ChartTemplate = ChartTemplate.fromDocument(templateDbo)
              val useReportDates: Boolean = request.getQueryString("useReportDates") match {
                case Some(useReportDates) => useReportDates.toBoolean
                case _ => false
              }
              
              val finalMetaData = if(!useReportDates){
                template.metaData.copy(startDate = None, endDate = None)
              } else {
                template.metaData
              }
              Future.successful(Redirect(reloadUri + "?metaData=" + URLEncoder.encode(Json.toJson(finalMetaData).toString(), "UTF-8")))
            case _ =>
              logger.debug("Template name not found: " + templateName)
              Future.successful(BadRequest("Template name not found"))
          }
        
        case _ =>
          logger.debug("Invalid reloadUri")
          Future.successful(BadRequest("invalid reloadUri"))
      }
      
  }
  
  def getTemplateNames = deadbolt.Dynamic(name = PermissionGroup.LynxCharts.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      request.getQueryString("reloadUri") match {
        case Some(reloadUri) =>
          val names = ChartTemplate.chartTemplateCollection.find(Document("metaData.reloadUri" -> reloadUri)).toList.map {
            templateDbo => templateDbo.getString("templateName")
          }
          Future.successful(Ok(Json.toJson(names).toString()))
        case _ =>
          Future.successful(BadRequest("invalid reloadUri"))
      }  
  }
}