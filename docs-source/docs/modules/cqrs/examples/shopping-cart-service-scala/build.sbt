val AkkaVersion = "2.6.8"
val AkkaPersistenceCassandraVersion = "1.0.1"
val AkkaHttpVersion = "10.2.0"
val AkkaProjectionVersion = "0.3"

enablePlugins(AkkaGrpcPlugin)

lazy val `shopping-cart-service-scala` = project
  .in(file("."))
  .settings(
    organization := "com.lightbend.akka.samples",
    version := "1.0",
    scalaVersion := "2.13.3",
    scalacOptions in Compile ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
    //tag::libraryDependencies[]
    libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion,
        "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
        "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion,
        "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
        "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
        "com.typesafe.akka" %% "akka-persistence-cassandra" % AkkaPersistenceCassandraVersion,
        "com.lightbend.akka" %% "akka-projection-eventsourced" % AkkaProjectionVersion,
        "com.lightbend.akka" %% "akka-projection-cassandra" % AkkaProjectionVersion,
        "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
        "com.typesafe.akka" %% "akka-http2-support" % AkkaHttpVersion,
        "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
        "ch.qos.logback" % "logback-classic" % "1.2.3",
        "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
        "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
        "com.lightbend.akka" %% "akka-projection-testkit" % AkkaProjectionVersion % Test,
        "org.scalatest" %% "scalatest" % "3.1.0" % Test),
    //end::libraryDependencies[]
    fork in run := false,
    Global / cancelable := false, // ctrl-c
    mainClass in (Compile, run) := Some("sample.shoppingcart.Main"),
    // disable parallel tests
    parallelExecution in Test := false,
    // show full stack traces and test case durations
    testOptions in Test += Tests.Argument("-oDF"),
    logBuffered in Test := false,
    licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0"))))
