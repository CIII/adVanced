package helpers

import com.google.api.ads.adwords.axis.v201609.cm.{AdGroup, Budget, Campaign}
import com.google.api.ads.adwords.axis.v201609.mcm.Customer
import com.mongodb.casbah.Imports.DBObject
import models.mongodb.google
import models.mongodb.google.Google.googleMccCollection
import models.mongodb.google.Google._

package object google {
  lazy val mccOptions = googleMccCollection.find().toList.map { mccObj =>
    val mcc = dboToMcc(mccObj)
    mcc.name -> mcc._id.toString
  }

  lazy val accountOptions = googleCustomerCollection.find().toList.map { accObj =>
    val acc = dboToGoogleEntity[Customer](accObj, "customer", None)
    acc.getDescriptiveName -> acc.getCustomerId
  }

  lazy val campaignOptions = googleCampaignCollection.find().toList.map{ campaignObj =>
    val campaign = dboToGoogleEntity[Campaign](campaignObj, "campaign", None)
    campaign.getName -> campaign.getId.toString
  }

  lazy val adGroupOptions = googleAdGroupCollection.find().toList.map{ adGroupObj =>
    val adGroup = dboToGoogleEntity[AdGroup](adGroupObj, "adGroup", None)
    adGroup.getName -> adGroup.getId.toString
  }

  lazy val budgetOptions = googleBudgetCollection.find().toList.map { budgetObj =>
    val budget = dboToGoogleEntity[Budget](budgetObj, "budget", None)
    budget.getName -> budget.getBudgetId
  }
}
