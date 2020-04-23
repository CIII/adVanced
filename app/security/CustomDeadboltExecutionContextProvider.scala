package security

import be.objectify.deadbolt.scala.DeadboltExecutionContextProvider

import scala.concurrent.ExecutionContext

class CustomDeadboltExecutionContextProvider extends DeadboltExecutionContextProvider {
  override def get(): ExecutionContext = scala.concurrent.ExecutionContext.global
}

