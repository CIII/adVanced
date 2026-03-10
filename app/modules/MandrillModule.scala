package modules

import play.api.inject.Module
import play.api.{Environment, Configuration}

/**
 * Placeholder for Mandrill email configuration.
 * TODO: Re-implement email sending using Play WS client.
 */
class MandrillModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = Seq.empty
}
