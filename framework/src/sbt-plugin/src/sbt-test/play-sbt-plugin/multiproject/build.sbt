//
// Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
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
  scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.11.7")
)

TaskKey[Unit]("checkPlayMonitoredFiles") := {
  val files: Seq[File] = PlayKeys.playMonitoredFiles.value
  val sorted = files.map(_.toPath).sorted.map(_.toFile)
  val base = baseDirectory.value
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
    throw new RuntimeException("Expected " + expected + " but got " + sorted)
  }
}

TaskKey[Unit]("checkPlayCompileEverything") := {
  val analyses: Seq[sbt.inc.Analysis] = play.sbt.PlayInternalKeys.playCompileEverything.value
  if (analyses.size != 4) {
    throw new RuntimeException("Expected 4 analysis objects, but got " + analyses.size)
  }
  val base = baseDirectory.value
  val expectedSourceFiles = Seq(
    base / "app" / "Root.scala",
    base / "nonplaymodule" / "src" / "main" / "scala" / "NonPlayModule.scala",
    base / "playmodule" / "app" / "PlayModule.scala",
    base / "transitive" / "app" / "Transitive.scala"
  )
  val allSources = analyses.flatMap(_.relations.allSources).map(_.toPath).sorted.map(_.toFile)
  if (expectedSourceFiles != allSources) {
    println("Expected compiled sources to be:")
    expectedSourceFiles.foreach(println)
    println()
    println("but got:")
    allSources.foreach(println)
    throw new RuntimeException("Expected " + expectedSourceFiles + " but got " + allSources)
  }
}
