package models.mongodb.performance

import models.mongodb.google.GoogleCampaignPerformance
import models.mongodb.google.GoogleAdGroupPerformance
import models.mongodb.google.GoogleAdPerformance
import models.mongodb.google.GoogleGeoPerformance
import models.mongodb.lynx.TQReportingPerformance
import models.mongodb.google.GoogleAccountPerformance

object PerformanceEntityFactory {
  
  def createGoogleCampaignPerformance(): GoogleCampaignPerformance = {
    new GoogleCampaignPerformance
  }

  def createGoogleAdGroupPerformance(): GoogleAdGroupPerformance = {
    new GoogleAdGroupPerformance
  }

  def createGoogleAdPerformance(): GoogleAdPerformance = {
    new GoogleAdPerformance
  }
  
  def createGoogleGeoPerformance(): GoogleGeoPerformance = {
    new GoogleGeoPerformance
  }

  def createGoogleAccountPerformance(): GoogleAccountPerformance = {
    new GoogleAccountPerformance
  }
  
  def createTQReportingPerformance(): TQReportingPerformance = {
    new TQReportingPerformance
  }
}