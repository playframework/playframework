/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.{previousArtifact, binaryIssueFilters, reportBinaryIssues}
import com.typesafe.tools.mima.core._
import com.typesafe.sbt.SbtScalariform.scalariformSettings
import scala.util.Properties.isJavaAtLeast
import play.twirl.sbt.SbtTwirl
import play.twirl.sbt.Import.TwirlKeys
import interplay.Omnidoc
import interplay.Omnidoc.Import.OmnidocKeys

object BuildSettings {

  val buildVersion = propOr("play.version", "2.4-SNAPSHOT")

  val defaultScalaVersion = "2.10.4"
  val buildScalaVersion = propOr("scala.version", defaultScalaVersion)

  val buildScalaVersionForSbt = propOr("play.sbt.scala.version", defaultScalaVersion)
  val buildScalaBinaryVersionForSbt = CrossVersion.binaryScalaVersion(buildScalaVersionForSbt)
  val buildSbtVersion = propOr("play.sbt.version", "0.13.5")
  val buildSbtMajorVersion = "0.13"
  val buildSbtVersionBinaryCompatible = CrossVersion.binarySbtVersion(buildSbtVersion)

  // Used by api docs generation to link back to the correct branch on GitHub, only when version is a SNAPSHOT
  val sourceCodeBranch = propOr("git.branch", "master")

  // Libraries that are not Scala libraries or are SBT libraries should not be published if the binary
  // version doesn't match this.
  val publishForScalaBinaryVersion = "2.10"
  val thisBuildIsCrossBuild =
    publishForScalaBinaryVersion != CrossVersion.binaryScalaVersion(buildScalaVersion)

  val buildOrganization = "com.typesafe.play"

  // Binary compatibility is tested against this version
  val previousVersion = "2.4.0"

  val buildWithDoc = boolProp("generate.doc")

  // Argument for setting size of permgen space or meta space for all forked processes
  val maxMetaspace = {
    val space = if (isJavaAtLeast("1.8")) "Metaspace" else "Perm"
    s"-XX:Max${space}Size=384m"
  }

  def propOr(name: String, value: String): String =
    (sys.props get name) orElse
      (sys.env get name) getOrElse
      value

  def boolProp(name: String, default: Boolean = false): Boolean =
    (sys.props get name) orElse
      (sys.env get name) filter
      (x => x == "true" || x == "") map
      (_ => true) getOrElse default

  def playCommonSettings: Seq[Setting[_]] = Seq(
    organization := buildOrganization,
    version := buildVersion,
    scalaVersion := buildScalaVersion,
    homepage := Some(url("https://playframework.com")),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    ivyLoggingLevel := UpdateLogging.DownloadOnly,
    javacOptions ++= makeJavacOptions("1.6"),
    javacOptions in doc := Seq("-source", "1.6"),
    resolvers ++= ResolverSettings.playResolvers,
    resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases", // specs2 depends on scalaz-stream
    fork in Test := true,
    testListeners in (Test,test) := Nil,
    javacOptions in Test := { if (isJavaAtLeast("1.8")) makeJavacOptions("1.8") else makeJavacOptions("1.6") },
    unmanagedSourceDirectories in Test ++= { if (isJavaAtLeast("1.8")) Seq((sourceDirectory in Test).value / "java8") else Nil },
    javaOptions in Test += maxMetaspace,
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v"),
    testOptions in Test += Tests.Filter(!_.endsWith("Benchmark"))
  )

  def makeJavacOptions(version: String) = Seq("-source", version, "-target", version, "-encoding", "UTF-8", "-Xlint:-options", "-J-Xmx512m")

