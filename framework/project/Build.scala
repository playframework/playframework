/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.{previousArtifact, binaryIssueFilters}
import com.typesafe.tools.mima.core._
import com.typesafe.sbt.SbtScalariform.defaultScalariformSettings
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

  class SharedProjectScalaVersion(val scalaVersion: String, val targetDir: String) {
    val nameSuffix:String = targetDir.replace(".","").trim()
    def toSettings(targetPrefix:String):Seq[Setting[_]] = Seq(
      target := target.value / s"$targetPrefix-$targetDir",
      Keys.scalaVersion := scalaVersion
    )
  }

  object SharedProjectScalaVersion {
    def forScalaVersion(scalaVersion:String):SharedProjectScalaVersion =
      new SharedProjectScalaVersion(scalaVersion,CrossVersion.binaryScalaVersion(scalaVersion))
  }

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
    parallelExecution in PerformanceTest := false
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
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
  )
  val publishSettings = Seq(
    publishArtifact in packageDoc := buildWithDoc,
    publishArtifact in (Compile, packageSrc) := true,
    publishTo := Some(publishingMavenRepository)
  )

  def PlaySharedJavaProject(name: String, dir: String, testBinaryCompatibility: Boolean = false): Project = {
    val bcSettings: Seq[Setting[_]] = if (testBinaryCompatibility) {
      mimaDefaultSettings ++ Seq(previousArtifact := Some(buildOrganization % StringUtilities.normalize(name) % previousVersion))
    } else Nil
    Project(name, file("src/" + dir))
      .configs(PerformanceTest)
      .settings(inConfig(PerformanceTest)(Defaults.testTasks) : _*)
      .settings(playCommonSettings: _*)
      .settings((if (publishNonCoreScalaLibraries) publishSettings else dontPublishSettings): _*)
      .settings(bcSettings: _*)
      .settings(
        scalaVersion := defaultScalaVersion,
        scalaBinaryVersion := CrossVersion.binaryScalaVersion(defaultScalaVersion),
        autoScalaLibrary := false,
        crossPaths := false
      )
  }

  def PlaySharedRuntimeProject(name: String, dir: String, targetPrefix:String, scalaVersion: SharedProjectScalaVersion, additionalSettings:Seq[Setting[_]]): Project = {
    Project(name, file("src/" + dir))
      .settings(playCommonSettings: _*)
      .settings(defaultScalariformSettings: _*)
      .settings(scalaVersion.toSettings(targetPrefix): _*)
      .settings(additionalSettings: _*)
  }

  /**
   * A project that is in the Play runtime
   */
  def PlayRuntimeProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .configs(PerformanceTest)
      .settings(inConfig(PerformanceTest)(Defaults.testTasks) : _*)
      .settings(playCommonSettings: _*)
      .settings(publishSettings: _*)
      .settings(mimaDefaultSettings: _*)
      .settings(defaultScalariformSettings: _*)
      .settings(playRuntimeSettings(name): _*)
  }

  def playRuntimeSettings(name: String): Seq[Setting[_]] = Seq(
    previousArtifact := Some(buildOrganization %
      (StringUtilities.normalize(name) + "_" + CrossVersion.binaryScalaVersion(buildScalaVersion)) % previousVersion),
    scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature"),
    Docs.apiDocsInclude := true
  )

  def PlaySbtProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .settings(playCommonSettings: _*)
      .settings((if (publishNonCoreScalaLibraries) publishSettings else dontPublishSettings): _*)
      .settings(defaultScalariformSettings: _*)
      .settings(
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

  def runSupportProject(prefix:String, sv:SharedProjectScalaVersion, additionalSettings: Seq[Setting[_]]) =
    PlaySharedRuntimeProject(s"$prefix-${sv.nameSuffix}", s"run-support", prefix, sv, additionalSettings).settings(
      libraryDependencies ++= runSupportDependencies
    )

  lazy val SbtRunSupportProject = runSupportProject("SBT-Run-Support",SharedProjectScalaVersion.forScalaVersion(buildScalaVersionForSbt),(if (publishNonCoreScalaLibraries) publishSettings else dontPublishSettings))

  lazy val RunSupportProject = runSupportProject("Run-Support", SharedProjectScalaVersion.forScalaVersion(buildScalaVersion),publishSettings)

  lazy val RoutesCompilerProject = PlaySbtProject("Routes-Compiler", "routes-compiler")
    .settings(libraryDependencies ++= routersCompilerDependencies)

  lazy val AnormProject = PlayRuntimeProject("Anorm", "anorm")
    .settings(
      libraryDependencies ++= anormDependencies,
      resolvers += sonatypeSnapshots,
      addScalaModules(scalaParserCombinators)
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
      sourceGenerators in Compile <+= sourceManaged in Compile map PlayVersion(buildScalaVersion),
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

    ).dependsOn(BuildLinkProject, PlayExceptionsProject, IterateesProject % "test->test;compile->compile", JsonProject)

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

  lazy val SbtPluginProject = PlaySbtProject("SBT-Plugin", "sbt-plugin")
    .settings(
      sbtPlugin := true,
      publishMavenStyle := false,
      libraryDependencies ++= sbtDependencies,
      sourceGenerators in Compile <+= sourceManaged in Compile map PlayVersion(buildScalaVersionForSbt),
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
      publishTo := Some(publishingIvyRepository),
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
      scriptedDependencies := {
        val () = publishLocal.value
        val () = (publishLocal in BuildLinkProject).value
        val () = (publishLocal in PlayExceptionsProject).value
        val () = (publishLocal in RoutesCompilerProject).value
        val () = (publishLocal in SbtRunSupportProject).value
        val () = (publishLocal in PlayTestProject).value
        val () = (publishLocal in PlayDocsProject).value
        val () = (publishLocal in PlayProject).value
        val () = (publishLocal in JsonProject).value
        val () = (publishLocal in IterateesProject).value
        val () = (publishLocal in FunctionalProject).value
        val () = (publishLocal in DataCommonsProject).value
      }
    ).dependsOn(BuildLinkProject, PlayExceptionsProject, RoutesCompilerProject, SbtRunSupportProject)

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
    .settings(dontPublishSettings:_*)
    .settings(localRepoCreationSettings:_*)
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

  lazy val PlayDocsSbtPlugin = PlaySbtProject("Play-Docs-SBT-Plugin", "play-docs-sbt-plugin")
    .settings(
      sbtPlugin := true,
      publishMavenStyle := false,
      libraryDependencies ++= playDocsSbtPluginDependencies,
      sbtVersion in GlobalScope := buildSbtVersion,
      sbtBinaryVersion in GlobalScope := buildSbtVersionBinaryCompatible,
      sbtDependency <<= sbtDependency { dep =>
        dep.copy(revision = buildSbtVersion)
      },
      publishTo := Some(publishingIvyRepository),
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
    .settings(playCommonSettings: _*)
    .settings(dontPublishSettings:_*)
    .settings(
      concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
      libraryDependencies ++= (runtime ++ jdbcDeps),
      Docs.apiDocsInclude := false,
      Docs.apiDocsIncludeManaged := false
    )
    .aggregate(publishedProjects: _*)
    .aggregate(RepositoryProject)
}
