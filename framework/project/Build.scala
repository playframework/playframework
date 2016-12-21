/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

import bintray.BintrayPlugin
import bintray.BintrayPlugin.autoImport._
import sbt._
import Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.{previousArtifact, binaryIssueFilters, reportBinaryIssues}
import com.typesafe.tools.mima.core._
import com.typesafe.sbt.SbtScalariform.defaultScalariformSettings
import com.typesafe.sbt.pgp.PgpKeys
import xerial.sbt.Sonatype
import xerial.sbt.Sonatype.autoImport._
import scala.util.Properties.isJavaAtLeast
import play.twirl.sbt.SbtTwirl
import play.twirl.sbt.Import.TwirlKeys

object BuildSettings {
  import Resolvers._

  def propOr(name: String, value: String): String =
    (sys.props get name) orElse
    (sys.env get name) getOrElse
    value

  def boolProp(name: String, default: Boolean = false): Boolean =
    (sys.props get name) orElse
    (sys.env get name) filter
    (x => x == "true" || x == "") map
    (_ => true) getOrElse default

  val experimental = Option(System.getProperty("experimental")).exists(_ == "true")

  val buildOrganization = "com.typesafe.play"
  val buildVersion = propOr("play.version", "2.3-SNAPSHOT")
  val buildWithDoc = boolProp("generate.doc")
  val previousVersion = "2.3.0"
  // Libraries that are not Scala libraries or are SBT libraries should not be published if the binary
  // version doesn't match this.
  val publishForScalaBinaryVersion = "2.10"
  val defaultScalaVersion = "2.10.4"
  val buildScalaVersion = propOr("scala.version", defaultScalaVersion)
  // TODO - Try to compute this from SBT... or not.
  val buildScalaVersionForSbt = propOr("play.sbt.scala.version", defaultScalaVersion)
  val buildScalaBinaryVersionForSbt = CrossVersion.binaryScalaVersion(buildScalaVersionForSbt)
  val buildSbtVersion = propOr("play.sbt.version", "0.13.5")
  val buildSbtMajorVersion = "0.13"
  val buildSbtVersionBinaryCompatible = CrossVersion.binarySbtVersion(buildSbtVersion)
  // Used by api docs generation to link back to the correct branch on GitHub, only when version is a SNAPSHOT
  val sourceCodeBranch = propOr("git.branch", "master")

  val publishNonCoreScalaLibraries = publishForScalaBinaryVersion == CrossVersion.binaryScalaVersion(buildScalaVersion)

  lazy val PerformanceTest = config("pt") extend(Test)

  val playCommonSettings = Seq(
    organization := buildOrganization,
    version := buildVersion,
    scalaVersion := buildScalaVersion,
    homepage := Some(url("https://playframework.com")),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    ivyLoggingLevel := UpdateLogging.DownloadOnly,
    javacOptions ++= makeJavacOptions("1.6"),
    javacOptions in doc := Seq("-source", "1.6"),
    resolvers ++= playResolvers,
    fork in Test := true,
    testListeners in (Test,test) := Nil,
    javacOptions in Test := { if (isJavaAtLeast("1.8")) makeJavacOptions("1.8") else makeJavacOptions("1.6") },
    unmanagedSourceDirectories in Test ++= { if (isJavaAtLeast("1.8")) Seq((sourceDirectory in Test).value / "java8") else Nil },
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v"),
    testOptions in Test += Tests.Filter(!_.endsWith("Benchmark")),
    testOptions in PerformanceTest ~= (_.filterNot(_.isInstanceOf[Tests.Filter]) :+ Tests.Filter(_.endsWith("Benchmark"))),
    parallelExecution in PerformanceTest := false,
    pomExtra := {
      <scm>
        <url>https://github.com/playframework/playframework</url>
        <connection>scm:git:git@github.com:playframework/playframework.git</connection>
      </scm>
        <developers>
          <developer>
            <id>playframework</id>
            <name>Play Framework Team</name>
            <url>https://github.com/playframework</url>
          </developer>
        </developers>
    },
    pomIncludeRepository := { _ => false }
  )

  def makeJavacOptions(version: String) = Seq("-source", version, "-target", version, "-encoding", "UTF-8", "-Xlint:-options")

