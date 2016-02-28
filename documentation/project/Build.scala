/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */

import com.typesafe.play.sbt.enhancer.PlayEnhancer
import sbt._
import sbt.Keys._
import play.sbt.Play.autoImport._
import play.core.PlayVersion
import scala.util.Properties.isJavaAtLeast
import com.typesafe.play.docs.sbtplugin._
import com.typesafe.play.docs.sbtplugin.Imports._

object ApplicationBuild extends Build {

  lazy val main = Project("Play-Documentation", file("."))
    .enablePlugins(PlayDocsPlugin).disablePlugins(PlayEnhancer).settings(
    resolvers += Resolver.sonatypeRepo("releases"), // TODO: Delete this eventually, just needed for lag between deploying to sonatype and getting on maven central
    version := PlayVersion.current,
    libraryDependencies ++= Seq(
      "org.mockito" % "mockito-core" % "1.9.5" % "test"
    ),

    PlayDocsKeys.docsJarFile := Some((packageBin in (playDocs, Compile)).value),
    PlayDocsKeys.playDocsValidationConfig := PlayDocsValidation.ValidationConfig(downstreamWikiPages = Set(
      "ScalaAnorm",
      "PlaySlickMigrationGuide",
      "ScalaTestingWithScalaTest",
      "ScalaFunctionalTestingWithScalaTest"
    )),

    PlayDocsKeys.javaManualSourceDirectories := (baseDirectory.value / "manual" / "working" / "javaGuide" ** "code").get,
    PlayDocsKeys.scalaManualSourceDirectories := (baseDirectory.value / "manual" / "working" / "scalaGuide" ** "code").get,
    PlayDocsKeys.commonManualSourceDirectories := (baseDirectory.value / "manual" / "working" / "commonGuide" ** "code").get,

    unmanagedSourceDirectories in Test ++= (baseDirectory.value / "manual" / "detailedTopics" ** "code").get,
    unmanagedResourceDirectories in Test ++= (baseDirectory.value / "manual" / "detailedTopics" ** "code").get,

    // Don't include sbt files in the resources
    excludeFilter in (Test, unmanagedResources) := (excludeFilter in (Test, unmanagedResources)).value || "*.sbt",

    crossScalaVersions := Seq("2.11.7"),
    scalaVersion := PlayVersion.scalaVersion,

    fork in Test := true,
    javaOptions in Test ++= Seq("-Xmx512m", "-Xms128m")
  )
   .dependsOn(
      playDocs,
      playProject("Play") % "test",
      playProject("Play-Specs2") % "test",
      playProject("Play-Java") % "test",
      playProject("Play-Java-JPA") % "test",
      playProject("Play-Cache") % "test",
      playProject("Play-Java-WS") % "test",
      playProject("Filters-Helpers") % "test",
      playProject("Play-JDBC-Evolutions") % "test",
      playProject("Play-JDBC") % "test",
      playProject("Play-Logback") % "test",
      playProject("Play-Java-JDBC") % "test"
  )

  lazy val playDocs = playProject("Play-Docs")

  def playProject(name: String) = ProjectRef(Path.fileProperty("user.dir").getParentFile / "framework", name)
}
