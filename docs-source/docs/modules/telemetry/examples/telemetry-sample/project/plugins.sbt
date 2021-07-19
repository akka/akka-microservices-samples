// TODO: replace with 2.16.0 once it's released
// tag::telemetry-sbt-plugin[]
// The Cinnamon Telemetry plugin
addSbtPlugin("com.lightbend.cinnamon" % "sbt-cinnamon" % "2.16.0")
// end::telemetry-sbt-plugin[]

// tag::telemetry-javaagent-docker[]
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.8.1")
// end::telemetry-javaagent-docker[]

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")