package controllers.msn.api_account.customer

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.microsoft.bingads.customermanagement.Customer
import com.mongodb.casbah.Imports._
import helpers.msn.api_account.customer.CustomerControllerHelper
import models.mongodb.msn.Msn._
import models.mongodb.{PermissionGroup, Utilities}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import security.HandlerKeys
import scala.concurrent.ExecutionContext.Implicits.global

import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class CustomerController @Inject()(val messagesApi: MessagesApi, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders) extends Controller with I18nSupport {
  import CustomerControllerHelper._

  def json = Action.async {
    implicit request =>
      var qry = DBObject.newBuilder
      var tsecsProjection = DBObject()
      List(
        "mccObjId",
        "mccApiId",
        "accountObjId",
        "accountApiId",
        "tsecs"
      ).foreach(x =>
        request.getQueryString(x) match {
          case Some(value) =>
            if (x == "tsecs") {
              tsecsProjection = DBObject("customer" -> ("$elemMatch" ->(
                "startTsecs" -> DBObject("$lte" -> value.toLong),
                "$or" -> MongoDBList(
                  DBObject("endTsecs" -> -1),
                  DBObject("endTsecs" -> DBObject("$gte" -> value.toLong))
                )
                )))
            } else {
              qry += (x -> value)
            }
          case _ =>
        }
      )

      tsecsProjection.putAll(DBObject("customer" -> DBObject("$slice" -> -1)))
      val customers = msnCustomerCollection.find(qry.result, tsecsProjection).toList
      Future(Ok(com.mongodb.util.JSON.serialize(customers)))
  }

  def customers(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name=PermissionGroup.MSNRead.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.msn.api_account.customer.customers(
        msnCustomerCollection.find().skip(page * pageSize).limit(pageSize).toList.map(dboToMsnEntity[Customer](_, "customer", None)),
        page,
        pageSize,
        orderBy,
        filter,
        msnCustomerCollection.count(),
        pendingCache(Left(request))
          .filter(
            x =>
              x.trafficSource == TrafficSource.MSN && x.changeCategory == ChangeCategory.CUSTOMER
          )
      )))
  }


  def newCustomer = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request => Future(Ok(views.html.msn.api_account.customer.new_customer(customerForm, List())))
  }


  def editCustomer(id: String) = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      msnCustomerCollection.findOne(DBObject("customerApiId" -> id)) match {
        case Some(customer_obj) =>
          val customer = gson.fromJson(
            customer_obj.expand[String]("customer").get,
            classOf[com.microsoft.bingads.customermanagement.Customer]
          )
          Future(Ok(views.html.msn.api_account.customer.edit_customer(
            id,
            customerForm.fill(
              CustomerForm(
                customerApiId = Some(customer.getId),
                customerNumber = Some(customer.getNumber),
                name = customer.getName,
                customerAddress = CustomerAddress(
                  line1 = Some(customer.getCustomerAddress.getLine1),
                  line2 = Some(customer.getCustomerAddress.getLine2),
                  line3 = Some(customer.getCustomerAddress.getLine3),
                  line4 = Some(customer.getCustomerAddress.getLine4),
                  city = Some(customer.getCustomerAddress.getCity),
                  stateOrProvince = Some(customer.getCustomerAddress.getStateOrProvince),
                  postalCode = Some(customer.getCustomerAddress.getPostalCode),
                  countryCode = Some(customer.getCustomerAddress.getCountryCode)
                )
              )
            )
          )))
        case None =>
          Future(BadRequest("Not Found"))
      }
  }


  def createCustomer = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      customerForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(views.html.msn.api_account.customer.new_customer(formWithErrors, List()))),
        customer => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.NEW,
              trafficSource = TrafficSource.MSN,
              changeCategory = ChangeCategory.CUSTOMER,
              changeData = customerFormToDbo(customer)
            )
          )
          Future(Redirect(controllers.msn.api_account.customer.routes.CustomerController.customers()))
        }
      )
  }

  def saveCustomer(id: String) = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      customerForm.bindFromRequest.fold(
        formWithErrors => {
          Future(BadRequest(
            views.html.msn.api_account.customer.edit_customer(
              id,
              formWithErrors
            )
          ))
        },
        customer => {
          setPendingCache(
            Left(request),
            pendingCache(Left(request)) :+ PendingCacheStructure(
              id = pendingCache(Left(request)).length + 1,
              changeType = ChangeType.UPDATE,
              trafficSource = TrafficSource.MSN,
              changeCategory = ChangeCategory.CUSTOMER,
              changeData = customerFormToDbo(customer)
            )
          )
          Future(Redirect(controllers.msn.api_account.customer.routes.CustomerController.customers()))
        }
      )
  }

  def deleteCustomers(id: String) = deadbolt.Dynamic(name=PermissionGroup.MSNWrite.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      setPendingCache(
        Left(request),
        pendingCache(Left(request)) :+ PendingCacheStructure(
          id = pendingCache(Left(request)).length + 1,
          changeType = ChangeType.DELETE,
          trafficSource = TrafficSource.MSN,
          changeCategory = ChangeCategory.CUSTOMER,
          changeData = DBObject("apiId" -> id)
        )
      )
      Future(Redirect(controllers.msn.api_account.customer.routes.CustomerController.customers()))
  }


  def bulkNewCustomer = deadbolt.Dynamic(name = PermissionGroup.MSNWrite.entryName, handler = handlers(HandlerKeys.defaultHandler))(parse.multipartFormData) {
    implicit request => {
      var error_list = new ListBuffer[String]()
      request.body.file("bulk").foreach {
        bulk => {
          val field_names = Utilities.getCaseClassParameter[CustomerForm]
          val customer_data_list = Utilities.bulkImport(bulk, field_names)
          for (((customer_data, action), index) <- customer_data_list.zipWithIndex) {
            customerForm.bind(customer_data.map(kv => (kv._1, kv._2)).toMap).fold(
              formWithErrors => {
                error_list += "Row " + index.toString + ": " + formWithErrors.errorsAsJson.toString
              },
              customer => {
                setPendingCache(
                  Left(request),
                  pendingCache(Left(request)) :+ PendingCacheStructure(
                    id = pendingCache(Left(request)).length + 1,
                    changeType = ChangeType.withName(action.toUpperCase),
                    trafficSource = TrafficSource.MSN,
                    changeCategory = ChangeCategory.CUSTOMER,
                    changeData = customerFormToDbo(customer)
                  )
                )
              }
            )
          }
        }
      }
      if (error_list.nonEmpty) {
        Future(BadRequest(views.html.msn.api_account.customer.new_customer(
          customerForm,
          error_list.toList
        )))
      } else {
        Future(Redirect(controllers.msn.api_account.customer.routes.CustomerController.customers()))
      }
    }
  }
}

