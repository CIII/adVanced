package helpers

import org.mongodb.scala.bson.Document

/**
 * Google Ads helper utilities.
 *
 * TODO: Re-implement with Google Ads API v18 when migration is complete.
 * The old AdWords API types (Customer, Campaign, AdGroup, Budget) are no longer available.
 * These lazy vals should be replaced with injectable services that query the MongoDB
 * collections and return Document-based representations.
 */
package object google {
  // TODO: Re-implement with async MongoDB queries via MongoService
  // These lazy vals used the old synchronous Casbah driver and AdWords API types.
  // They should become methods on an injectable GoogleDataService class.
}
