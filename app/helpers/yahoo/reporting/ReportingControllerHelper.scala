package helpers.yahoo.reporting

import com.mongodb.casbah.Imports._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DurationFieldType, Days, DateTime}

import scala.collection.mutable.ListBuffer

object ReportingControllerHelper {
  val yahooReportDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

  def buildQry(advertiserId: String, campaignId: String, adGroupId: String, adId: String, keywordId: String, startDate: String, endDate: String): DBObject = {
    val qry = DBObject.newBuilder
    if(advertiserId.nonEmpty) { qry += ("advertiser id" -> advertiserId) }
    if(campaignId.nonEmpty) { qry += ("campaign id" -> campaignId) }
    if(adGroupId.nonEmpty) { qry += ("ad group id" -> adGroupId) }
    if(adId.nonEmpty) { qry += ("ad id" -> adId) }
    if(keywordId.nonEmpty) { qry += ("keyword id" -> keywordId) }

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
      rangeList += yahooReportDateFormatter.print(s.withFieldAdded(DurationFieldType.days(), day))
    }

    qry += ("day" -> DBObject("$in" -> MongoDBList(rangeList: _*)))

    qry.result()
  }
}
