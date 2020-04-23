package controllers.google.mcc.account

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.google.api.ads.adwords.axis.v201609.cm.Budget
import com.mongodb.casbah.Imports._
import helpers.google.mcc.account.BudgetControllerHelper._
import models.mongodb._
import models.mongodb.google.Google._
import play.api.cache.CacheApi
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import security.HandlerKeys

import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class BudgetController @Inject()(
  val messagesApi: MessagesApi,
  deadbolt: DeadboltActions,
  handlers: HandlerCache,
  actionBuilder: ActionBuilders,
  cache: CacheApi
) extends Controller with I18nSupport {
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
        googleBudgetCollection
      )))
  }

  def budgets(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name = PermissionGroup.GoogleRead.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.google.mcc.account.budget.budgets(
        googleBudgetCollection.find().skip(page * pageSize).limit(pageSize).toList.map(dboToGoogleEntity[Budget](_, "budget", None)),
        page,
        pageSize,
        orderBy,
        filter,
        googleBudgetCollection.count(),
        cache.get(pendingCacheKey(Left(request)))
          .getOrElse(List())
          .asInstanceOf[List[PendingCacheStructure]]
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
      googleBudgetCollection.findOne(DBObject("apiId" -> api_id)) match {
        case Some(budgetObj) =>
          def budget = dboToGoogleEntity[Budget](budgetObj, "budget", None)
          Future(Ok(views.html.google.mcc.account.budget.edit_budget(
            api_id,
            budgetForm.fill(
              BudgetForm(
                apiId = Some(budget.getBudgetId),
                name = budget.getName,
                amount = Some(budget.getAmount.getMicroAmount),
                deliveryMethod = Some(budget.getDeliveryMethod.toString),
                isExplicitlyShared = Some(budget.getIsExplicitlyShared),
                status = Some(budget.getStatus.toString)
              )
            )
          )))
        case None =>
          Future(Redirect(controllers.google.mcc.account.routes.BudgetController.budgets()))
      }
  }

  def createBudget = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      val current_cache = Await.result(Shared.Shared.redisClient.lrange[PendingCacheStructure](pendingCacheKey(Left(request)), 0, -1), 5 seconds).toList
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
          Shared.Shared.redisClient.lpush(
            pendingCacheKey(Left(request)),
            (current_cache :+ PendingCacheStructure(
              id = current_cache.length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.GOOGLE,
              changeCategory = ChangeCategory.BUDGET,
              changeData = budgetFormToDbo(budget)
            )): _*
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
          val field_names = Utilities.getCaseClassParameter[Budget]
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
                    changeData = budgetFormToDbo(budget)
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
              changeData = budgetFormToDbo(budget)
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
          changeData = DBObject("apiId" -> api_id)
        )
      )
      Future(Redirect(controllers.google.mcc.account.routes.BudgetController.budgets()))
  }
  
  def amount(budget_id: Long) = deadbolt.Dynamic(name = PermissionGroup.GoogleWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      request.getQueryString("dollarAmt") match {
        case Some(amtStr) => 
          val dollarAmt = amtStr.toDouble
          googleBudgetCollection.findOne(DBObject("budget.object.budgetId" -> budget_id)) match {
            case Some(budgetObj) =>
              val budget = dboToGoogleEntity[Budget](budgetObj, "budget", None)
              setPendingCache(
                Left(request),
                pendingCache(Left(request)) :+ PendingCacheStructure(
                  id = pendingCache(Left(request)).length + 1,
                  changeType = ChangeType.UPDATE,
                  trafficSource = TrafficSource.GOOGLE,
                  changeCategory = ChangeCategory.BUDGET,
                  changeData = budgetFormToDbo(
                    BudgetForm(
                      apiId = Some(budget_id),
                      name = budget.getName,
                      amount = Some(dollarsToMicro(dollarAmt)),
                      deliveryMethod = Some(budget.getDeliveryMethod.toString),
                      isExplicitlyShared = Some(budget.getIsExplicitlyShared),
                      status = Some(budget.getStatus.toString)
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