  val dontPublishSettings = Seq(
    /**
     * We actually must publish when doing a publish-local in order to ensure the scala 2.11 build works, very strange
     * things happen if you set publishArtifact := false, since it still publishes an ivy file when you do a
     * publish-local, but that ivy file says there's no artifacts.
     *
     * So, to disable publishing for the 2.11 build, we simply publish to a dummy repo instead of to the real thing.
     */
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo"))),
    publish := (),
    publishLocal := (),
    PgpKeys.publishSigned := ()
  )
  val sonatypePublishSettings = Seq(
    publishArtifact in packageDoc := buildWithDoc,
    publishArtifact in (Compile, packageSrc) := true,
    sonatypeProfileName := "com.typesafe"
  )

  def PlaySharedJavaProject(name: String, dir: String, testBinaryCompatibility: Boolean = false): Project = {
    val bcSettings: Seq[Setting[_]] = mimaDefaultSettings ++ (if (testBinaryCompatibility) {
      Seq(previousArtifact := Some(buildOrganization % moduleName.value % previousVersion))
    } else Nil)
    Project(name, file("src/" + dir))
      .disablePlugins(BintrayPlugin)
      .configs(PerformanceTest)
      .settings(inConfig(PerformanceTest)(Defaults.testTasks) : _*)
      .settings(playCommonSettings: _*)
      .settings((if (publishNonCoreScalaLibraries) sonatypePublishSettings else dontPublishSettings): _*)
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
      .disablePlugins(BintrayPlugin)
      .settings(playCommonSettings: _*)
      .settings(defaultScalariformSettings: _*)
      .settings(sonatypePublishSettings: _*)
      .settings(mimaDefaultSettings: _*)
  }

