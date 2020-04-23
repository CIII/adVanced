package sync.msn.bingads

import com.microsoft.bingads.customermanagement.ICustomerManagementService
import com.microsoft.bingads.v11.campaignmanagement.ICampaignManagementService
import com.microsoft.bingads.{AuthorizationData, PasswordAuthentication, ServiceClient}

class BingAdsHelper(
  userName: String,
  password: String,
  developerToken: String,
  customerId: Option[Long] = None,
  customerAccountId: Option[Long] = None
) {

  lazy val authData = new AuthorizationData()
  authData.setDeveloperToken(developerToken)
  authData.setAuthentication(new PasswordAuthentication(userName, password))
  customerId match {
    case Some(id) => authData.setCustomerId(id)
    case _ =>
  }

  customerAccountId match {
    case Some(id) => authData.setAccountId(id)
    case _ =>
  }

  lazy val campaignManagementService = getCampaignManagementService()
  lazy val customerManagementService = getCustomerManagementService()

  private def getCampaignManagementService(
    userName: String = userName,
    password: String = password,
    developerToken: String = developerToken,
    customerId: Option[Long] = customerId,
    customerAccountId: Option[Long] = customerAccountId
  ): ServiceClient[ICampaignManagementService] = new ServiceClient[ICampaignManagementService](authData, classOf[ICampaignManagementService])

  private def getCustomerManagementService(
    userName: String = userName,
    password: String = password,
    developerToken: String = developerToken,
    customerId: Option[Long] = customerId,
    customerAccountId: Option[Long] = customerAccountId
  ): ServiceClient[ICustomerManagementService] = new ServiceClient[ICustomerManagementService](authData, classOf[ICustomerManagementService])
}
