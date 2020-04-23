var conn, db;

conn = new Mongo();
db = conn.getDB('advanced');

/**
Index Analysis:
https://docs.google.com/spreadsheets/u/1/d/1xsJlSOfCBBQ1XKwxoBTQaA-FSs7TjfFHKHGcwWs-k8g/edit#gid=0

According to the spreadsheet above, the most efficient way to index (in terms of both speed and the size of indexes),
we should create an individual index for each of the "dimensions" that are displayed in the attribution charts.  These
are non-numerical columns which define the roll-up and number of unique rows in the result.

Additionally, the most ideal scenario would be to have a compound index per query containing all visible dimensions
and measures.  Since this is unrealistic due to the number of indexes, we picked out the combinations of dimensions &
measures that are most likely to be very commonly displayed and indexed those.

**/

// ********************** Individual Indexes ************************ //

// Campaign
// Segments
db.google_campaign_performance_report.ensureIndex({ "dayOfWeek": -1});
db.google_campaign_performance_report.ensureIndex({ "network": -1});
db.google_campaign_performance_report.ensureIndex({ "device": -1});
db.google_campaign_performance_report.ensureIndex({ "date": -1});
db.google_campaign_performance_report.ensureIndex({ "campaignId": -1});
db.google_campaign_performance_report.ensureIndex({ "entityId": -1});
// Dimensions
db.google_campaign_performance_report.ensureIndex({ "accountName": -1});
db.google_campaign_performance_report.ensureIndex({ "campaign": -1});
db.google_campaign_performance_report.ensureIndex({ "campaignState": -1});

// Ad Group
// Segments
db.google_adgroup_performance_report.ensureIndex({ "dayOfWeek": -1});
db.google_adgroup_performance_report.ensureIndex({ "network": -1});
db.google_adgroup_performance_report.ensureIndex({ "device": -1});
db.google_adgroup_performance_report.ensureIndex({ "date": -1});
db.google_adgroup_performance_report.ensureIndex({ "adGroupId": -1});
db.google_adgroup_performance_report.ensureIndex({ "entityId": -1});
// Dimensions
db.google_adgroup_performance_report.ensureIndex({ "campaignId": -1});
db.google_adgroup_performance_report.ensureIndex({ "accountName": -1});
db.google_adgroup_performance_report.ensureIndex({ "campaign": -1});
db.google_adgroup_performance_report.ensureIndex({ "campaignState": -1});
db.google_adgroup_performance_report.ensureIndex({ "adGroupName": -1});
db.google_adgroup_performance_report.ensureIndex({ "adGroupState": -1});

// Ad
// Segments
db.google_ad_performance_report.ensureIndex({ "dayOfWeek": -1});
db.google_ad_performance_report.ensureIndex({ "network": -1});
db.google_ad_performance_report.ensureIndex({ "date": -1});
db.google_ad_performance_report.ensureIndex({ "device": -1});
db.google_ad_performance_report.ensureIndex({ "adId": -1});
db.google_ad_performance_report.ensureIndex({ "entityId": -1});

// Dimensions
db.google_ad_performance_report.ensureIndex({ "accountName": -1});
db.google_ad_performance_report.ensureIndex({ "campaignId": -1});
db.google_ad_performance_report.ensureIndex({ "campaign": -1});
db.google_ad_performance_report.ensureIndex({ "campaignState": -1});
db.google_ad_performance_report.ensureIndex({ "adGroupName": -1});
db.google_ad_performance_report.ensureIndex({ "adGroupId": -1});
db.google_ad_performance_report.ensureIndex({ "adGroupState": -1});
db.google_ad_performance_report.ensureIndex({ "ad": -1});
db.google_ad_performance_report.ensureIndex({ "adState": -1});
db.google_ad_performance_report.ensureIndex({ "adTypeField": -1});
db.google_ad_performance_report.ensureIndex({ "adDescription": -1});
db.google_ad_performance_report.ensureIndex({ "adDescriptionLine1": -1});
db.google_ad_performance_report.ensureIndex({ "headline1": -1});
db.google_ad_performance_report.ensureIndex({ "headline2": -1});
db.google_ad_performance_report.ensureIndex({ "longHeadline": -1});
db.google_ad_performance_report.ensureIndex({ "imageAdName": -1});
db.google_ad_performance_report.ensureIndex({ "imageWidth": -1});
db.google_ad_performance_report.ensureIndex({ "imageHeight": -1});
db.google_ad_performance_report.ensureIndex({ "path1": -1});
db.google_ad_performance_report.ensureIndex({ "path2": -1});

