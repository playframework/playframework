/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

import com.typesafe.play.docs.sbtplugin.Imports._
import com.typesafe.play.docs.sbtplugin._
import com.typesafe.play.sbt.enhancer.PlayEnhancer
import play.core.PlayVersion
import sbt._

lazy val main = Project("Play-Documentation", file("."))
    .enablePlugins(PlayDocsPlugin, SbtTwirl)
    .disablePlugins(PlayEnhancer)
    .settings(
      // Avoid the use of deprecated APIs in the docs
      scalacOptions ++= Seq("-deprecation", "-Xfatal-warnings"),
      javacOptions ++= Seq("-encoding", "UTF-8", "-source", "1.8", "-target", "1.8", "-parameters", "-Xlint:unchecked", "-Xlint:deprecation", "-Werror"),

      // We need to publishLocal playDocs since its jar file is
      // a dependency of `docsJarFile` setting.
      test in Test <<= (test in Test).dependsOn(publishLocal in playDocs),

      resolvers += Resolver.sonatypeRepo("releases"), // TODO: Delete this eventually, just needed for lag between deploying to sonatype and getting on maven central
      version := PlayVersion.current,
      libraryDependencies ++= Seq(
        "com.typesafe" % "config" % "1.3.3" % Test,
        "com.h2database" % "h2" % "1.4.197" % Test,
        "org.mockito" % "mockito-core" % "2.18.3" % "test",
        // https://github.com/logstash/logstash-logback-encoder/tree/logstash-logback-encoder-4.9#including
        "net.logstash.logback" % "logstash-logback-encoder" % "5.1" % "test"
      ),

      PlayDocsKeys.docsJarFile := Some((packageBin in(playDocs, Compile)).value),
      PlayDocsKeys.playDocsValidationConfig := PlayDocsValidation.ValidationConfig(downstreamWikiPages = Set(
        "JavaEbean",
        "ScalaAnorm",
        "PlaySlick",
        "PlaySlickMigrationGuide",
        "ScalaTestingWithScalaTest",
        "ScalaFunctionalTestingWithScalaTest",
        "ScalaJson",
        "ScalaJsonAutomated",
        "ScalaJsonCombinators",
        "ScalaJsonTransformers"
      )),

      PlayDocsKeys.javaManualSourceDirectories :=
        (baseDirectory.value / "manual" / "working" / "javaGuide" ** "code").get ++
        (baseDirectory.value / "manual" / "gettingStarted" ** "code").get,

      PlayDocsKeys.scalaManualSourceDirectories :=
        (baseDirectory.value / "manual" / "working" / "scalaGuide" ** "code").get ++
        (baseDirectory.value / "manual" / "tutorial" ** "code").get ++
        (baseDirectory.value / "manual" / "experimental" ** "code").get,

      PlayDocsKeys.commonManualSourceDirectories :=
        (baseDirectory.value / "manual" / "working" / "commonGuide" ** "code").get ++
        (baseDirectory.value / "manual" / "gettingStarted" ** "code").get,

      unmanagedSourceDirectories in Test ++= (baseDirectory.value / "manual" / "detailedTopics" ** "code").get,
      unmanagedResourceDirectories in Test ++= (baseDirectory.value / "manual" / "detailedTopics" ** "code").get,

      // Don't include sbt files in the resources
      excludeFilter in(Test, unmanagedResources) := (excludeFilter in(Test, unmanagedResources)).value || "*.sbt",

      crossScalaVersions := Seq(PlayVersion.scalaVersion, "2.11.12"),
      scalaVersion := PlayVersion.scalaVersion,

      fork in Test := true,
      javaOptions in Test ++= Seq("-Xmx512m", "-Xms128m"),

      headerLicense := Some(HeaderLicense.Custom("Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>")),

      // No need to show eviction warnings for Play documentation.
      evictionWarningOptions in update := EvictionWarningOptions.default
        .withWarnTransitiveEvictions(false)
        .withWarnDirectEvictions(false)
    )
    .dependsOn(
      playDocs,
      playProject("Play") % "test",
      playProject("Play-Specs2") % "test",
      playProject("Play-Java") % "test",
      playProject("Play-Java-Forms") % "test",
      playProject("Play-Java-JPA") % "test",
      playProject("Play-Guice") % "test",
      playProject("Play-Caffeine-Cache") % "test",
      playProject("Play-AHC-WS") % "test",
      playProject("Play-OpenID") % "test",
      playProject("Filters-Helpers") % "test",
      playProject("Play-JDBC-Evolutions") % "test",
      playProject("Play-JDBC") % "test",
      playProject("Play-Logback") % "test",
      playProject("Play-Java-JDBC") % "test",
      playProject("Play-Akka-Http-Server") % "test",
      playProject("Play-Netty-Server") % "test"
    )

lazy val playDocs = playProject("Play-Docs")

def playProject(name: String) = ProjectRef(Path.fileProperty("user.dir").getParentFile / "framework", name)