  /**
   * A project that is shared between the SBT runtime and the Play runtime
   */
  def PlaySharedJavaProject(name: String, dir: String, testBinaryCompatibility: Boolean = false): Project = {
    val bcSettings: Seq[Setting[_]] = mimaDefaultSettings ++ (if (testBinaryCompatibility) {
      Seq(previousArtifact := Some(buildOrganization % moduleName.value % previousVersion))
    } else Nil)
    Project(name, file("src/" + dir))
      .settings(playCommonSettings: _*)
      .settings(PublishSettings.nonCrossBuildPublishSettings: _*)
      .settings(bcSettings: _*)
      .settings(
        scalaVersion := defaultScalaVersion,
        scalaBinaryVersion := CrossVersion.binaryScalaVersion(defaultScalaVersion),
        autoScalaLibrary := false,
        crossPaths := false
      )
  }

  /**
   * A project that is only used when running in development.
   */
  def PlayDevelopmentProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .settings(playCommonSettings: _*)
      .settings(scalariformSettings: _*)
      .settings(PublishSettings.publishSettings: _*)
      .settings(mimaDefaultSettings: _*)
  }

  /**
   * A project that is in the Play runtime
   */
  def PlayRuntimeProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .settings(playCommonSettings: _*)
      .settings(PublishSettings.publishSettings: _*)
      .settings(mimaDefaultSettings: _*)
      .settings(scalariformSettings: _*)
      .settings(playRuntimeSettings: _*)
      .settings(omnidocSettings: _*)
  }

  def playRuntimeSettings: Seq[Setting[_]] = Seq(
    previousArtifact := Some(buildOrganization % s"${moduleName.value}_${scalaBinaryVersion.value}" % previousVersion),
    scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature"),
    Docs.apiDocsInclude := true
  )

  def omnidocSettings: Seq[Setting[_]] = Omnidoc.projectSettings ++ Seq(
    OmnidocKeys.githubRepo := "playframework/playframework",
    OmnidocKeys.snapshotBranch := sourceCodeBranch,
    OmnidocKeys.tagPrefix := "",
    OmnidocKeys.pathPrefix := "framework/"
  )

  def playSbtCommonSettings: Seq[Setting[_]] = playCommonSettings ++ scalariformSettings ++ Seq(
    scalaVersion := buildScalaVersionForSbt,
    scalaBinaryVersion := CrossVersion.binaryScalaVersion(buildScalaVersionForSbt),
    scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature")
  )

  /**
   * A project that runs in the SBT runtime
   */
  def PlaySbtProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .settings(playSbtCommonSettings: _*)
      .settings(PublishSettings.nonCrossBuildPublishSettings: _*)
  }

  /**
   * A project that *is* an SBT plugin
   */
  def PlaySbtPluginProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .settings(playSbtCommonSettings: _*)
      .settings(PublishSettings.sbtPluginPublishSettings: _*)
      .settings(ScriptedPlugin.scriptedSettings: _*)
      .settings(
        sbtPlugin := true,
        sbtVersion in GlobalScope := buildSbtVersion,
        sbtBinaryVersion in GlobalScope := buildSbtVersionBinaryCompatible,
        sbtDependency <<= sbtDependency { dep =>
          dep.copy(revision = buildSbtVersion)
        },
        // Must be false, because due to the way SBT integrates with test libraries, and the way SBT uses Java object
        // serialisation to communicate with forked processes, and because this plugin will have SBT 0.13 on the forked
        // processes classpath while it's actually being run by SBT 0.12... if it forks you get serialVersionUID errors.
        fork in Test := false
      )
  }

  /**
   * Adds a set of Scala 2.11 modules to the build.
   *
   * Only adds if Scala version is >= 2.11.
   */
  def addScalaModules(modules: ModuleID*): Setting[_] = {
    libraryDependencies := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor >= 11 =>
          libraryDependencies.value ++ modules
        case _ =>
          libraryDependencies.value
      }
    }
  }

  val scalaParserCombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1"
}

object PlayBuild extends Build {

  import Dependencies._
  import BuildSettings._
  import Generators._
  import Tasks._

  lazy val BuildLinkProject = PlaySharedJavaProject("Build-Link", "build-link")
    .settings(libraryDependencies ++= link)
    .dependsOn(PlayExceptionsProject)

