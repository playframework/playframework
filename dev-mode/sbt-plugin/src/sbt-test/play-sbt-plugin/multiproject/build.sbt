//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .dependsOn(playmodule, nonplaymodule)
  .settings(common: _*)

lazy val playmodule = (project in file("playmodule"))
  .enablePlugins(PlayScala)
  .dependsOn(transitive)
  .settings(common: _*)

// A transitive dependency of playmodule, to check that we are pulling in transitive deps
lazy val transitive = (project in file("transitive"))
  .enablePlugins(PlayScala)
  .settings(common: _*)

// A non play module, to check that play settings that are not defined don't cause errors
// and are still included in compilation
lazy val nonplaymodule = (project in file("nonplaymodule"))
  .settings(common: _*)

def common: Seq[Setting[_]] = Seq(
  scalaVersion := sys.props("scala.version"),
  updateOptions := updateOptions.value.withLatestSnapshots(false),
  evictionWarningOptions in update ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
  libraryDependencies += guice
)

TaskKey[Unit]("checkPlayMonitoredFiles") := {
  val files: Seq[File] = PlayKeys.playMonitoredFiles.value.distinct
  val sorted           = files.map(_.toPath).sorted.map(_.toFile)
  val base             = baseDirectory.value
  // Expect all source, resource, assets, public directories that exist
  val expected = Seq(
    base / "app",
    base / "nonplaymodule" / "src" / "main" / "resources",
    base / "nonplaymodule" / "src" / "main" / "scala",
    base / "playmodule" / "app",
    base / "public",
    base / "transitive" / "app"
  )
  if (sorted != expected) {
    println("Expected play monitored directories to be:")
    expected.foreach(println)
    println()
    println("but got:")
    sorted.foreach(println)
    sys.error(s"Expected $expected but got $sorted")
  }
}

TaskKey[Unit]("checkPlayCompileEverything") := {
  val analyses = play.sbt.PlayInternalKeys.playCompileEverything.value
  if (analyses.size != 4) {
    sys.error(s"Expected 4 analysis objects, but got ${analyses.size}")
  }
  val base = baseDirectory.value
  val expectedSourceFiles = Seq(
    base / "app" / "Root.scala",
    base / "nonplaymodule" / "src" / "main" / "scala" / "NonPlayModule.scala",
    base / "playmodule" / "app" / "PlayModule.scala",
    base / "transitive" / "app" / "Transitive.scala"
  )
  val allSources = analyses.flatMap(_.relations.allSources).map(_ match {
    case JFile(file) => // sbt < 1.4
      file.toPath
    case VirtualFile(vf) => // sbt 1.4+ virtual file
      val names = vf.getClass.getMethod("names").invoke(vf).asInstanceOf[Array[String]]
      val path =
        if (names.head.startsWith("${")) { // check for ${BASE} or similar (in case it changes)
          // It's an relative path, skip the first element (which usually is "${BASE}")
          java.nio.file.Paths.get(names.drop(1).head, names.drop(2): _*).toAbsolutePath
        } else {
          // It's an absolute path, sbt uses them e.g. for subprojects located outside of the base project
          val id = vf.getClass.getMethod("id").invoke(vf).asInstanceOf[String]
          // In Windows the sbt virtual file id does not start with a slash, but absolute paths in Java URIs need that
          val extraSlash = if (id.startsWith("/")) "" else "/"
          val prefix     = "file://" + extraSlash
          // The URI will be like file:///home/user/project/SomeClass.scala (Linux/Mac) or file:///C:/Users/user/project/SomeClass.scala (Windows)
          java.nio.file.Paths.get(java.net.URI.create(s"$prefix$id")).toAbsolutePath
        }
      path
    case other =>
      sys.error(s"Don't know how to handle class ${other.getClass.getName}")
  }).sorted.map(_.toFile)
  if (expectedSourceFiles != allSources) {
    println("Expected compiled sources to be:")
    expectedSourceFiles.foreach(println)
    println()
    println("but got:")
    allSources.foreach(println)
    sys.error(s"Expected $expectedSourceFiles but got $allSources")
  }
}
