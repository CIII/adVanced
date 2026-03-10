package controllers.google.mcc.account

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import helpers.google.mcc.account.BudgetControllerHelper._
import models.mongodb._
import models.mongodb.MongoExtensions._
import models.mongodb.google.Google._
import play.api.i18n.I18nSupport
import play.api.mvc._
import security.HandlerKeys

import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class BudgetController @Inject()(
  val controllerComponents: ControllerComponents,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders
)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {
  def json = Action.async {
    implicit request =>
      Future(Ok(controllers.json(
        request,
        List(
          "mccObjId",
          "mccApiId",
          "customerObjId",
          "customerApiId",
          "tsecs"
        ),
        "budget",
        googleBudgetCollection.namespace.getCollectionName
      )))
  }

  def budgets(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      // TODO: Migrate to Google Ads API v18 - documentToGoogleEntity[Budget] replaced with Document-based access
      val budgetDocs = googleBudgetCollection.find().skip(page * pageSize).limit(pageSize).toList
      Future(Ok(views.html.google.mcc.account.budget.budgets(
        budgetDocs,
        page,
        pageSize,
        orderBy,
        filter,
        googleBudgetCollection.countSync().toInt,
        pendingCache(Left(request))
          .filter(x => x.trafficSource == TrafficSource.GOOGLE && x.changeCategory == ChangeCategory.BUDGET)
      )))
  }

  def newBudget = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.budget.new_budget(
        budgetForm,
        List()
      )))
  }

  def editBudget(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      googleBudgetCollection.findOne(Document("apiId" -> api_id)) match {
        case Some(budgetObj) =>
          // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Budget] with Document-based access
          val budgetDoc = Option(budgetObj.toBsonDocument.get("budget")).map(v => Document(v.asDocument())).flatMap(d => Option(d.toBsonDocument.get("object")).map(v => Document(v.asDocument())))
          Future(Ok(views.html.google.mcc.account.budget.edit_budget(
            api_id,
            budgetForm.fill(
              BudgetForm(
                apiId = budgetDoc.flatMap(d => Option(d.getLong("budgetId"))).map(_.toLong),
                name = budgetDoc.map(_.getString("name")).getOrElse(""),
                amount = budgetDoc.flatMap(d => Option(d.toBsonDocument.get("amount")).map(v => Document(v.asDocument())).flatMap(a => Option(a.getLong("microAmount")).map(_.toLong))),
                deliveryMethod = budgetDoc.map(d => Option(d.getString("deliveryMethod")).getOrElse("STANDARD")),
                isExplicitlyShared = budgetDoc.flatMap(d => Option(d.getBoolean("isExplicitlyShared")).map(_.booleanValue())),
                status = budgetDoc.map(d => Option(d.getString("status")).getOrElse("ENABLED"))
              )
            )
          )))
        case None =>
          Future(Redirect(controllers.google.mcc.account.routes.BudgetController.budgets()))
      }
  }

  def createBudget = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      budgetForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.google.mcc.account.budget.new_budget(
              formWithErrors,
              List()
            )
          ))
        },
        budget => {
          // TODO: Migrate to RedisService injection - redisClient.lpush removed
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.BUDGET,
              changeData = budgetFormToDocument(budget)
            )
          )
          Future(Redirect(controllers.google.mcc.account.routes.BudgetController.budgets()))
        }
      )
  }

  def bulkNewBudget = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      request.body.file("bulk").foreach {
        bulk => {
          // TODO: Migrate to Google Ads API v18 - replaced Utilities.getCaseClassParameter[Budget] with BudgetForm
          val field_names = Utilities.getCaseClassParameter[BudgetForm]
          val budget_data_list = Utilities.bulkImport(bulk, field_names)
          for (((budget_data, action), index) <- budget_data_list.zipWithIndex) {
            budgetForm.bind(budget_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              budget => {
                setPendingCache(
                  Left(request),
                  pendingCache(Left(request)) :+ PendingCacheStructure(
                    id = pendingCache(Left(request)).length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.GOOGLE,
                    changeCategory = ChangeCategory.BUDGET,
                    changeData = budgetFormToDocument(budget)
                  )
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.google.mcc.account.budget.new_budget(
          budgetForm,
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.google.mcc.account.routes.BudgetController.budgets()))
      }
    }
  }


  def saveBudget(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      budgetForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.google.mcc.account.budget.edit_budget(
              api_id,
              formWithErrors
            )
          ))
        },
        budget => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.BUDGET,
              changeData = budgetFormToDocument(budget)
            )
          )
          Future(Redirect(controllers.google.mcc.account.routes.BudgetController.budgets()))
        }
      )
  }


  def deleteBudget(api_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
          id = pendingCache(Left(request)).length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.GOOGLE,
          changeCategory = ChangeCategory.BUDGET,
          changeData = Document("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.google.mcc.account.routes.BudgetController.budgets()))
  }

  def amount(budget_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      request.getQueryString("dollarAmt") match {
        case Some(amtStr) =>
          val dollarAmt = amtStr.toDouble
          googleBudgetCollection.findOne(Document("budget.object.budgetId" -> budget_id)) match {
            case Some(budgetObj) =>
              // TODO: Migrate to Google Ads API v18 - replaced documentToGoogleEntity[Budget] with Document-based access
              val budgetDoc = Option(budgetObj.toBsonDocument.get("budget")).map(v => Document(v.asDocument())).flatMap(d => Option(d.toBsonDocument.get("object")).map(v => Document(v.asDocument())))
              setPendingCache(
                Left(request),
                pendingCache(Left(request)) :+ PendingCacheStructure(
                  id = pendingCache(Left(request)).length + 1,
                  changeType = ChangeType.UPDATE,
                  trafficSource = TrafficSource.GOOGLE,
                  changeCategory = ChangeCategory.BUDGET,
                  changeData = budgetFormToDocument(
                    BudgetForm(
                      apiId = Some(budget_id),
                      name = budgetDoc.map(_.getString("name")).getOrElse(""),
                      amount = Some(dollarsToMicro(dollarAmt)),
                      deliveryMethod = budgetDoc.map(d => Option(d.getString("deliveryMethod")).getOrElse("STANDARD")),
                      isExplicitlyShared = budgetDoc.flatMap(d => Option(d.getBoolean("isExplicitlyShared")).map(_.booleanValue())),
                      status = budgetDoc.map(d => Option(d.getString("status")).getOrElse("ENABLED"))
                    )
                  )
                )
              )

              Future(Ok(s"Successfully created update task for budget $budget_id"))

            case _ => Future(BadRequest("No budget found for specified Id"))
          }
        case _ => Future(BadRequest("dollarAmt is a required field"))
      }
  }
}
