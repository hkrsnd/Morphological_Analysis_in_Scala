name := "Morphologial_Analisys_in_Scala"

organization := "org.pii5656"

scalaVersion := "2.11.7"

resolvers += "kuromoji repo" at "http://www.atilika.org/nexus/content/repositories/atilika"

libraryDependencies ++= Seq(
  "org.atilika.kuromoji" % "kuromoji" % "0.7.7",
  "org.twitter4j" % "twitter4j-core" % "4.0.2",
  "com.typesafe.slick" %% "slick" % "3.0.0",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.typesafe" % "config" % "1.3.0",
  "com.zaxxer" % "HikariCP-java6" % "2.3.3"
)