  /**
   * A project that is in the Play runtime
   */
  def PlayRuntimeProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .disablePlugins(BintrayPlugin)
      .configs(PerformanceTest)
      .settings(inConfig(PerformanceTest)(Defaults.testTasks) : _*)
      .settings(playCommonSettings: _*)
      .settings(sonatypePublishSettings: _*)
      .settings(mimaDefaultSettings: _*)
      .settings(defaultScalariformSettings: _*)
      .settings(playRuntimeSettings: _*)
  }

  def playRuntimeSettings: Seq[Setting[_]] = Seq(
    previousArtifact := Some(buildOrganization % s"${moduleName.value}_${scalaBinaryVersion.value}" % previousVersion),
    scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature"),
    Docs.apiDocsInclude := true
  )

  def PlaySbtProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .disablePlugins(BintrayPlugin)
      .settings(playCommonSettings: _*)
      .settings((if (publishNonCoreScalaLibraries) sonatypePublishSettings else dontPublishSettings): _*)
      .settings(defaultScalariformSettings: _*)
      .settings(
        scalaVersion := buildScalaVersionForSbt,
        scalaBinaryVersion := CrossVersion.binaryScalaVersion(buildScalaVersionForSbt),
        scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked"))
  }

  def PlaySbtPluginProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .disablePlugins(Sonatype)
      .settings(playCommonSettings: _*)
      .settings((if (publishNonCoreScalaLibraries) Nil else dontPublishSettings): _*)
      .settings(defaultScalariformSettings: _*)
      .settings(
        bintrayOrganization := Some("playframework"),
        bintrayRepository := "sbt-plugin-releases",
        bintrayPackage := "play-sbt-plugin",
        bintrayReleaseOnPublish := false,
        scalaVersion := buildScalaVersionForSbt,
        scalaBinaryVersion := CrossVersion.binaryScalaVersion(buildScalaVersionForSbt),
        scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked"))
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

object Resolvers {

  import BuildSettings._

  val typesafeReleases = "Typesafe Releases Repository" at "https://repo.typesafe.com/typesafe/releases/"
  val typesafeSnapshots = "Typesafe Snapshots Repository" at "https://repo.typesafe.com/typesafe/snapshots/"
  val typesafeIvyReleases = Resolver.url("Typesafe Ivy Releases Repository", url("https://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)
  val typesafeIvySnapshots = Resolver.url("Typesafe Ivy Snapshots Repository", url("https://repo.typesafe.com/typesafe/ivy-snapshots"))(Resolver.ivyStylePatterns)
  val publishTypesafeMavenReleases = "Typesafe Maven Releases Repository for publishing" at "https://private-repo.typesafe.com/typesafe/maven-releases/"
  val publishTypesafeMavenSnapshots = "Typesafe Maven Snapshots Repository for publishing" at "https://private-repo.typesafe.com/typesafe/maven-snapshots/"
  val publishTypesafeIvyReleases = Resolver.url("Typesafe Ivy Releases Repository for publishing", url("https://private-repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)
  val publishTypesafeIvySnapshots = Resolver.url("Typesafe Ivy Snapshots Repository for publishing", url("https://private-repo.typesafe.com/typesafe/ivy-snapshots/"))(Resolver.ivyStylePatterns)

  val sonatypeSnapshots = "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
  // This is a security issue. This repository should not be loaded via http
  // See http://blog.ontoillogical.com/blog/2014/07/28/how-to-take-over-any-java-developer/
  val sbtPluginSnapshots = Resolver.url("sbt plugin snapshots", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns)

  val isSnapshotBuild = buildVersion.endsWith("SNAPSHOT")
  val playResolvers = if (isSnapshotBuild) {
    Seq(typesafeReleases, typesafeIvyReleases, typesafeSnapshots, typesafeIvySnapshots, sonatypeSnapshots, sbtPluginSnapshots)
  } else {
    Seq(typesafeReleases, typesafeIvyReleases)
  }
  val publishingMavenRepository = if (isSnapshotBuild) publishTypesafeMavenSnapshots else publishTypesafeMavenReleases
  val publishingIvyRepository = if (isSnapshotBuild) publishTypesafeIvySnapshots else publishTypesafeIvyReleases
}


object PlayBuild extends Build {

  import Resolvers._
  import Dependencies._
  import BuildSettings._
  import Generators._
  import Tasks._

  lazy val BuildLinkProject = PlaySharedJavaProject("Build-Link", "build-link")
    .settings(libraryDependencies ++= link)
    .dependsOn(PlayExceptionsProject)

  lazy val RunSupportProject = PlayDevelopmentProject("Run-Support", "run-support")
    .settings(libraryDependencies ++= runSupportDependencies(scalaBinaryVersion.value))
    .dependsOn(BuildLinkProject)

  // extra run-support project that is only compiled against sbt scala version
  lazy val SbtRunSupportProject = PlaySbtProject("SBT-Run-Support", "run-support")
    .settings(
      target := target.value / "sbt-run-support",
      libraryDependencies ++= runSupportDependencies(scalaBinaryVersion.value)
    ).dependsOn(BuildLinkProject)

  lazy val RoutesCompilerProject = PlaySbtProject("Routes-Compiler", "routes-compiler")
    .settings(libraryDependencies ++= routersCompilerDependencies)

  lazy val AnormProject = PlayRuntimeProject("Anorm", "anorm")
    .settings(
      libraryDependencies ++= anormDependencies,
      resolvers += sonatypeSnapshots,
      addScalaModules(scalaParserCombinators),
      binaryIssueFilters ++= Seq(
        // Not part of the public API (private/internal def)
        ProblemFilters.exclude[MissingMethodProblem]("anorm.Sql.sql"),
        ProblemFilters.exclude[MissingMethodProblem]("anorm.Sql.anorm$Sql$$as"),
        ProblemFilters.exclude[MissingMethodProblem](
          "anorm.Sql.resultSetToStream"),
        ProblemFilters.exclude[MissingMethodProblem](
          "anorm.Sql.anorm$Sql$$data$1"),
        ProblemFilters.exclude[MissingMethodProblem]("anorm.Sql.foldWhile"),
        ProblemFilters.exclude[MissingMethodProblem]("anorm.Sql.fold"),
        ProblemFilters.exclude[MissingMethodProblem]("anorm.Sql.resultSet"),
        ProblemFilters.exclude[IncompatibleTemplateDefProblem](
          "anorm.SqlQueryResult"),
        ProblemFilters.exclude[IncompatibleResultTypeProblem](
          "anorm.SimpleSql.resultSet"),
        ProblemFilters.exclude[IncompatibleResultTypeProblem](
          "anorm.SqlQuery.resultSet"))
    )

  lazy val IterateesProject = PlayRuntimeProject("Play-Iteratees", "iteratees")
    .settings(libraryDependencies ++= iterateesDependencies)

  lazy val FunctionalProject = PlayRuntimeProject("Play-Functional", "play-functional")

  lazy val DataCommonsProject = PlayRuntimeProject("Play-DataCommons", "play-datacommons")

  lazy val JsonProject = PlayRuntimeProject("Play-Json", "play-json")
    .settings(libraryDependencies ++= jsonDependencies)
    .dependsOn(IterateesProject, FunctionalProject, DataCommonsProject)

  lazy val PlayExceptionsProject = PlaySharedJavaProject("Play-Exceptions", "play-exceptions",
    testBinaryCompatibility = true)

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
      parallelExecution in Test := false,
      binaryIssueFilters ++= Seq(
        // These methods were changed on a play[private] class
        ProblemFilters.exclude[MissingClassProblem]("play.core.ClosableLazy$CreateResult$"),
        ProblemFilters.exclude[MissingClassProblem]("play.core.ClosableLazy$CreateResult"),
        ProblemFilters.exclude[MissingMethodProblem]("play.core.ClosableLazy.CreateResult"),
        ProblemFilters.exclude[MissingMethodProblem]("play.core.ClosableLazy.close"),
        ProblemFilters.exclude[IncompatibleResultTypeProblem]("play.core.ClosableLazy.create"),
        ProblemFilters.exclude[MissingMethodProblem]("play.core.ClosableLazy.create"),
        ProblemFilters.exclude[IncompatibleResultTypeProblem]("controllers.Assets.controllers$Assets$$assetInfoFromResource")
      )

    ).dependsOn(BuildLinkProject, IterateesProject % "test->test;compile->compile", JsonProject)

  lazy val PlayJdbcProject = PlayRuntimeProject("Play-JDBC", "play-jdbc")
    .settings(libraryDependencies ++= jdbcDeps)
    .dependsOn(PlayProject).dependsOn(PlayTestProject % "test")

  lazy val PlayJavaJdbcProject = PlayRuntimeProject("Play-Java-JDBC", "play-java-jdbc")
    .dependsOn(PlayJdbcProject, PlayJavaProject)

  lazy val PlayEbeanProject = PlayRuntimeProject("Play-Java-Ebean", "play-java-ebean")
    .settings(
      libraryDependencies ++= ebeanDeps ++ jpaDeps,
      compile in (Compile) <<= (dependencyClasspath in Compile, compile in Compile, classDirectory in Compile) map {
        (deps, analysis, classes) =>

        // Ebean (really hacky sorry)
          val cp = deps.map(_.data.toURL).toArray :+ classes.toURL
          val cl = new java.net.URLClassLoader(cp)

          val t = cl.loadClass("com.avaje.ebean.enhance.agent.Transformer").getConstructor(classOf[Array[URL]], classOf[String]).newInstance(cp, "debug=0").asInstanceOf[AnyRef]
          val ft = cl.loadClass("com.avaje.ebean.enhance.ant.OfflineFileTransform").getConstructor(
            t.getClass, classOf[ClassLoader], classOf[String], classOf[String]
          ).newInstance(t, ClassLoader.getSystemClassLoader, classes.getAbsolutePath, classes.getAbsolutePath).asInstanceOf[AnyRef]

          ft.getClass.getDeclaredMethod("process", classOf[String]).invoke(ft, "play/db/ebean/**")

          analysis
      }
    ).dependsOn(PlayJavaJdbcProject)

  lazy val PlayJpaProject = PlayRuntimeProject("Play-Java-JPA", "play-java-jpa")
    .settings(libraryDependencies ++= jpaDeps)
    .dependsOn(PlayJavaJdbcProject)

  lazy val PlayTestProject = PlayRuntimeProject("Play-Test", "play-test")
    .settings(
      libraryDependencies ++= testDependencies,
      parallelExecution in Test := false
    ).dependsOn(PlayProject)

  lazy val PlayJavaProject = PlayRuntimeProject("Play-Java", "play-java")
    .settings(libraryDependencies ++= javaDeps ++ javaTestDeps)
    .dependsOn(PlayProject % "compile;test->test")

  lazy val PlayDocsProject = PlayRuntimeProject("Play-Docs", "play-docs")
    .settings(Docs.settings: _*)
    .settings(
      libraryDependencies ++= playDocsDependencies
    ).dependsOn(PlayProject)

  import ScriptedPlugin._

  lazy val SbtPluginProject = PlaySbtPluginProject("SBT-Plugin", "sbt-plugin")
    .settings(
      sbtPlugin := true,
      publishMavenStyle := false,
      libraryDependencies ++= sbtDependencies,
      sourceGenerators in Compile <+= (version, scalaVersion, sbtVersion, sourceManaged in Compile) map PlayVersion,
      sbtVersion in GlobalScope := buildSbtVersion,
      sbtBinaryVersion in GlobalScope := buildSbtVersionBinaryCompatible,
      sbtDependency <<= sbtDependency { dep =>
        dep.copy(revision = buildSbtVersion)
      },
      TaskKey[Unit]("sbtdep") <<= allDependencies map { deps: Seq[ModuleID] =>
        deps.map { d =>
          d.organization + ":" + d.name + ":" + d.revision
        }.sorted.foreach(println)
      },
      // Must be false, because due to the way SBT integrates with test libraries, and the way SBT uses Java object
      // serialisation to communicate with forked processes, and because this plugin will have SBT 0.13 on the forked
      // processes classpath while it's actually being run by SBT 0.12... if it forks you get serialVersionUID errors.
      fork in Test := false
    ).settings(scriptedSettings: _*)
    .settings(
      scriptedLaunchOpts ++= Seq(
        "-XX:MaxPermSize=384M",
        "-Dperformance.log=" + new File(baseDirectory.value, "target/sbt-repcomile-performance.properties"),
        "-Dproject.version=" + version.value
      ),
      /* The project does not publish all of the artifacts it needs for scripted tests as there is
       * an assumed order of testing operations.
       *
       * From @pvlugter:
       * The play docs project in particular takes a while to rebuild. The runtests script already
       * publishes everything locally as a first step (including scala cross build), so this just
       * lengthens the validation time. So when developing sbt plugins the idea is to publishLocal
       * everything first and have the scripted test just publishLocal the plugin parts themselves -
       * basically those parts that will be changing when working on the plugin.
       */
      scriptedDependencies := {
        val () = publishLocal.value
        val () = (publishLocal in RoutesCompilerProject).value
      }
    ).dependsOn(RoutesCompilerProject, SbtRunSupportProject)

  lazy val ForkRunProtocolProject = PlayDevelopmentProject("Fork-Run-Protocol", "fork-run-protocol")
    .settings(libraryDependencies ++= forkRunProtocolDependencies(scalaBinaryVersion.value))
    .dependsOn(RunSupportProject)

  // extra fork-run-protocol project that is only compiled against sbt scala version
  lazy val SbtForkRunProtocolProject = PlaySbtProject("SBT-Fork-Run-Protocol", "fork-run-protocol")
    .settings(
      target := target.value / "sbt-fork-run-protocol",
      libraryDependencies ++= forkRunProtocolDependencies(scalaBinaryVersion.value))
    .dependsOn(SbtRunSupportProject)

  lazy val ForkRunProject = PlayDevelopmentProject("Fork-Run", "fork-run")
    .settings(libraryDependencies ++= forkRunDependencies(scalaBinaryVersion.value))
    .dependsOn(ForkRunProtocolProject)

  lazy val SbtForkRunPluginProject = PlaySbtPluginProject("SBT-Fork-Run-Plugin", "sbt-fork-run-plugin")
    .settings(
      sbtPlugin := true,
      publishMavenStyle := false,
      libraryDependencies ++= sbtForkRunPluginDependencies)
    .settings(scriptedSettings: _*)
    .settings(
      scriptedLaunchOpts ++= Seq(
        "-XX:MaxPermSize=384M",
        "-Dproject.version=" + version.value
      ),
      scriptedDependencies := {
        val () = publishLocal.value
        val () = (publishLocal in SbtPluginProject).value
        val () = (publishLocal in RoutesCompilerProject).value
      })
    .dependsOn(SbtForkRunProtocolProject, SbtPluginProject)

  lazy val PlayWsProject = PlayRuntimeProject("Play-WS", "play-ws")
    .settings(
      libraryDependencies ++= playWsDeps,
      parallelExecution in Test := false
    ).dependsOn(PlayProject)
    .dependsOn(PlayTestProject % "test")

  lazy val PlayWsJavaProject = PlayRuntimeProject("Play-Java-WS", "play-java-ws")
      .settings(
        libraryDependencies ++= playWsDeps,
        parallelExecution in Test := false
      ).dependsOn(PlayProject)
    .dependsOn(PlayWsProject % "test->test;compile->compile", PlayJavaProject)

  lazy val PlayFiltersHelpersProject = PlayRuntimeProject("Filters-Helpers", "play-filters-helpers")
    .settings(
      parallelExecution in Test := false,
      binaryIssueFilters ++= Seq(
        // See https://github.com/playframework/playframework/issues/2967, these methods had to be changed because as
        // they could not be used as intended
        ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.filters.headers.SecurityHeadersFilter.apply"),
        ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.filters.headers.SecurityHeadersFilter.this")
      )
    ).dependsOn(PlayProject, PlayTestProject % "test", PlayJavaProject % "test", PlayWsProject % "test")

  // This project is just for testing Play, not really a public artifact
  lazy val PlayIntegrationTestProject = PlayRuntimeProject("Play-Integration-Test", "play-integration-test")
    .settings(
      parallelExecution in Test := false,
      libraryDependencies ++= integrationTestDependencies,
      previousArtifact := None
    )
    .dependsOn(PlayProject % "test->test", PlayWsProject, PlayWsJavaProject, PlayTestProject)
    .dependsOn(PlayFiltersHelpersProject)
    .dependsOn(PlayJavaProject)

  lazy val PlayCacheProject = PlayRuntimeProject("Play-Cache", "play-cache")
    .settings(
      libraryDependencies ++= playCacheDeps,
      parallelExecution in Test := false
    ).dependsOn(PlayProject)
    .dependsOn(PlayTestProject % "test")

  import RepositoryBuilder._
  lazy val RepositoryProject = Project(
      "Play-Repository", file("repository"))
    .disablePlugins(Sonatype, BintrayPlugin)
    .settings(dontPublishSettings:_*)
    .settings(localRepoCreationSettings:_*)
    .settings(mimaDefaultSettings: _*)
    .settings(
      localRepoProjectsPublished <<= (publishedProjects map (publishLocal in _)).dependOn,
      addProjectsToRepository(publishedProjects),
      localRepoArtifacts ++= Seq(
        "org.scala-lang" % "scala-compiler" % BuildSettings.buildScalaVersion % "master",
        "org.scala-lang" % "jline" % BuildSettings.buildScalaVersion % "master",
        "org.scala-lang" % "scala-compiler" % BuildSettings.buildScalaVersionForSbt % "master",
        "org.scala-lang" % "jline" % BuildSettings.buildScalaVersionForSbt % "master",
        "org.scala-sbt" % "sbt" % BuildSettings.buildSbtVersion,
        "org.fusesource.jansi" % "jansi" % "1.11" % "master"
      )
    )

  lazy val PlayDocsSbtPlugin = PlaySbtPluginProject("Play-Docs-SBT-Plugin", "play-docs-sbt-plugin")
    .settings(
      sbtPlugin := true,
      publishMavenStyle := false,
      libraryDependencies ++= playDocsSbtPluginDependencies,
      sbtVersion in GlobalScope := buildSbtVersion,
      sbtBinaryVersion in GlobalScope := buildSbtVersionBinaryCompatible,
      sbtDependency <<= sbtDependency { dep =>
        dep.copy(revision = buildSbtVersion)
      },
      // Must be false, because due to the way SBT integrates with test libraries, and the way SBT uses Java object
      // serialisation to communicate with forked processes, and because this plugin will have SBT 0.13 on the forked
      // processes classpath while it's actually being run by SBT 0.12... if it forks you get serialVersionUID errors.
      fork in Test := false
    ).dependsOn(SbtPluginProject)

  lazy val publishedProjects = Seq[ProjectReference](
    PlayProject,
    BuildLinkProject,
    AnormProject,
    IterateesProject,
    FunctionalProject,
    DataCommonsProject,
    JsonProject,
    RoutesCompilerProject,
    PlayCacheProject,
    PlayJdbcProject,
    PlayJavaProject,
    PlayJavaJdbcProject,
    PlayEbeanProject,
    PlayJpaProject,
    PlayWsProject,
    PlayWsJavaProject,
    SbtRunSupportProject,
    RunSupportProject,
    SbtPluginProject,
    ForkRunProtocolProject,
    SbtForkRunProtocolProject,
    ForkRunProject,
    SbtForkRunPluginProject,
    PlayTestProject,
    PlayExceptionsProject,
    PlayDocsProject,
    PlayFiltersHelpersProject,
    PlayIntegrationTestProject,
    PlayDocsSbtPlugin
  )

  lazy val Root = Project(
    "Root",
    file("."))
    .disablePlugins(Sonatype, BintrayPlugin)
    .settings(playCommonSettings: _*)
    .settings(dontPublishSettings:_*)
    .settings(
      concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
      libraryDependencies ++= (runtime ++ jdbcDeps),
      Docs.apiDocsInclude := false,
      Docs.apiDocsIncludeManaged := false,
      reportBinaryIssues := ()
    )
    .aggregate(publishedProjects: _*)
    .aggregate(RepositoryProject)
}
