package helpers.facebook.reporting

import com.mongodb.casbah.Imports._
import org.joda.time.{DurationFieldType, Days, DateTime}
import org.joda.time.format.DateTimeFormat

import scala.collection.mutable.ListBuffer

object ReportingControllerHelper {

  val facebookReportDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

  def buildQry(campaignId: String, adGroupId: String, adId: String, keywordId: String, startDate: String, endDate: String): DBObject = {
    val qry = DBObject.newBuilder
    /*if(campaignId.nonEmpty) { qry += ("campaign id" -> campaignId) }
    if(adGroupId.nonEmpty) { qry += ("ad group id" -> adGroupId) }
    if(adId.nonEmpty) { qry += ("ad id" -> adId) }
    if(keywordId.nonEmpty) { qry += ("keyword id" -> keywordId) }*/

    val rangeList = ListBuffer[String]()
    var s: DateTime = DateTime.now.minusDays(1)
    var e = DateTime.now

    if(startDate.nonEmpty && endDate.nonEmpty) {
      s = DateTime.parse(startDate)
      e = DateTime.parse(endDate)
    } else if(startDate.nonEmpty && endDate.isEmpty) {
      s = DateTime.parse(startDate)
      e = s.plusDays(1)
    }

    val days = Days.daysBetween(s, e).getDays
    for(day <- Range(0, days)) {
      rangeList += facebookReportDateFormatter.print(s.withFieldAdded(DurationFieldType.days(), day))
    }

    qry += ("day" -> DBObject("$in" -> MongoDBList(rangeList: _*)))

    qry.result()
  }
}
