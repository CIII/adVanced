package models.mongodb

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._
import org.joda.time.DateTime

case class SessionSettings (
   chartStartDate: DateTime,
   chartEndDate: DateTime
)

object SessionSettings {

  private val userSettings = new ConcurrentHashMap[String, SessionSettings]().asScala

  def getSettings(user: UserAccount): SessionSettings = {
    userSettings.getOrElseUpdate(user.userName, getDefaultSettings)
  }

  def setChartStartDate(user: UserAccount, dt: DateTime): Unit = {
    userSettings.put(user.userName, getSettings(user).copy(chartStartDate = dt))
  }

  def setChartEndDate(user: UserAccount, dt: DateTime): Unit = {
    userSettings.put(user.userName, getSettings(user).copy(chartEndDate = dt))
  }

  def getDefaultSettings = SessionSettings(
    getDefaultChartStartDate,
    getDefaultChartEndDate
  )

  def getDefaultChartStartDate: DateTime = {
    DateTime.now.minusDays(30)
  }

  def getDefaultChartEndDate: DateTime = {
    DateTime.now
  }
}
