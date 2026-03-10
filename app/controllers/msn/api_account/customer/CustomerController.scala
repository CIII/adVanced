package controllers.msn.api_account.customer

import javax.inject.Inject

import Shared.Shared._
import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{ActionBuilders, DeadboltActions}
import com.microsoft.bingads.v13.customermanagement.Customer
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import helpers.msn.api_account.customer.CustomerControllerHelper
import models.mongodb.MongoExtensions._
import models.mongodb.msn.Msn._
import models.mongodb.{PermissionGroup, Utilities}
import play.api.i18n.I18nSupport
import play.api.mvc._
import security.HandlerKeys
import scala.concurrent.ExecutionContext

import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class CustomerController @Inject()(val controllerComponents: ControllerComponents, deadbolt: DeadboltActions, handlers: HandlerCache, actionBuilder: ActionBuilders)(implicit ec: ExecutionContext) extends BaseController with I18nSupport {
  import CustomerControllerHelper._

  def json = Action.async {
    implicit request =>
      var qry = Document()
      var tsecsProjection = Document()
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
              tsecsProjection = Document("customer" -> Document("$elemMatch" -> Document(
                "startTsecs" -> Document("$lte" -> value.toLong),
                "$or" -> Seq(
                  Document("endTsecs" -> -1),
                  Document("endTsecs" -> Document("$gte" -> value.toLong))
                )
              )))
            } else {
              qry = qry ++ Document(x -> value)
            }
          case _ =>
        }
      )

      tsecsProjection = tsecsProjection ++ Document("customer" -> Document("$slice" -> -1))
      val customers = msnCustomerCollection.find(qry).projection(tsecsProjection).toList
      Future(Ok(customers.map(_.toJson()).mkString("[", ",", "]")))
  }

  def customers(page: Int, pageSize: Int, orderBy: Int, filter: String) = deadbolt.Dynamic(name=PermissionGroup.MSNRead.entryName, handler=handlers(HandlerKeys.defaultHandler))() {
    implicit request =>
      Future(Ok(views.html.msn.api_account.customer.customers(
        msnCustomerCollection.find().skip(page * pageSize).limit(pageSize).toList.map(documentToMsnEntity[Customer](_, "customer", None)),
        page,
        pageSize,
        orderBy,
        filter,
        msnCustomerCollection.countSync().toInt,
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
      msnCustomerCollection.findOne(Document("customerApiId" -> id)) match {
        case Some(customer_obj) =>
          val customer = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
            customer_obj.getString("customer"),
            classOf[com.microsoft.bingads.v13.customermanagement.Customer]
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
              changeData = customerFormToDocument(customer)
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
              changeData = customerFormToDocument(customer)
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
          changeData = Document("apiId" -> id)
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
                    changeData = customerFormToDocument(customer)
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