  lazy val RunSupportProject = PlayDevelopmentProject("Run-Support", "run-support")
    .settings(libraryDependencies ++= runSupportDependencies(scalaBinaryVersion.value))

  // extra run-support project that is only compiled against sbt scala version
  lazy val SbtRunSupportProject = PlaySbtProject("SBT-Run-Support", "run-support")
    .settings(
      target := target.value / "sbt-run-support",
      libraryDependencies ++= runSupportDependencies(scalaBinaryVersion.value)
    )

  lazy val RoutesCompilerProject = PlaySbtProject("Routes-Compiler", "routes-compiler")
    .enablePlugins(SbtTwirl)
    .settings(
      libraryDependencies ++= routersCompilerDependencies,
      TwirlKeys.templateFormats := Map("twirl" -> "play.routes.compiler.ScalaFormat")
    )

  lazy val IterateesProject = PlayRuntimeProject("Play-Iteratees", "iteratees")
    .settings(libraryDependencies ++= iterateesDependencies)

  lazy val StreamsProject = PlayRuntimeProject("Play-Streams-Experimental", "play-streams")
    .settings(libraryDependencies ++= streamsDependencies)
    .dependsOn(IterateesProject)

  lazy val FunctionalProject = PlayRuntimeProject("Play-Functional", "play-functional")

  lazy val DataCommonsProject = PlayRuntimeProject("Play-DataCommons", "play-datacommons")

  lazy val JsonProject = PlayRuntimeProject("Play-Json", "play-json")
    .settings(libraryDependencies ++= jsonDependencies)
    .dependsOn(IterateesProject, FunctionalProject, DataCommonsProject)

  lazy val PlayExceptionsProject = PlaySharedJavaProject("Play-Exceptions", "play-exceptions",
    testBinaryCompatibility = true)

  lazy val PlayNettyUtilsProject = PlaySharedJavaProject("Play-Netty-Utils", "play-netty-utils")
    .settings(javacOptions in (Compile,doc) ++= {
      // References to the rest of Netty don't work in the code that
      // we've excised. Disable Javadoc lint checking in Java 8+.
      if (isJavaAtLeast("1.8")) Seq("-Xdoclint:none") else Seq()
    })

  lazy val PlayProject = PlayRuntimeProject("Play", "play")
    .enablePlugins(SbtTwirl)
    .settings(
      addScalaModules(scalaParserCombinators),
      libraryDependencies ++= runtime ++ scalacheckDependencies,
      sourceGenerators in Compile <+= (version, scalaVersion, sbtVersion, sourceManaged in Compile) map PlayVersion,
      sourceDirectories in (Compile, TwirlKeys.compileTemplates) := (unmanagedSourceDirectories in Compile).value,
      TwirlKeys.templateImports += "play.api.templates.PlayMagic._",
      mappings in (Compile, packageSrc) <++= scalaTemplateSourceMappings,
      Docs.apiDocsIncludeManaged := true,
      parallelExecution in Test := false
    ).settings(Docs.playdocSettings: _*)
     .dependsOn(
      BuildLinkProject,
      IterateesProject % "test->test;compile->compile",
      JsonProject,
      PlayExceptionsProject,
      PlayNettyUtilsProject
    )

  lazy val PlayServerProject = PlayRuntimeProject("Play-Server", "play-server")
    .settings(libraryDependencies ++= playServerDependencies)
    .dependsOn(
      PlayProject,
      IterateesProject % "test->test;compile->compile"
    )

  lazy val PlayNettyServerProject = PlayRuntimeProject("Play-Netty-Server", "play-netty-server")
    .settings(libraryDependencies ++= netty)
    .dependsOn(PlayServerProject)

  import ScriptedPlugin._

