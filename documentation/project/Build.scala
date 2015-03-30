/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import sbt.Keys._
import play.Play.autoImport._
import play.core.PlayVersion
import scala.util.Properties.isJavaAtLeast
import com.typesafe.play.docs.sbtplugin._
import com.typesafe.play.docs.sbtplugin.Imports._

object ApplicationBuild extends Build {

  /*
   * Circular dependency support.
   *
   * We want to build docs with the main build, including in pull requests, etc.  Hence, we can't have any circular
   * dependencies where the documentation code snippets depend on code external to Play, that itself depends on Play,
   * in case a new change in Play breaks the external code.
   *
   * To address this, we have multiple modes that this build can run in, controlled by an external.modules system
   * property.
   *
   * If this property is not set, or is none, we only compile/test the code snippets that doesn't depend on external
   * modules.
   *
   * If it is all, we compile/test all code snippets.
   *
   * If it is a comma separated list of projects, we compile/test that comma separated list of projects.
   *
   * To add a new project, let's call it foo, add a new entry to the map below with that projects dependencies keyed
   * with "foo".  Then place all the code snippets that use that external module in a directory called "code-foo".
   */

  val externalPlayModules: Map[String, Seq[Setting[_]]] = Map(
    "scalatestplus-play" -> Seq(
      resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases", // TODO: Delete this eventually, just needed for lag between deploying to sonatype and getting on maven central
      libraryDependencies += "org.scalatestplus" %% "play" % "1.1.0-RC1" % "test" exclude("com.typesafe.play", "play-test_2.10")
    )
  )

  val enabledExternalPlayModules = Option(System.getProperty("external.modules"))

  val (externalPlayModuleSettings, codeFilter): (Seq[Setting[_]], FileFilter) = enabledExternalPlayModules match {
    case None | Some("none") => (Nil, new ExactFilter("code"))
    case Some("all") => (externalPlayModules.toSeq.flatMap(_._2),
      new ExactFilter("code") || FileFilter.globFilter("code-*"))
    case Some(explicit) =>
      val enabled = explicit.split(",")
      (enabled.flatMap(e => externalPlayModules.get(e).getOrElse(Nil)),
        enabled.foldLeft[FileFilter](new ExactFilter("code")) { (filter, e) => filter || new ExactFilter("code-" + e) })
  }

  lazy val main = Project("Play-Documentation", file(".")).enablePlugins(PlayDocsPlugin).settings(
    resolvers += Resolver.sonatypeRepo("releases"), // TODO: Delete this eventually, just needed for lag between deploying to sonatype and getting on maven central
    version := PlayVersion.current,
    libraryDependencies ++= Seq(
      "org.mockito" % "mockito-core" % "1.9.5" % "test"
    ),

    PlayDocsKeys.docsJarFile := Some((packageBin in (playDocs, Compile)).value),

    PlayDocsKeys.javaManualSourceDirectories := (baseDirectory.value / "manual" / "working" / "javaGuide" ** codeFilter).get,
    PlayDocsKeys.scalaManualSourceDirectories := (baseDirectory.value / "manual" / "working" / "scalaGuide" ** codeFilter).get,

    unmanagedSourceDirectories in Test ++= (baseDirectory.value / "manual" / "detailedTopics" ** codeFilter).get,
    unmanagedResourceDirectories in Test ++= (baseDirectory.value / "manual" / "detailedTopics" ** codeFilter).get,

    crossScalaVersions := Seq("2.10.4", "2.11.5"),
    scalaVersion := PlayVersion.scalaVersion,

    fork in Test := true
  ).settings(externalPlayModuleSettings:_*)
   .dependsOn(
      playDocs,
      playProject("Play") % "test",
      playProject("Play-Specs2") % "test",
      playProject("Play-Java") % "test",
      playProject("Play-Cache") % "test",
      playProject("Play-Java-WS") % "test",
      playProject("Filters-Helpers") % "test",
      playProject("Play-JDBC") % "test",
      playProject("Play-Java-JDBC") % "test"
  )

  lazy val playDocs = playProject("Play-Docs")

  def playProject(name: String) = ProjectRef(Path.fileProperty("user.dir").getParentFile / "framework", name)
}
