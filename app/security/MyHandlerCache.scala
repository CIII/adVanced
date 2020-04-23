package security

import javax.inject.Singleton

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{DeadboltHandler, HandlerKey}

@Singleton
class MyHandlerCache extends HandlerCache {

  val defaultHandler: DeadboltHandler = new MyDeadboltHandler

  val handlers: Map[Any, DeadboltHandler] = Map(
    HandlerKeys.defaultHandler -> defaultHandler,
    HandlerKeys.altHandler -> new MyDeadboltHandler(Some(new MyDynamicResourceHandler()))
  )

  override def apply(): DeadboltHandler = defaultHandler

  override def apply(handlerKey: HandlerKey): DeadboltHandler = handlers(handlerKey)
}