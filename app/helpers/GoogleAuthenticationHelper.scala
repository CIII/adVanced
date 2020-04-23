package helpers

import com.google.api.services.oauth2.model.Userinfoplus
import com.google.api.client.http.HttpTransport
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import javax.inject.Inject
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.Details
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import java.util.Arrays
import com.google.api.client.auth.oauth2.Credential
import com.google.api.services.oauth2.Oauth2
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.services.oauth2.model.Tokeninfo
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import java.util.concurrent.TimeoutException

class GoogleAuthenticationHelper @Inject()( implicit val configuration: play.api.Configuration) {
  val httpTransport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport
  val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance
  val scopes = Arrays.asList(
        "https://www.googleapis.com/auth/userinfo.profile",
        "https://www.googleapis.com/auth/userinfo.email"
  );
  
  val clientId = configuration.getString("google_auth.client_id").getOrElse(throw new Exception("Missing Google Authentication Client Id"))
  val clientSecret = configuration.getString("google_auth.client_secret").getOrElse(throw new Exception("Missing Google Authentication Client Secret"))
  val appName = configuration.getString("google_auth.application_name").getOrElse(throw new Exception("Missing Google Authentication Application Name"))
  val redirectUri = configuration.getString("google_auth.redirect_uri").getOrElse(throw new Exception("Missing Google Authentication Redirect URI"))
  val authTimeout = configuration.getInt("google_auth.timeout_seconds").getOrElse(throw new Exception("Missing Google Authentication Timeout"))
  
  @throws[TimeoutException]
  def authenticate: Userinfoplus = {
      var receiver = (new LocalServerReceiver.Builder)
                        .setHost(redirectUri)
                        .build()
      
      try{
        // Create client secret
        var clientSecrets: GoogleClientSecrets = new GoogleClientSecrets
        var details = new Details
        details.setClientId(clientId)
        details.setClientSecret(clientSecret)
        clientSecrets.setInstalled(details)
        
        // create code flow
        var flow: GoogleAuthorizationCodeFlow = new GoogleAuthorizationCodeFlow.Builder(
            httpTransport,
            jsonFactory,
            clientSecrets,
            scopes
        ).build
        
        var credential: Credential = Await.result(Future {
          new AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
        }, authTimeout second)
        
        // use the credential to authenticate
        var oauth2: Oauth2 = new Oauth2.Builder(
            httpTransport,
            jsonFactory,
            credential
        ).setApplicationName(appName).build
        
        var tokenInfo: Tokeninfo = oauth2.tokeninfo.setAccessToken(credential.getAccessToken).execute
        
        if(!tokenInfo.getAudience.equals(clientSecrets.getDetails.getClientId)){
          throw new Exception("Could not authenticate - Audience does not match client Id")
        }
        
        oauth2.userinfo.get.execute
      } finally {
        receiver.stop()
      }
  }
}
