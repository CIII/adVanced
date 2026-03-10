package helpers.google.reporting

import org.mongodb.scala.bson.Document
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DurationFieldType, Days, DateTime}

import scala.collection.mutable.ListBuffer

object ReportingControllerHelper {

  val googleReportDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

  def buildQry(campaignId: String, adGroupId: String, adId: String, keywordId: String, startDate: String, endDate: String): Document = {
    var qry = Document()
    if (campaignId.nonEmpty) { qry = qry ++ Document("campaign id" -> campaignId) }
    if (adGroupId.nonEmpty) { qry = qry ++ Document("ad group id" -> adGroupId) }
    if (adId.nonEmpty) { qry = qry ++ Document("ad id" -> adId) }
    if (keywordId.nonEmpty) { qry = qry ++ Document("keyword id" -> keywordId) }

    val rangeList = ListBuffer[String]()
    var s: DateTime = DateTime.now.minusMonths(6)
    var e = DateTime.now

    if (startDate.nonEmpty && endDate.nonEmpty) {
      s = DateTime.parse(startDate)
      e = DateTime.parse(endDate)
    } else if (startDate.nonEmpty && endDate.isEmpty) {
      s = DateTime.parse(startDate)
      e = s.plusDays(1)
    }

    val days = Days.daysBetween(s, e).getDays
    for (day <- Range(0, days)) {
      rangeList += googleReportDateFormatter.print(s.withFieldAdded(DurationFieldType.days(), day))
    }

    qry = qry ++ Document("day" -> Document("$in" -> rangeList.toList))
    qry
  }
}
