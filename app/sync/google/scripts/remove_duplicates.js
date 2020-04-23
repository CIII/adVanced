var conn, db;

conn = new Mongo();
db = conn.getDB('advanced');

var collections = [
    "google_campaign_performance_report",
    "google_adgroup_performance_report",
    "google_ad_performance_report",
    "google_geo_performance_report"
];

for(i = 0; i < collections.length; i++){
    print('processing collection: ' + collections[i]);
    var cursor = db.getCollection(collections[i]).aggregate([{
        $group: {
            "_id": {
                "dayOfWeek": "$dayOfWeek",
                "network": "$network",
                "device": "$device",
                "date": "$date",
                "entityId": "$entityId"
            },
            uniqueIds: {
                $addToSet: "$_id"
            },
            count: {
                $sum: 1
            }
        }
    }, {
        $match: {
            count: {
                $gt: 1
            }
        }
    }]);

    while(cursor.hasNext()){
        var doc = cursor.next();
        var index = 1;
        while(index < doc.uniqueIds.length){
            db.getCollection(collections[i]).remove(doc.uniqueIds[index]);
            index = index + 1;
        }
    }    
}

conn.close();
