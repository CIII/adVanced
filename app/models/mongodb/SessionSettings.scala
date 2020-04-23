package models.mongodb

import org.joda.time.DateTime

case class SessionSettings (
   var chartStartDate: DateTime,
   var chartEndDate: DateTime
)

object SessionSettings {
  
  var userSettings: Map[String, SessionSettings] = Map()
  
  def getSettings(user: UserAccount): SessionSettings = {
    if(!userSettings.contains(user.userName)){
      userSettings += (user.userName -> getDefaultSettings)
    }
    
    userSettings(user.userName)
  }
  
  def setChartStartDate(user: UserAccount, dt: DateTime) = {
    getSettings(user).chartStartDate = dt
  }
  
  def setChartEndDate(user: UserAccount, dt: DateTime) = {
    getSettings(user).chartEndDate = dt
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