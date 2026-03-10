package modules

import play.api.inject.{SimpleModule, bind}

/**
 * Replaces Global.scala onStart. Eager binding ensures
 * StartupTasks is instantiated when the application starts.
 */
class StartupModule extends SimpleModule(bind[StartupTasks].toSelf.eagerly())
