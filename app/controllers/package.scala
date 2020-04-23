import com.mongodb.casbah.Imports._
import models.mongodb.Utilities
import play.api.mvc.{AnyContent, Controller, Request}

import scala.collection.immutable.List

package object controllers extends Controller {
  def json(request: Request[AnyContent], fields: List[String], entityName: String, collection: MongoCollection, addl_qry_opt: Option[(String, Any)]=None): String = {
    var qry = DBObject.newBuilder
    addl_qry_opt match {
      case Some(addl_qry) => qry += addl_qry
      case _ =>
    }
    var tsecsProjection = DBObject()
    fields.foreach(x =>
      request.getQueryString(x) match {
        case Some(value) =>
          if (x == "tsecs") {
            tsecsProjection = DBObject(entityName -> ("$elemMatch" ->(
              "startTsecs" -> DBObject("$lte" -> value.toLong),
              "$or" -> MongoDBList(
                DBObject("endTsecs" -> -1),
                DBObject("endTsecs" -> DBObject("$gte" -> value.toLong))
              )
              )))
          } else {
            qry += (x -> value)
          }
        case _ =>
      }
    )

    tsecsProjection.putAll(DBObject(entityName -> DBObject("$slice" -> -1)))
    val entities = collection.find(qry.result(), tsecsProjection).toList
    com.mongodb.util.JSON.serialize(entities)
  }

  object Google {
    case class AdGroupAdParent (
      var mccObjId: Option[String],
      var customerApiId: Option[Long],
      var campaignApiId: Option[Long],
      var adGroupApiId: Option[Long]
    )

    abstract class AdGroupAdForm{
      var parent: AdGroupAdParent
      var apiId: Option[Long]
      var status: Option[String]
    }

    def dboToAdGroupAdParent(dbo: DBObject) = AdGroupAdParent(
      mccObjId=dbo.getAsOrElse[Option[String]]("mccObjId", None),
      customerApiId=dbo.getAsOrElse[Option[Long]]("customerApiId", None),
      campaignApiId=dbo.getAsOrElse[Option[Long]]("campaignApiId", None),
      adGroupApiId=dbo.getAsOrElse[Option[Long]]("adGroupApiId", None)
    )

    def adGroupAdParentToDbo(agap: AdGroupAdParent): DBObject = {
      var dbo = DBObject.newBuilder
      for((name, idx) <- Utilities.getCaseClassParameter[AdGroupAdParent].zipWithIndex) {
        dbo += (Utilities.getMethodName(name) -> agap.productElement(idx))
      }
      dbo.result()
    }

    case class AdGroupCriterionParent(
      var mccObjId: Option[String],
      var customerApiId: Option[Long],
      var campaignApiId: Option[Long],
      var adGroupApiId: Option[Long]
    )

    def dboToAdGroupCriterionParent(dbo: DBObject) = AdGroupCriterionParent(
      mccObjId=dbo.getAsOrElse[Option[String]]("mccObjId", None),
      customerApiId=dbo.getAsOrElse[Option[Long]]("customerApiId", None),
      campaignApiId=dbo.getAsOrElse[Option[Long]]("campaignApiId", None),
      adGroupApiId=dbo.getAsOrElse[Option[Long]]("adGroupApiId", None)
    )

    def adGroupCriterionParentToDbo(agcp: AdGroupCriterionParent) = DBObject(
      "mccObjId" -> agcp.mccObjId.get,
      "customerApiId" -> agcp.customerApiId.get,
      "campaignApiId" -> agcp.campaignApiId.get,
      "adGroupApiId" -> agcp.adGroupApiId.get
    )

    case class CustomParameter(
      key: String,
      value: Option[String]
    )

    def dboToCustomParameter(dbo: DBObject) = CustomParameter(
      key=dbo.getAs[String]("key").get,
      value=dbo.getAs[String]("value")
    )

    def customParameterToDbo(cp: CustomParameter) = DBObject(
      "key" -> cp.key,
      "value" -> cp.value.get
    )

    case class CampaignCriterionParent(
      mccObjId: Option[String],
      customerApiId: Option[Long],
      campaignApiId: Option[Long]
    )

    def campaignCriterionParentToDbo(p: CampaignCriterionParent): DBObject = {
      DBObject(
        "mccObjId" -> p.mccObjId,
        "customerApiId" -> p.customerApiId,
        "campaignApiId" -> p.campaignApiId
      )
    }

    def dboToCampaignCriterionParent(dbo: DBObject): CampaignCriterionParent = {
      CampaignCriterionParent(
        mccObjId = dbo.getAsOrElse[Option[String]]("mccObjId", None),
        customerApiId = dbo.getAsOrElse[Option[Long]]("customerApiId", None),
        campaignApiId = dbo.getAsOrElse[Option[Long]]("campaignApiId", None)
      )
    }
  }
}
