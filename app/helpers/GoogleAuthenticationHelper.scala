package helpers

import javax.inject.Inject
import scala.concurrent.ExecutionContext

// TODO: Google OAuth helper stub - the following classes are no longer available in the
// versions of google-api-client / google-api-services-oauth2 declared in build.sbt:
//   - com.google.api.services.oauth2.model.Userinfoplus  (removed; use Userinfo)
//   - com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
//   - com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
// This helper is kept as a stub so the project compiles. Re-implement using the
// current Google Identity / OAuth2 client libraries when server-side OAuth is needed.

class GoogleAuthenticationHelper @Inject()(implicit val configuration: play.api.Configuration, ec: ExecutionContext) {

  val clientId: String      = configuration.get[String]("google_auth.client_id")
  val clientSecret: String  = configuration.get[String]("google_auth.client_secret")
  val appName: String       = configuration.get[String]("google_auth.application_name")
  val redirectUri: String   = configuration.get[String]("google_auth.redirect_uri")
  val authTimeout: Int      = configuration.get[Int]("google_auth.timeout_seconds")

  /** TODO: Implement with current Google Identity OAuth2 libraries. */
  @throws[java.util.concurrent.TimeoutException]
  @throws[UnsupportedOperationException]
  def authenticate: Map[String, String] =
    throw new UnsupportedOperationException(
      "GoogleAuthenticationHelper.authenticate is not yet implemented. " +
      "Re-implement using the current Google Identity OAuth2 client library."
    )
}
