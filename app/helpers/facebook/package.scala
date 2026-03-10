package helpers

import org.mongodb.scala.bson.Document
import models.mongodb.facebook.Facebook
import models.mongodb.facebook.Facebook._
import models.mongodb.MongoExtensions._

package object facebook {
  def accountOptions = Facebook.facebookApiAccountCollection.find().toList.map { accObj =>
    val acc = documentToApiAccount(accObj)
    acc.accountId -> acc.accountId
  }

  def campaignOptions = Facebook.facebookCampaignCollection.find().toList.map { campaignObj =>
    val campaign = documentToFacebookEntity(campaignObj, "campaign", None)
    Option(campaign.getString("id")).getOrElse("") -> Option(campaign.getString("name")).getOrElse("")
  }

  def adSetOptions = Facebook.facebookAdSetCollection.find().toList.map { adSetObj =>
    val adSet = documentToFacebookEntity(adSetObj, "adSet", None)
    Option(adSet.getString("id")).getOrElse("") -> Option(adSet.getString("name")).getOrElse("")
  }

  val buyingTypeOptions = Seq(
    "AUCTION" -> "Auction",
    "RESERVED" -> "Reserved"
  )

  val statusOptions = Seq(
    "ACTIVE" -> "Active",
    "PAUSED" -> "Paused",
    "DELETED" -> "Deleted",
    "ARCHIVED" -> "Archived"
  )
}
