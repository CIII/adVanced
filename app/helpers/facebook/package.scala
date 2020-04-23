package helpers

import com.facebook.ads.sdk.{AdSet, Campaign}
import com.mongodb.casbah.Imports.DBObject
import models.mongodb.facebook.Facebook._

package object facebook {
  lazy val accountOptions = facebookApiAccountCollection.find().toList.map { accObj =>
    val acc = DBObjectToFacebookApiAccount(accObj)
    acc.accountId -> acc.accountId
  }

  lazy val campaignOptions = facebookCampaignCollection.find().toList.map { campaignObj =>
    val campaign = dboToFacebookEntity[Campaign](campaignObj, "campaign", None)
    campaign.getId -> campaign.getFieldName
  }

  lazy val adSetOptions = facebookAdSetCollection.find().toList.map { adSetObj =>
    val adSet = dboToFacebookEntity[Campaign](adSetObj, "adSet", None)
    adSet.getId -> adSet.getFieldName
  }

  lazy val buyingTypeOptions = Seq(
    "AUCTION" -> "Auction",
    "RESERVED" -> "Reserved"
  )

  lazy val statusOptions = Seq(
    "ACTIVE" -> "Active",
    "PAUSED" -> "Paused",
    "DELETED" -> "Deleted",
    "ARCHIVED" -> "Archived"
  )
}
