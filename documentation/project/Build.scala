/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import sbt.Keys._
import play.Play.autoImport._
import play.PlaySourceGenerators._
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
    scalaVersion := PlayVersion.scalaVersion,
    libraryDependencies ++= Seq(
      component("play") % "test",
      component("play-test") % "test",
      component("play-java") % "test",
      component("play-cache") % "test",
      component("play-java-ws") % "test",
      component("filters-helpers") % "test",
      "org.mockito" % "mockito-core" % "1.9.5" % "test",
      component("play-docs")
    ),

    PlayDocsKeys.fallbackToJar := false,

    javaManualSourceDirectories <<= (baseDirectory)(base => (base / "manual" / "javaGuide" ** codeFilter).get),
    scalaManualSourceDirectories <<= (baseDirectory)(base => (base / "manual" / "scalaGuide" ** codeFilter).get),

    javaManualSourceDirectories <++= (baseDirectory) { base =>
      if (isJavaAtLeast("1.8")) (base / "manual" / "javaGuide" ** "java8code").get else Nil
    },

    unmanagedSourceDirectories in Test <++= javaManualSourceDirectories,
    unmanagedSourceDirectories in Test <++= scalaManualSourceDirectories,
    unmanagedSourceDirectories in Test <++= (baseDirectory)(base => (base / "manual" / "detailedTopics" ** codeFilter).get),

    unmanagedResourceDirectories in Test <++= javaManualSourceDirectories,
    unmanagedResourceDirectories in Test <++= scalaManualSourceDirectories,
    unmanagedResourceDirectories in Test <++= (baseDirectory)(base => (base / "manual" / "detailedTopics" ** codeFilter).get),

    parallelExecution in Test := false,

    (compile in Test) <<= Enhancement.enhanceJavaClasses,

    javacOptions ++= Seq("-g", "-Xlint:deprecation"),

    javaRoutesSourceManaged := target.value / "routes" / "java",
    scalaRoutesSourceManaged := target.value / "routes" / "scala",
    javaTwirlSourceManaged := target.value / "twirl" / "java",
    scalaTwirlSourceManaged := target.value / "twirl" / "scala",
    (managedSourceDirectories in Test) ++= Seq(
      javaRoutesSourceManaged.value,
      scalaRoutesSourceManaged.value,
      javaTwirlSourceManaged.value,
      scalaTwirlSourceManaged.value
    ),

    // Need to ensure that templates in the Java docs get Java imports, and in the Scala docs get Scala imports
    sourceGenerators in Test <+= (javaManualSourceDirectories, javaTwirlSourceManaged, streams) map { (from, to, s) =>
      compileTemplates(from, to, defaultTemplateImports ++ defaultJavaTemplateImports, s.log)
    },
    sourceGenerators in Test <+= (scalaManualSourceDirectories, scalaTwirlSourceManaged, streams) map { (from, to, s) =>
      compileTemplates(from, to, defaultTemplateImports ++ defaultScalaTemplateImports, s.log)
    },

    sourceGenerators in Test <+= (state, javaManualSourceDirectories, javaRoutesSourceManaged) map  { (s, ds, g) =>
      RouteFiles(s, ds, g, Seq("play.libs.F"), true, true, true)
    },
    sourceGenerators in Test <+= (state, scalaManualSourceDirectories, scalaRoutesSourceManaged) map  { (s, ds, g) =>
      RouteFiles(s, ds, g, Seq(), true, true, true)
    },

    testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "sequential", "true", "junitxml", "console"),
    testOptions in Test += Tests.Argument(TestFrameworks.JUnit, "-v", "--ignore-runners=org.specs2.runner.JUnitRunner")

  ).settings(externalPlayModuleSettings:_*)

  val templateFormats = Map("html" -> "play.twirl.api.HtmlFormat")
  val templateFilter = "*.scala.*"
  val templateCodec = scala.io.Codec("UTF-8")

  def compileTemplates(sourceDirectories: Seq[File], target: File, imports: Seq[String], log: Logger) = {
    play.twirl.sbt.TemplateCompiler.compile(sourceDirectories, target, templateFormats, imports, templateFilter, HiddenFileFilter, templateCodec, false, log)
  }

  val javaManualSourceDirectories = SettingKey[Seq[File]]("java-manual-source-directories")
  val scalaManualSourceDirectories = SettingKey[Seq[File]]("scala-manual-source-directories")
  val javaRoutesSourceManaged = SettingKey[File]("java-routes-source-managed")
  val scalaRoutesSourceManaged = SettingKey[File]("scala-routes-source-managed")
  val javaTwirlSourceManaged = SettingKey[File]("java-routes-source-managed")
  val scalaTwirlSourceManaged = SettingKey[File]("scala-routes-source-managed")

}
