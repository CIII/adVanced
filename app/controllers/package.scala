import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import models.mongodb.Utilities
import models.mongodb.MongoExtensions._
import play.api.mvc.{AnyContent, Request}

import scala.collection.immutable.List

package object controllers {

  // TODO: Migrate json() to use async MongoDB driver (returns Future)
  // This function queries MongoDB synchronously and needs to be refactored
  // to return Future[String] once callers are updated.
  def json(request: Request[AnyContent], fields: List[String], entityName: String, collectionName: String, addl_qry_opt: Option[(String, Any)] = None): String = {
    // Placeholder - needs MongoService injection to work
    "{}"
  }

  object Google {
    case class AdGroupAdParent(
      mccObjId: Option[String],
      customerApiId: Option[Long],
      campaignApiId: Option[Long],
      adGroupApiId: Option[Long]
    )

    abstract class AdGroupAdForm {
      def parent: AdGroupAdParent
      def apiId: Option[Long]
      def status: Option[String]
    }

    def documentToAdGroupAdParent(doc: Document): AdGroupAdParent = AdGroupAdParent(
      mccObjId = Option(doc.getString("mccObjId")),
      customerApiId = Option(doc.getLong("customerApiId")).map(_.toLong),
      campaignApiId = Option(doc.getLong("campaignApiId")).map(_.toLong),
      adGroupApiId = Option(doc.getLong("adGroupApiId")).map(_.toLong)
    )

    def adGroupAdParentToDocument(agap: AdGroupAdParent): Document = Document(
      "mccObjId" -> agap.mccObjId,
      "customerApiId" -> agap.customerApiId,
      "campaignApiId" -> agap.campaignApiId,
      "adGroupApiId" -> agap.adGroupApiId
    )

    case class AdGroupCriterionParent(
      mccObjId: Option[String],
      customerApiId: Option[Long],
      campaignApiId: Option[Long],
      adGroupApiId: Option[Long]
    )

    def documentToAdGroupCriterionParent(doc: Document): AdGroupCriterionParent = AdGroupCriterionParent(
      mccObjId = Option(doc.getString("mccObjId")),
      customerApiId = Option(doc.getLong("customerApiId")).map(_.toLong),
      campaignApiId = Option(doc.getLong("campaignApiId")).map(_.toLong),
      adGroupApiId = Option(doc.getLong("adGroupApiId")).map(_.toLong)
    )

    def adGroupCriterionParentToDocument(agcp: AdGroupCriterionParent): Document = Document(
      "mccObjId" -> agcp.mccObjId.getOrElse(""),
      "customerApiId" -> agcp.customerApiId.getOrElse(0L),
      "campaignApiId" -> agcp.campaignApiId.getOrElse(0L),
      "adGroupApiId" -> agcp.adGroupApiId.getOrElse(0L)
    )

    case class CustomParameter(
      key: String,
      value: Option[String]
    )

    def documentToCustomParameter(doc: Document): CustomParameter = CustomParameter(
      key = doc.getString("key"),
      value = Option(doc.getString("value"))
    )

    def customParameterToDocument(cp: CustomParameter): Document = Document(
      "key" -> cp.key,
      "value" -> cp.value.getOrElse("")
    )

    case class CampaignCriterionParent(
      mccObjId: Option[String],
      customerApiId: Option[Long],
      campaignApiId: Option[Long]
    )

    def campaignCriterionParentToDocument(p: CampaignCriterionParent): Document = Document(
      "mccObjId" -> p.mccObjId,
      "customerApiId" -> p.customerApiId,
      "campaignApiId" -> p.campaignApiId
    )

    def documentToCampaignCriterionParent(doc: Document): CampaignCriterionParent = CampaignCriterionParent(
      mccObjId = Option(doc.getString("mccObjId")),
      customerApiId = Option(doc.getLong("customerApiId")).map(_.toLong),
      campaignApiId = Option(doc.getLong("campaignApiId")).map(_.toLong)
    )
  }
}
