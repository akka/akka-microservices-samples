import sbt._

object Version {
  // FIXME once akka 2.6.9 is released
  val AkkaVersion = "2.6.8+71-57fb9e90"
  val AkkaPersistenceCassandraVersion = "1.0.1"
  val AlpakkaKafkaVersion = "2.0.4"
  val AkkaHttpVersion = "10.2.0"
  // FIXME once akka management 1.0.9 is released
  val AkkaManagementVersion = "1.0.8+35-9feaa689+20200825-1429"
  val AkkaProjectionVersion = "1.0.0-RC1"
  val logbackVersion = "1.2.3"
  val scalaTestVersion = "3.1.2"
  val scalaVersion = "2.13.3"
}

object Dependencies {
  //tag::libraryDependencies[]
  val dependencies = Seq(
    "com.typesafe.akka" %% "akka-cluster-sharding-typed" % Version.AkkaVersion,
    "com.typesafe.akka" %% "akka-stream" % Version.AkkaVersion,
    "com.typesafe.akka" %% "akka-persistence-cassandra" % Version.AkkaPersistenceCassandraVersion,
    "com.lightbend.akka" %% "akka-projection-eventsourced" % Version.AkkaProjectionVersion,
    "com.lightbend.akka" %% "akka-projection-cassandra" % Version.AkkaProjectionVersion,
    "com.typesafe.akka" %% "akka-http" % Version.AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http2-support" % Version.AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % Version.AkkaHttpVersion,
    "com.lightbend.akka.management" %% "akka-management" % Version.AkkaManagementVersion,
    "com.lightbend.akka.management" %% "akka-management-cluster-http" % Version.AkkaManagementVersion,
    "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % Version.AkkaManagementVersion,
    "com.typesafe.akka" %% "akka-persistence-typed" % Version.AkkaVersion,
    "com.typesafe.akka" %% "akka-persistence-query" % Version.AkkaVersion,
    "com.typesafe.akka" %% "akka-serialization-jackson" % Version.AkkaVersion,
    "com.typesafe.akka" %% "akka-discovery" % Version.AkkaVersion,
    "com.typesafe.akka" %% "akka-stream-kafka" % Version.AlpakkaKafkaVersion,
    // Logging
    "com.typesafe.akka" %% "akka-slf4j" % Version.AkkaVersion,
    "ch.qos.logback" % "logback-classic" % Version.logbackVersion,
    // Test dependencies
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % Version.AkkaVersion % Test,
    "com.typesafe.akka" %% "akka-persistence-testkit" % Version.AkkaVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % Version.AkkaVersion % Test,
    "com.lightbend.akka" %% "akka-projection-testkit" % Version.AkkaProjectionVersion % Test,
    "org.scalatest" %% "scalatest" % Version.scalaTestVersion % Test
  )
  //end::libraryDependencies[]

}

