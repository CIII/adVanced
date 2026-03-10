package sync.scripts.shared

import java.net.URLDecoder

import org.apache.pekko.actor.ActorSystem
import play.api.Logging

import scala.collection.mutable.ListBuffer

/**
 * Shared utilities for sync scripts.
 *
 * TODO: Remove squeryl dependency (org.squeryl.PrimitiveTypeMode) - replaced by Slick.
 * TODO: Replace Google Ads API error handling when Google Ads v18 is integrated.
 */
object Shared extends Logging {
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

  val batch_limit = 200

  val usage =
    """
    Usage: FixDestinationUrls [--client-id num]
    """

  case class EntityMap(adGroupApiId: Long, keywordApiId: Long, internalKeywordId: Long, destinationUrl: String)

  def checkUrl(destinationUrl: String): Boolean = true

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

  def nextOption(map: OptionMap, list: List[String]): OptionMap = {
    list match {
      case Nil => map
      case "--client-id" :: value :: tail =>
        nextOption(map ++ Map(Symbol("clientid") -> value.toInt), tail)
      case option :: tail =>
        logger.error(s"Unknown option $option")
        sys.exit(1)
    }
  }
}

object Google {
  def microAmountMultiplier = 1000000

  /**
   * TODO: Replace with Google Ads API v18 error handling.
   */
  def checkErrors(partialErrors: Seq[String], operationCount: Int, context: String): Boolean = {
    if (partialErrors.nonEmpty) {
      val errorTypes = new ListBuffer[String]()
      for (error <- partialErrors) {
        if (!errorTypes.contains(error)) {
          println(s"Job ($context) ${partialErrors.count(_ == error)} $error errors out of $operationCount operations")
          errorTypes += error
        }
      }
      true
    } else {
      println(s"Job ($context) 0 errors out of $operationCount operations")
      false
    }
  }
}

object Msn {
  /**
   * TODO: Update for Bing Ads SDK v13 error types.
   */
  def checkErrors(errors: Seq[String], operationCount: Int, context: String): Boolean = {
    if (errors.nonEmpty) {
      val errorTypes = new ListBuffer[String]()
      for (error <- errors) {
        if (!errorTypes.contains(error)) {
          println(s"Job ($context) ${errors.count(_ == error)} $error errors out of $operationCount operations")
          errorTypes += error
        }
      }
      true
    } else {
      println(s"Job ($context) 0 errors out of $operationCount operations")
      false
    }
  }
}
