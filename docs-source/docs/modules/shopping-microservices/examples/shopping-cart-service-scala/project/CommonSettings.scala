import sbt.Keys._
import sbt._

object CommonSettings {
  lazy val commonSettings = Seq(
    Compile / scalacOptions ++= CompileOptions.scalaCompileOptions,
    Compile / javacOptions ++= CompileOptions.javaCompileOptions,

    licenses := Seq(("CC0", url("https://creativecommons.org/publicdomain/zero/1.0"))),

    // For akka management snapshot
    resolvers += Resolver.bintrayRepo("akka", "snapshots"),
    // For akka nightlies
    resolvers += "Akka Snapshots" at "https://repo.akka.io/snapshots/",

    Test / parallelExecution := false,
    Test / testOptions += Tests.Argument("-oDF"),
    Test / logBuffered := false,

    run / fork := false,

    libraryDependencies ++= Dependencies.dependencies
  )

  lazy val configure: Project => Project = (project: Project) => {
    project
      .settings(CommonSettings.commonSettings: _*)
  }
}
