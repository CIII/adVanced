package sync.msn.bingads.account

import com.microsoft.bingads.customermanagement._
import sync.msn.bingads.BingAdsHelper

class AccountHelper(BingAds: BingAdsHelper) {

  def getAccount(accountId: Long): Option[Account] = {
    val getAccountRequest = new GetAccountRequest
    getAccountRequest.setAccountId(accountId)
    val result = BingAds.customerManagementService.getService.getAccount(getAccountRequest)
    result.getAccount match {
      case account =>
        Some(account)
    }
  }

  def findAccountsOrCustomersInfo(
    applicationScope: ApplicationType = ApplicationType.ADVERTISER,
    filter: Option[String] = None,
    top: Int = 5000
  ): ArrayOfAccountInfoWithCustomerData = {
    val findAccountsOrCustomersInfoRequest = new FindAccountsOrCustomersInfoRequest()
    findAccountsOrCustomersInfoRequest.setFilter(filter.getOrElse(""))
    findAccountsOrCustomersInfoRequest.setApplicationScope(applicationScope)
    findAccountsOrCustomersInfoRequest.setTopN(top)
    val result = BingAds.customerManagementService.getService.findAccountsOrCustomersInfo(findAccountsOrCustomersInfoRequest)
    result.getAccountInfoWithCustomerData
  }
}
