package sync.scripts.shared

import java.net.URLDecoder

import akka.actor.ActorSystem
import com.google.api.ads.adwords.axis.v201609.cm.ApiError
import com.microsoft.bingads.v11.bulk.BatchError
import org.squeryl.PrimitiveTypeMode._

import scala.collection.mutable.ListBuffer

object Shared{
  val matchType = "mt={matchtype}"
  val device = "dt={device}"
  val network = "nw={network}"
  val adPosition = "ap={adposition}"
  val semReporting = "semreporting=External"
  val googleCreative = "creativeid={creative}"
  val msnCreative = "creativeid={AdId}"

  def qsKey(qsPair: String): String = {
    qsPair.split("=")(0).toLowerCase
  }

  val system = ActorSystem("FixDestinationUrlSystem")
  val batch_limit = 200
  var running_processes = 0
  var client_id: Option[Any] = None
  val usage =
    """
    Usage: FixDestinationUrls [--client-id num]
    """

  case class EntityMap(var ad_group_api_id: Long, var keyword_api_id: Long, var internal_keyword_id: Long, var destination_url: String)

  def checkUrl(destinationUrl: String): Boolean = {
    true
  }

  def parseUriParameters(uri: String): Map[String, String] = {
    var params = Map[String, String]()
    val parts = uri split "\\?"
    if (parts.length > 1) {
      val query = parts(1)
      query split "&" foreach { param =>
        val pair = param split "="
        val key = URLDecoder.decode(pair(0), "UTF-8")
        val value = pair.length match {
          case l if l > 1 => URLDecoder.decode(pair(1), "UTF-8")
          case _ => ""
        }
            params += key -> value
      }
    }
    params
  }

  case class destinationUrlMapping(
    adGroupId: Long,
    entityId: Long,
    destinationUrl: String
  )

  type OptionMap = Map[Symbol, Any]

  case object StartMessage
  case object StopMessage

  def nextOption(map : OptionMap, list: List[String]) : OptionMap = {
    list match {
      case Nil => map
      case "--client-id" :: value :: tail =>
        nextOption(map ++ Map('clientid -> value.toInt), tail)
      case option :: tail => println("Unknown option %s".format(option))
        sys.exit(1)
    }
  }
}

object Google {
  def microAmountMultiplier = 1000000

  def checkErrors(partialErrors: Array[ApiError], operationCount: Int, context: String): Boolean = {
    if (partialErrors != null && partialErrors.length > 0) {
      val errorTypes = new ListBuffer[String]()
      for (error <- partialErrors) {
        if (!(errorTypes contains error.getErrorString)) {
          println("Job (%s) %s %s errors out of %s operations".format(
            context,
            partialErrors.count(err => error.getErrorString == err.getErrorString),
            error.getErrorString,
            operationCount
          )
          )
          errorTypes += error.getErrorString
        }
      }
      true
    } else {
      println("Job (%s) 0 errors out of %s operations".format(context, operationCount))
      false
    }
  }
}

object Msn {
  def checkErrors(errors: Array[BatchError], operationCount: Int, context: String): Boolean = {
    if (errors != null && errors.length > 0) {
      val errorTypes = new ListBuffer[String]()
      for (error <- errors) {
        if (!(errorTypes contains error.getErrorCode)) {
          println("Job (%s) %s %s errors out of %s operations".format(
            context,
            errors.count(err => error.getErrorCode == err.getErrorCode),
            error.getMessage,
            operationCount
          )
          )
          errorTypes += error.getErrorCode
        }
      }
      true
    } else {
      println("Job (%s) 0 errors out of %s operations".format(context, operationCount))
      false
    }
  }
}