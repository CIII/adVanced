// --- !Ups
db.createCollection("google")
db.createCollection("accounts")
db.createCollection("campaigns")
db.createCollection("ad_groups")
db.createCollection("keywords")

// --- !Downs
db.collection.remove("google")
db.collection.remove("accounts")
db.collection.remove("campaigns")
db.collection.remove("ad_groups")
db.collection.remove("keywords")