package controllers

import java.net.URLEncoder
import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import be.objectify.deadbolt.scala.cache.HandlerCache
import com.mongodb.casbah.Imports._
import models.mongodb.{ChartTemplate, PermissionGroup}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import security.HandlerKeys

import scala.collection.JavaConverters._
import scala.concurrent.Future

class ChartTemplateController @Inject()(
  val messagesApi: MessagesApi,  
  deadbolt: DeadboltActions,  
  handlers: HandlerCache,  
  actionBuilder: ActionBuilders
) extends Controller with I18nSupport {
  
  def saveChartTemplate = deadbolt.Dynamic(name = PermissionGroup.LynxCharts.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      request.body.asJson.get.toString match {
        case templateJson =>
          val template = gson.fromJson(templateJson, classOf[ChartTemplate])
          val templateDbo = ChartTemplate.toDbo(template)
          ChartTemplate.chartTemplateCollection.update(
            DBObject("userName" -> template.userName,
              "templateName" -> template.templateName,
              "metaData.reloadUri" -> template.metaData.reloadUri
            ),
            templateDbo,
            true
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
          DBObject("metaData.reloadUri" -> reloadUri,
            "templateName" -> templateName
          )) match {
            case Some(templateDbo: DBObject) =>
              val template: ChartTemplate = ChartTemplate.fromDbo(templateDbo)
              val useReportDates: Boolean = request.getQueryString("useReportDates") match {
                case Some(useReportDates) => useReportDates.toBoolean
                case _ => false
              }
              
              if(!useReportDates){
                template.metaData.startDate = None
                template.metaData.endDate = None
              }
              Future.successful(Redirect(reloadUri + "?metaData=" + URLEncoder.encode(gson.toJson(template.metaData), "UTF-8")))
            case _ =>
              Logger.debug("Template name not found: " + templateName)
              Future.successful(BadRequest("Template name not found"))
          }
        
        case _ =>
          Logger.debug("Invalid reloadUri")
          Future.successful(BadRequest("invalid reloadUri"))
      }
      
  }
  
  def getTemplateNames = deadbolt.Dynamic(name = PermissionGroup.LynxCharts.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      request.getQueryString("reloadUri") match {
        case Some(reloadUri) =>
          val names = ChartTemplate.chartTemplateCollection.find(DBObject("metaData.reloadUri" -> reloadUri)).toList.map { 
            templateDbo => templateDbo.getAs[String]("templateName").get
          }.asJava
          Future.successful(Ok(gson.toJson(names)))  
        case _ =>
          Future.successful(BadRequest("invalid reloadUri"))
      }  
  }
}