  lazy val PlayAkkaHttpServerProject = PlayRuntimeProject("Play-Akka-Http-Server-Experimental", "play-akka-http-server")
    .settings(libraryDependencies ++= akkaHttp)
     // Include scripted tests here as well as in the SBT Plugin, because we
     // don't want the SBT Plugin to have a dependency on an experimental module.
    .settings(scriptedSettings: _*)
    .settings(
      scriptedLaunchOpts ++= Seq(
        maxMetaspace,
        "-Dproject.version=" + version.value,
        "-Dscala.version=" + buildScalaVersion
      )
    )
    .dependsOn(PlayServerProject, StreamsProject)
    .dependsOn(PlaySpecs2Project % "test", PlayWsProject % "test")

  lazy val PlayJdbcProject = PlayRuntimeProject("Play-JDBC", "play-jdbc")
    .settings(libraryDependencies ++= jdbcDeps)
    .dependsOn(PlayProject).dependsOn(PlaySpecs2Project % "test")

  lazy val PlayJavaJdbcProject = PlayRuntimeProject("Play-Java-JDBC", "play-java-jdbc").settings(libraryDependencies ++= javaJdbcDeps)
    .dependsOn(PlayJdbcProject, PlayJavaProject)
    .dependsOn(PlaySpecs2Project % "test")

  lazy val PlayJpaProject = PlayRuntimeProject("Play-Java-JPA", "play-java-jpa")
    .settings(libraryDependencies ++= jpaDeps)
    .dependsOn(PlayJavaJdbcProject % "compile;test->test")

  lazy val PlayTestProject = PlayRuntimeProject("Play-Test", "play-test")
    .settings(
      libraryDependencies ++= testDependencies,
      parallelExecution in Test := false
    ).dependsOn(PlayNettyServerProject)

  lazy val PlaySpecs2Project = PlayRuntimeProject("Play-Specs2", "play-specs2")
    .settings(
      libraryDependencies ++= specsBuild,
      parallelExecution in Test := false
    ).dependsOn(PlayTestProject)

  lazy val PlayJavaProject = PlayRuntimeProject("Play-Java", "play-java")
    .settings(libraryDependencies ++= javaDeps ++ javaTestDeps)
    .dependsOn(PlayProject % "compile;test->test")

  lazy val PlayDocsProject = PlayRuntimeProject("Play-Docs", "play-docs")
    .settings(Docs.settings: _*)
    .settings(
      libraryDependencies ++= playDocsDependencies
    ).dependsOn(PlayNettyServerProject)

  lazy val SbtPluginProject = PlaySbtPluginProject("SBT-Plugin", "sbt-plugin")
    .settings(
      libraryDependencies ++= sbtDependencies,
      sourceGenerators in Compile <+= (version, scalaVersion, sbtVersion, sourceManaged in Compile) map PlayVersion,
      TaskKey[Unit]("sbtdep") <<= allDependencies map { deps: Seq[ModuleID] =>
        deps.map { d =>
          d.organization + ":" + d.name + ":" + d.revision
        }.sorted.foreach(println)
      },
      scriptedLaunchOpts ++= Seq(
        "-Xmx768m",
        maxMetaspace,
        "-Dperformance.log=" + new File(baseDirectory.value, "target/sbt-repcomile-performance.properties"),
        "-Dproject.version=" + version.value,
        "-Dscala.version=" + buildScalaVersion
      ),
      scriptedDependencies := {
        val () = publishLocal.value
        val () = (publishLocal in RoutesCompilerProject).value
      }
    ).dependsOn(BuildLinkProject, PlayExceptionsProject, RoutesCompilerProject, SbtRunSupportProject)

  lazy val PlayWsProject = PlayRuntimeProject("Play-WS", "play-ws")
    .settings(
      libraryDependencies ++= playWsDeps,
      parallelExecution in Test := false,
      // quieten deprecation warnings in tests
      scalacOptions in Test := (scalacOptions in Test).value diff Seq("-deprecation")
    ).dependsOn(PlayProject)
    .dependsOn(PlaySpecs2Project % "test")

