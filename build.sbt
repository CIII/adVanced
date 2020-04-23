name := """adVanced"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

routesGenerator := InjectedRoutesGenerator

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += "tapquality" at "http://leadpath.staging.easiersolar.com"

libraryDependencies ++= Seq(
  cache,
  ws,
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0",
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.2.0",
  "org.scalatest" % "scalatest_2.11" % "3.0.1",
  "org.squeryl" % "squeryl_2.11" % "0.9.6-RC4",
  "org.mongodb" %% "casbah" % "3.1.1",
  "mysql" % "mysql-connector-java" % "5.1.10",
  "com.github.etaty" %% "rediscala" % "1.6.0",
  "com.google.code.gson" % "gson" % "2.5",
  "org.json" % "json" % "20140107",
  "org.scala-lang.modules" %% "scala-async" % "0.9.2",
  "be.objectify" % "deadbolt-scala_2.11" % "2.5.0",
  "com.google.api-ads" % "ads-lib" % "2.21.0",
  "com.google.api-ads" % "adwords-axis" % "2.21.0",
  "com.beachape" %% "enumeratum" % "1.3.2",
  "com.beachape" %% "enumeratum-play" % "1.3.2",
  "com.microsoft.bingads" % "microsoft.bingads" % "11.5.3",
  "com.github.marklister" % "product-collections_2.11" % "1.4.2",
  "org.reactivemongo" %% "reactivemongo" % "0.12.0",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.12.0",
  "com.facebook.ads.sdk" % "facebook-java-ads-sdk" % "2.10.0",
  "com.google.apis" % "google-api-services-oauth2" % "v2-rev125-1.22.0",
  "com.google.api-client" % "google-api-client-java6" % "1.22.0",
  "com.google.oauth-client" % "google-oauth-client-jetty" % "1.22.0",
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.tapquality" % "shared-libs" % "1.0-SNAPSHOT",
  "org.mindrot" % "jbcrypt" % "0.3m"
)