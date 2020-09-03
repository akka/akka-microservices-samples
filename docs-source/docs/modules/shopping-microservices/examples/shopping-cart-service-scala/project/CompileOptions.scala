object CompileOptions {

  val scalaCompileOptions = Seq(
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xlog-reflective-calls",
    "-Xlint"
  )

  val javaCompileOptions = Seq(
    "-Xlint:unchecked",
    "-Xlint:deprecation"
  )
}
