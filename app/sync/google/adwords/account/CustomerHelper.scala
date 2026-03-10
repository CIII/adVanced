package sync.google.adwords.account

import play.api.Logging

/**
 * Helper for Google Ads customer/account operations.
 *
 * TODO: Re-implement with Google Ads API v18.
 * - CustomerServiceClient for customer operations
 * - ManagedCustomerService → CustomerClient
 * - BudgetService → CampaignBudgetServiceClient
 * - BiddingStrategyService → BiddingStrategyServiceClient
 * - Reports via GoogleAdsServiceClient.searchStream() with GAQL
 * - Selector-based queries → GAQL queries
 */
class CustomerHelper extends Logging {
  // TODO: Implement with Google Ads API v18
}