// Geo
// Segments
db.google_geo_performance_report.ensureIndex({ "dayOfWeek": -1});
db.google_geo_performance_report.ensureIndex({ "network": -1});
db.google_geo_performance_report.ensureIndex({ "device": -1});
db.google_geo_performance_report.ensureIndex({ "entityId": -1});
db.google_geo_performance_report.ensureIndex({ "date": -1});
db.google_geo_performance_report.ensureIndex({ "adGroupName": -1});
db.google_geo_performance_report.ensureIndex({ "adGroupId": -1});
db.google_geo_performance_report.ensureIndex({ "adGroupState": -1});

// Dimensions
db.google_geo_performance_report.ensureIndex({ "accountName": -1});
db.google_geo_performance_report.ensureIndex({ "campaignId": -1});
db.google_geo_performance_report.ensureIndex({ "campaign": -1});
db.google_geo_performance_report.ensureIndex({ "campaignState": -1});
db.google_geo_performance_report.ensureIndex({ "region": -1});
db.google_geo_performance_report.ensureIndex({ "city": -1});
db.google_geo_performance_report.ensureIndex({ "mostSpecificLocation": -1});
db.google_geo_performance_report.ensureIndex({ "targetable": -1});
db.google_geo_performance_report.ensureIndex({ "clientName": -1});
db.google_geo_performance_report.ensureIndex({ "currency": -1});
db.google_geo_performance_report.ensureIndex({ "countryTerritory": -1});
db.google_geo_performance_report.ensureIndex({ "metroArea": -1});
db.google_geo_performance_report.ensureIndex({ "customerId": -1});

// ********************** Compound Indexes ************************ //

// Default pages
db.google_campaign_performance_report.ensureIndex({ "campaignId": -1, "clicks": -1, "cost": -1, "revenue": -1, "conversions": -1});
db.google_adgroup_performance_report.ensureIndex({ "campaignId": -1, "clicks": -1, "cost": -1, "revenue": -1, "conversions": -1});
db.google_ad_performance_report.ensureIndex({ "campaignId": -1, "clicks": -1, "cost": -1, "revenue": -1, "conversions": -1});
db.google_geo_performance_report.ensureIndex({ "campaignId": -1, "clicks": -1, "cost": -1, "revenue": -1, "conversions": -1});

// ********************** Unique Indexes ************************** //
db.google_campaign_performance_report.ensureIndex({ "dayOfWeek": -1, "network": -1, "device": -1, "date": -1, "campaignId": -1, "entityId": -1}, {name: "g_campaign_comp_unique", unique: true, dropDups: true});
db.google_adgroup_performance_report.ensureIndex({ "dayOfWeek": -1, "network": -1, "device": -1, "date": -1, "adGroupId": -1, "entityId": -1}, {name: "g_adgroup_comp_unique", unique: true, dropDups: true});
db.google_ad_performance_report.ensureIndex({ "dayOfWeek": -1, "network": -1, "device": -1, "date": -1, "adId": -1, "entityId": -1}, {name: "g_ad_comp_unique", unique: true, dropDups: true});
db.google_geo_performance_report.ensureIndex({ "dayOfWeek": -1, "network": -1, "device": -1, "date": -1, "campaignId": -1, "entityId": -1, "adgroupId": -1, "region": -1, "city": -1, "mostSpecificLocation": -1, "targetable": -1, "clientName": -1, "currency": -1, "countryTerritory": -1, "metroArea": -1, "customerId": -1}, {name: "g_geo_comp_unique", unique: true, dropDups: true});

// ********************** TQ Reporting **************************** //
db.tq_reporting_arrival_facts.ensureIndex({ "created_at": -1});
db.tq_reporting_arrival_facts.ensureIndex({ "revenue": -1});
db.tq_reporting_arrival_facts.ensureIndex({ "conf": -1});
db.tq_reporting_arrival_facts.ensureIndex({ "browser": -1});
db.tq_reporting_arrival_facts.ensureIndex({ "day_of_week": -1});
db.tq_reporting_arrival_facts.ensureIndex({ "last_activity": -1});
db.tq_reporting_arrival_facts.ensureIndex({ "user_agent": -1});
db.tq_reporting_arrival_facts.ensureIndex({ "utm_source": -1});
db.tq_reporting_arrival_facts.ensureIndex({ "utm_campaign": -1});
db.tq_reporting_arrival_facts.ensureIndex({ "utm_medium": -1});
db.tq_reporting_arrival_facts.ensureIndex({ "keyword": -1});
db.tq_reporting_arrival_facts.ensureIndex({ "g_network": -1});
db.tq_reporting_arrival_facts.ensureIndex({ "g_device": -1});
db.tq_reporting_arrival_facts.ensureIndex({ "g_location": -1});
db.tq_reporting_arrival_facts.ensureIndex({ "session_id": -1 }, { unique: true });
db.tq_reporting_arrival_facts.ensureIndex({ "created_at": -1, "revenue": -1, "conf": -1 });

// Close connection
conn.close();