package modules

import play.api.inject.Module
import play.api.Environment
import play.api.Configuration
import com.tapquality.dao.MandrillDAO
import com.tapquality.dao.impl.MandrillDAO

class MandrillModule extends Module {
  
  def bindings(environment : Environment, configuration : Configuration) = {
    Seq(
        bind[String].qualifiedWith("mandrill.key").to(configuration.getString("mandrill.key").get),
        bind[String].qualifiedWith("mandrill.url").to(configuration.getString("mandrill.url").get),
        bind(classOf[com.tapquality.dao.MandrillDAO]).to(classOf[com.tapquality.dao.impl.MandrillDAO]))
  }
}