  lazy val PlayWsJavaProject = PlayRuntimeProject("Play-Java-WS", "play-java-ws")
      .settings(
        libraryDependencies ++= playWsDeps,
        parallelExecution in Test := false
      ).dependsOn(PlayProject)
    .dependsOn(PlayWsProject % "test->test;compile->compile", PlayJavaProject)

  lazy val PlayFiltersHelpersProject = PlayRuntimeProject("Filters-Helpers", "play-filters-helpers")
    .settings(
      parallelExecution in Test := false
    ).dependsOn(PlayProject, PlaySpecs2Project % "test", PlayJavaProject % "test", PlayWsProject % "test")

  // This project is just for testing Play, not really a public artifact
  lazy val PlayIntegrationTestProject = PlayRuntimeProject("Play-Integration-Test", "play-integration-test")
    .settings(
      parallelExecution in Test := false,
      previousArtifact := None
    )
    .dependsOn(PlayProject % "test->test", PlayWsProject, PlayWsJavaProject, PlaySpecs2Project)
    .dependsOn(PlayFiltersHelpersProject)
    .dependsOn(PlayJavaProject)

  lazy val PlayCacheProject = PlayRuntimeProject("Play-Cache", "play-cache")
    .settings(
      libraryDependencies ++= playCacheDeps,
      parallelExecution in Test := false
    ).dependsOn(PlayProject)
    .dependsOn(PlaySpecs2Project % "test")

  import RepositoryBuilder._
  lazy val RepositoryProject = Project(
      "Play-Repository", file("repository"))
    .settings(PublishSettings.dontPublishSettings: _*)
    .settings(localRepoCreationSettings:_*)
    .settings(mimaDefaultSettings: _*)
    .settings(
      localRepoProjectsPublished <<= (publishedProjects map (publishLocal in _)).dependOn,
      addProjectsToRepository(publishedProjects),
      localRepoArtifacts ++= Seq(
        "org.scala-lang" % "scala-compiler" % buildScalaVersion % "master",
        "org.scala-lang" % "jline" % buildScalaVersion % "master",
        "org.scala-lang" % "scala-compiler" % buildScalaVersionForSbt % "master",
        "org.scala-lang" % "jline" % buildScalaVersionForSbt % "master",
        "org.scala-sbt" % "sbt" % buildSbtVersion,
        "org.fusesource.jansi" % "jansi" % "1.11" % "master"
      )
    )

  lazy val PlayDocsSbtPlugin = PlaySbtPluginProject("Play-Docs-SBT-Plugin", "play-docs-sbt-plugin")
    .enablePlugins(SbtTwirl)
    .settings(
      libraryDependencies ++= playDocsSbtPluginDependencies
    ).dependsOn(SbtPluginProject)

  lazy val publishedProjects = Seq[ProjectReference](
    PlayProject,
    BuildLinkProject,
    IterateesProject,
    FunctionalProject,
    DataCommonsProject,
    JsonProject,
    RoutesCompilerProject,
    PlayAkkaHttpServerProject,
    PlayCacheProject,
    PlayJdbcProject,
    PlayJavaProject,
    PlayJavaJdbcProject,
    PlayJpaProject,
    PlayNettyUtilsProject,
    PlayNettyServerProject,
    PlayServerProject,
    PlayWsProject,
    PlayWsJavaProject,
    SbtRunSupportProject,
    RunSupportProject,
    SbtPluginProject,
    PlaySpecs2Project,
    PlayTestProject,
    PlayExceptionsProject,
    PlayDocsProject,
    PlayFiltersHelpersProject,
    PlayIntegrationTestProject,
    PlayDocsSbtPlugin,
    StreamsProject
  )

  lazy val Root = Project(
    "Root",
    file("."))
    .settings(playCommonSettings: _*)
    .settings(PublishSettings.dontPublishSettings: _*)
    .settings(
      concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
      libraryDependencies ++= (runtime ++ jdbcDeps),
      Docs.apiDocsInclude := false,
      Docs.apiDocsIncludeManaged := false,
      reportBinaryIssues := ()
    )
    .aggregate(publishedProjects: _*)
}
