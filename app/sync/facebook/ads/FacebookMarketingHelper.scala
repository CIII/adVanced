package sync.facebook.ads

// TODO: Update to facebook-java-business-sdk v20
import com.facebook.ads.sdk.{APIContext, AdAccount}

class FacebookMarketingHelper(
  accountId: String,
  applicationSecret: String,
  accessToken: String
) {
  lazy val context: APIContext = new APIContext(accessToken, applicationSecret)
  lazy val adAccount: AdAccount = new AdAccount(accountId, context)
}
