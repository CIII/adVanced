package helpers.lynx

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBList
import collection.JavaConverters._

object SessionControllerHelper {
  def formsToHtml(formsRaw: MongoDBList): String = {
    formsRaw.map { form =>
      val formDBO = form.asInstanceOf[DBObject]
      formDBO.keySet().asScala.map { key =>
        s"field: $key; value: ${formDBO.get(key).toString}"
      }.mkString("<br/>")
    }.mkString("<br/>---<br/>")
  }

  def sessionAttributesToHtml(sessionAttributesRaw: MongoDBList): String = {
    sessionAttributesRaw.map { sessionAttribute =>
      val sessionAttributeDBO = sessionAttribute.asInstanceOf[DBObject]
      sessionAttributeDBO.keySet().asScala.map { key =>
        s"attr: $key; value: ${sessionAttributeDBO.get(key).toString}"
      }.mkString("<br/>")
    }.mkString("<br/>")
  }

  def eventsToHtml(eventsRaw: MongoDBList): String = {
    eventsRaw.map { event =>
      val eventDBO = event.asInstanceOf[DBObject]
      eventDBO.keySet().asScala.map { key =>
        s"event: $key; value: ${eventDBO.get(key).toString}"
      }.mkString("<br/>")
    }.mkString("<br/>")
  }
}
