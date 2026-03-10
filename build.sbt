name := "adVanced"

version := "1.0-SNAPSHOT"

scalaVersion := "2.13.16"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  guice,
  caffeine,
  ws,

  // Play Slick (MySQL)
  "org.playframework" %% "play-slick" % "6.1.1",
  "org.playframework" %% "play-slick-evolutions" % "6.1.1",

  // Joda-Time / Slick mapper
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.9.1",

  // Testing
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,

  // MongoDB (replaces casbah + reactivemongo)
  "org.mongodb.scala" %% "mongo-scala-driver" % "5.3.1",

  // MySQL connector
  "com.mysql" % "mysql-connector-j" % "8.4.0",

  // Redis (replaces rediscala)
  "io.lettuce" % "lettuce-core" % "6.5.3.RELEASE",

  // Deadbolt authorization
  "be.objectify" %% "deadbolt-scala" % "3.0.0",

  // Google Ads API (replaces adwords-axis)
  "com.google.api-ads" % "google-ads" % "34.0.0",

  // Enumeratum
  "com.beachape" %% "enumeratum" % "1.7.5",
  "com.beachape" %% "enumeratum-play" % "1.8.2",

  // Bing Ads
  "com.microsoft.bingads" % "microsoft.bingads" % "13.0.22.1",

  // Facebook Business SDK (replaces facebook-java-ads-sdk)
  "com.facebook.business.sdk" % "facebook-java-business-sdk" % "24.0.1",

  // Google OAuth2 / API Client
  "com.google.api-client" % "google-api-client" % "2.7.2",
  "com.google.apis" % "google-api-services-oauth2" % "v2-rev20200213-2.0.0",

  // Joda-Time
  "joda-time" % "joda-time" % "2.13.1",
  "org.joda" % "joda-convert" % "3.0.1",

  // BCrypt
  "org.mindrot" % "jbcrypt" % "0.4",

  // CSV parsing
  "com.opencsv" % "opencsv" % "5.9",
)
