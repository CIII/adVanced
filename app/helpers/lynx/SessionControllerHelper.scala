package helpers.lynx

import org.mongodb.scala.bson.Document
import org.mongodb.scala.bson.BsonArray
import scala.jdk.CollectionConverters._

object SessionControllerHelper {
  def formsToHtml(formsRaw: BsonArray): String = {
    formsRaw.getValues.asScala.map { form =>
      val formDBO = Document(form.asDocument())
      formDBO.keys.map { key =>
        s"field: $key; value: ${formDBO(key).toString}"
      }.mkString("<br/>")
    }.mkString("<br/>---<br/>")
  }

  def sessionAttributesToHtml(sessionAttributesRaw: BsonArray): String = {
    sessionAttributesRaw.getValues.asScala.map { sessionAttribute =>
      val sessionAttributeDBO = Document(sessionAttribute.asDocument())
      sessionAttributeDBO.keys.map { key =>
        s"attr: $key; value: ${sessionAttributeDBO(key).toString}"
      }.mkString("<br/>")
    }.mkString("<br/>")
  }

  def eventsToHtml(eventsRaw: BsonArray): String = {
    eventsRaw.getValues.asScala.map { event =>
      val eventDBO = Document(event.asDocument())
      eventDBO.keys.map { key =>
        s"event: $key; value: ${eventDBO(key).toString}"
      }.mkString("<br/>")
    }.mkString("<br/>")
  }
}
