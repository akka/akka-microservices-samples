// FIXME once akka 2.6.9 is released
val AkkaVersion = "2.6.8+71-57fb9e90"
val AkkaPersistenceCassandraVersion = "1.0.1"
val AlpakkaKafkaVersion = "2.0.5"
val AkkaHttpVersion = "10.2.0"
// FIXME once akka management 1.0.9 is released
val AkkaManagementVersion = "1.0.8+35-9feaa689+20200825-1429"
val AkkaProjectionVersion = "1.0.0-RC3"

enablePlugins(AkkaGrpcPlugin)

name := "shopping-cart-service-scala"
version := "1.0"

organization := "com.lightbend.akka.samples"
organizationHomepage := Some(url("https://akka.io"))
licenses := Seq(("CC0", url("https://creativecommons.org/publicdomain/zero/1.0")))

// For akka management snapshot
resolvers += Resolver.bintrayRepo("akka", "snapshots")
// For akka nightlies
resolvers += "Akka Snapshots".at("https://repo.akka.io/snapshots/")

Compile / scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint")
Compile / javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

Test / parallelExecution := false
Test / testOptions += Tests.Argument("-oDF")
Test / logBuffered := false

scalaVersion := "2.13.3"
//tag::libraryDependencies[]
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-cassandra" % AkkaPersistenceCassandraVersion,
  "com.lightbend.akka" %% "akka-projection-eventsourced" % AkkaProjectionVersion,
  "com.lightbend.akka" %% "akka-projection-cassandra" % AkkaProjectionVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http2-support" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
  "com.lightbend.akka.management" %% "akka-management" % AkkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
  "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % AlpakkaKafkaVersion,
  // Logging
  "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  // Test dependencies
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
  "com.lightbend.akka" %% "akka-projection-testkit" % AkkaProjectionVersion % Test,
  "org.scalatest" %% "scalatest" % "3.1.2" % Test)
//end::libraryDependencies[]

run / fork := false
Global / cancelable := false // ctrl-c
