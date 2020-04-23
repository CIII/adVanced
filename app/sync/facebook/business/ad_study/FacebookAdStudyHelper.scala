package sync.facebook.business.ad_study

import models.mongodb.facebook.Facebook._
import scala.concurrent.duration._
import com.mongodb.casbah.Imports._
import sync.facebook.business.FacebookBusinessHelper
import scala.concurrent.Await
import Shared.Shared._
import akka.event.LoggingAdapter
import org.joda.time.DateTime
import play.api.libs.json.JsObject

/**
 * The Facebook AdsInsights SDK does not currently support split testing.
 * Therefore we have to build the call manually.
 *  
 */
class FacebookAdStudyHelper(
    fbBizAccount: FacebookBusinessAccount, 
    log: LoggingAdapter
) extends FacebookBusinessHelper(fbBizAccount, log) {
  
  /**
   * Retrieve a list of all split tests associated with this business account.
   */
  def getSplitTests: List[FacebookSplitTest] = {
    val response = Await.result(requestWithTimeout(fbGraphUrl.concat(fbBizAccount.accountNumber).concat("/ad_studies")).get, Duration.Inf)
    val data = (response.json \ "data").as[List[JsObject]]
    log.debug("Retrieved ".concat(data.length.toString).concat(" Split Tests"))
    data.map { splitTestObj =>
      val splitTest = parseSplitTestFromFacebookResponse(splitTestObj)
      val cellTuple = getSplitTestCells(splitTest)
      splitTest.cells = cellTuple._1
      splitTest.testType = cellTuple._2
      splitTest
    }
  }
  
  /**
   * Given a jsobject representation of the "data" object in the Facebook ad_study response, 
   * parse it into a FacebookSplitTest object
   */
  def parseSplitTestFromFacebookResponse(splitTestObj: JsObject): FacebookSplitTest = {
    FacebookSplitTest (
        Some(new ObjectId),
        fbBizAccount._id,
        Some((splitTestObj \ "id").as[String]),
        (splitTestObj \ "name").as[String],
        (splitTestObj \ "description").as[String],
        DateTime.parse((splitTestObj \ "start_time").as[String]),
        DateTime.parse((splitTestObj \ "end_time").as[String]),
        "",
        List[FacebookSplitTestCell]()
    )
  }
  
  /**
   * Retrieve each of the "cells" for an ad_study.  The cells are a different edge, and therefore require a separate call.
   * return the list of cells, and the type of the cells as a tuple
   */
  def getSplitTestCells(splitTest: FacebookSplitTest): (List[FacebookSplitTestCell], String) = {
    val response = Await.result(requestWithTimeout(fbGraphUrl.concat(splitTest.adStudyId.get).concat("/cells")).get, Duration.Inf)
    val data = (response.json \ "data").as[List[JsObject]]
    var cellType = ""
    (data.map(splitTestCellObj => {
        var cellTuple = parseSplitTestCellFromFacebookResponse(splitTestCellObj)
        cellType = cellTuple._2
        cellTuple._1
      }
    ), cellType)
  }

  /**
   * Given a jsobject representation of the "data" object in the Facebook ad_study/cells response,
   * parse it into a FacebookSplitTestCell object
   */
  def parseSplitTestCellFromFacebookResponse(splitTestCellObj: JsObject): (FacebookSplitTestCell, String) = {
    
    val campaigns = parseEntitiesFromSplitTestCellObj("campaigns", splitTestCellObj)
    val adsets = parseEntitiesFromSplitTestCellObj("adsets", splitTestCellObj)
    val adaccounts = parseEntitiesFromSplitTestCellObj("adaccounts", splitTestCellObj)
    var entityTuple : (List[String], String) =
      { // Set the entity ids depending on which has content.  Only one should not be empty.
          if(campaigns.nonEmpty){
            ( campaigns, "campaign")
          } 
          else if(adsets.nonEmpty){
            (adsets, "adsets")
          } 
          else{ 
            (adaccounts, "adaccounts") 
          }
      }
    
    (FacebookSplitTestCell(
        Some((splitTestCellObj \ "id").as[String]),
        (splitTestCellObj \ "name").as[String],
        (splitTestCellObj \ "treatment_percentage").as[Int],
        entityTuple._1
    ), entityTuple._2)
  }
  
  /**
   * Each split test cell can contain a number of entities, which are either campaigns, adaccounts, or adsets. Given
   * an entitiy jsobject, return each of the entity ids in a string list.
   */
  def parseEntitiesFromSplitTestCellObj(entityType: String, splitTestCellObj: JsObject): List[String] = {
    if(splitTestCellObj.keys.contains(entityType)){
      (splitTestCellObj \ entityType \ "data").as[List[JsObject]].map(
          data => (data \ "id").as[String]
      )
    } else {
      List[String]()
    }
  }
  
  /**
   * Create a new split test.  On success, get the split test ad study id and update the split test in mongo.
   */
  def createSplitTest(splitTest: FacebookSplitTest) {
    log.debug("Creating split test: " + splitTest._id.toString)
    val body = createFbSplitTestRequestBody(splitTest)
    val response = Await.result(
        this.requestWithTimeout(fbGraphUrl.concat(fbBizAccount.accountNumber).concat("/ad_studies")).post(body), 
        Duration.Inf
    )
    
    // Update the new ad study with the ad study Id & insert the object into mongo
    splitTest.adStudyId = Some((response.json \ "id").as[String])
    facebookSplitTestCollection.insert(
        FacebookSplitTest.toDBO(splitTest)
    )
  }
  
  /**
   * Update a split test via Facebook API and update mongo
   */
  def updateSplitTest(splitTest: FacebookSplitTest) {
    log.debug("Updating split test: " + splitTest._id.toString)
    val body = createFbSplitTestRequestBody(splitTest)
    this.requestWithTimeout(this.fbGraphUrl.concat(splitTest.adStudyId.get)).post(body)
    facebookSplitTestCollection.update(
        DBObject("_id" -> splitTest._id),
        FacebookSplitTest.toDBO(splitTest)
    )
  }
  
  /**
   * Delete a split test via Facebook API and update mongo.
   */
  def deleteSplitTest(splitTest: FacebookSplitTest) {
    log.debug("Deleting split test: " + splitTest._id.toString)
    this.requestWithTimeout(this.fbGraphUrl.concat(splitTest.adStudyId.get)).delete
    facebookSplitTestCollection.remove(
        DBObject("_id" -> splitTest._id)
    )
  }
  
  /**
   * Given a split test, populate a form encoded url string with the values from the object.
   */
  def createFbSplitTestRequestBody(splitTest: FacebookSplitTest): String = {
    var body = "name=%s&description=%s&start_time=%s&end_time=%s&type=SPLIT_TEST".format(
          splitTest.name, splitTest.description, splitTest.startTime.getMillis, splitTest.endTime.getMillis
        )
    
    if(splitTest.cells.nonEmpty){
      body.concat("&cells=[")
      for(cell <- splitTest.cells){
        body.concat("{name:\"%s\", treatment_percentage:%d, %s:[%s]},".format(
          cell.name, cell.treatmentPercentage, splitTest.testType, cell.entityIds.mkString(",")
        ))
      }
      
      body.dropRight(1)  // remove the last comma from the cell array.
      body.concat("]")
    }
    
    body
  }
}