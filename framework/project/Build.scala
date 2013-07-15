import sbt._
import Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact
import com.typesafe.sbt.SbtScalariform.defaultScalariformSettings

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

  val experimental = Option(System.getProperty("experimental")).filter(_ == "true").map(_ => true).getOrElse(false)

  val buildOrganization = "com.typesafe.play"
  val buildVersion = propOr("play.version", "2.2-SNAPSHOT")
  val buildWithDoc = boolProp("generate.doc")
  val previousVersion = "2.1.0"
  val buildScalaVersion = propOr("scala.version", "2.10.0")
  // TODO - Try to compute this from SBT... or not.
  val buildScalaVersionForSbt = propOr("play.sbt.scala.version", "2.10.2")
  val buildScalaBinaryVersionForSbt = CrossVersion.binaryScalaVersion(buildScalaVersionForSbt)
  val buildSbtVersion = propOr("play.sbt.version", "0.13.0-RC1")
  val buildSbtMajorVersion = "0.13"
  val buildSbtVersionBinaryCompatible = CrossVersion.binarySbtVersion(buildSbtVersion)
  // Used by api docs generation to link back to the correct branch on GitHub, only when version is a SNAPSHOT
  val sourceCodeBranch = propOr("git.branch", "master")

  lazy val PerformanceTest = config("pt") extend(Test)

  val playCommonSettings = Seq(
    organization := buildOrganization,
    version := buildVersion,
    scalaVersion := buildScalaVersion,
    scalaBinaryVersion := CrossVersion.binaryScalaVersion(buildScalaVersion),
    ivyLoggingLevel := UpdateLogging.DownloadOnly,
    publishTo := Some(publishingMavenRepository),
    javacOptions ++= Seq("-source", "1.6", "-target", "1.6", "-encoding", "UTF-8", "-Xlint:-options"),
    javacOptions in doc := Seq("-source", "1.6"),
    resolvers ++= typesafeResolvers,
    fork in Test := true,
    testOptions in Test += Tests.Filter(!_.endsWith("Benchmark")),
    testOptions in PerformanceTest ~= (_.filterNot(_.isInstanceOf[Tests.Filter]) :+ Tests.Filter(_.endsWith("Benchmark"))),
    parallelExecution in PerformanceTest := false
  )

  def PlaySharedJavaProject(name: String, dir: String, testBinaryCompatibility: Boolean = false): Project = {
    val bcSettings: Seq[Setting[_]] = if (testBinaryCompatibility) {
      mimaDefaultSettings ++ Seq(previousArtifact := Some(buildOrganization % name % previousVersion))
    } else Nil
    Project(name, file("src/" + dir))
      .configs(PerformanceTest)
      .settings(inConfig(PerformanceTest)(Defaults.testTasks) : _*)
      .settings(playCommonSettings: _*)
      .settings(bcSettings: _*)
      .settings(
        autoScalaLibrary := false,
        crossPaths := false,
        publishArtifact in packageDoc := buildWithDoc,
        publishArtifact in (Compile, packageSrc) := true)
  }

  def PlayRuntimeProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .configs(PerformanceTest)
      .settings(inConfig(PerformanceTest)(Defaults.testTasks) : _*)
      .settings(playCommonSettings: _*)
      .settings(mimaDefaultSettings: _*)
      .settings(defaultScalariformSettings: _*)
      .settings(playRuntimeSettings(name): _*)
  }

  def playRuntimeSettings(name: String): Seq[Setting[_]] = Seq(
    previousArtifact := Some(buildOrganization %% name % previousVersion),
    scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature"),
    publishArtifact in packageDoc := buildWithDoc,
    publishArtifact in (Compile, packageSrc) := true,
    ApiDocs.apiDocsInclude := true
  )

  def PlaySbtProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .settings(playCommonSettings: _*)
      .settings(defaultScalariformSettings: _*)
      .settings(
        scalaVersion := buildScalaVersionForSbt,
        scalaBinaryVersion := CrossVersion.binaryScalaVersion(buildScalaVersionForSbt),
        publishTo := Some(publishingMavenRepository),
        publishArtifact in packageDoc := false,
        publishArtifact in (Compile, packageSrc) := false,
        scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked"))

  }
}

object Resolvers {

  import BuildSettings._

  val typesafeReleases = "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/"
  val typesafeSnapshots = "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
  val typesafeIvyReleases = Resolver.url("Typesafe Ivy Releases Repository", url("http://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)
  val typesafeIvySnapshots = Resolver.url("Typesafe Ivy Snapshots Repository", url("http://repo.typesafe.com/typesafe/ivy-snapshots"))(Resolver.ivyStylePatterns)
  val publishTypesafeMavenReleases = "Typesafe Maven Releases Repository for publishing" at "https://typesafe.artifactoryonline.com/typesafe/maven-releases/"
  val publishTypesafeMavenSnapshots = "Typesafe Maven Snapshots Repository for publishing" at "https://typesafe.artifactoryonline.com/typesafe/maven-snapshots/"
  val publishTypesafeIvyReleases = Resolver.url("Typesafe Ivy Releases Repository for publishing", url("https://typesafe.artifactoryonline.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)
  val publishTypesafeIvySnapshots = Resolver.url("Typesafe Ivy Snapshots Repository for publishing", url("https://typesafe.artifactoryonline.com/typesafe/ivy-snapshots/"))(Resolver.ivyStylePatterns)

  val sonatypeSnapshots = "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

  val isSnapshotBuild = buildVersion.endsWith("SNAPSHOT")
  val typesafeResolvers = if (isSnapshotBuild) Seq(typesafeReleases, typesafeSnapshots) else Seq(typesafeReleases)
  val publishingMavenRepository = if (isSnapshotBuild) publishTypesafeMavenSnapshots else publishTypesafeMavenReleases
  val publishingIvyRepository = if (isSnapshotBuild) publishTypesafeIvySnapshots else publishTypesafeIvyReleases
}


object PlayBuild extends Build {

  import Resolvers._
  import Dependencies._
  import BuildSettings._
  import Generators._
  import Tasks._

  lazy val SbtLinkProject = PlaySharedJavaProject("SBT-link", "sbt-link")
    .settings(libraryDependencies := link)

  lazy val TemplatesProject = PlayRuntimeProject("Templates", "templates")
    .settings(libraryDependencies := templatesDependencies)

  lazy val RoutesCompilerProject = PlaySbtProject("Routes-Compiler", "routes-compiler")
    .settings(libraryDependencies := routersCompilerDependencies)

  lazy val TemplatesCompilerProject = PlaySbtProject("Templates-Compiler", "templates-compiler")
    .settings(
      libraryDependencies := templatesCompilerDependencies,
      libraryDependencies <+= scalaVersion apply { sv =>
        "org.scala-lang" % "scala-compiler" % sv
      }
    )

  lazy val AnormProject = PlayRuntimeProject("Anorm", "anorm")

  lazy val IterateesProject = PlayRuntimeProject("Play-Iteratees", "iteratees")
    .settings(libraryDependencies := iterateesDependencies)

  lazy val FunctionalProject = PlayRuntimeProject("Play-Functional", "play-functional")

  lazy val DataCommonsProject = PlayRuntimeProject("Play-DataCommons", "play-datacommons")

  lazy val JsonProject = PlayRuntimeProject("Play-Json", "play-json")
    .settings(libraryDependencies := jsonDependencies)
    .dependsOn(IterateesProject, FunctionalProject, DataCommonsProject)

  lazy val PlayExceptionsProject = PlaySharedJavaProject("Play-Exceptions", "play-exceptions",
    testBinaryCompatibility = true)

  lazy val PlayProject = PlayRuntimeProject("Play", "play")
    .settings(
      libraryDependencies := runtime,
      sourceGenerators in Compile <+= sourceManaged in Compile map PlayVersion,
      mappings in(Compile, packageSrc) <++= scalaTemplateSourceMappings,
      ApiDocs.apiDocsIncludeManaged := true,
      parallelExecution in Test := false,
      sourceGenerators in Compile <+= (dependencyClasspath in TemplatesCompilerProject in Runtime, packageBin in TemplatesCompilerProject in Compile, scalaSource in Compile, sourceManaged in Compile, streams) map ScalaTemplates
    ).dependsOn(SbtLinkProject, PlayExceptionsProject, TemplatesProject, IterateesProject % "test->test;compile->compile", JsonProject)

  lazy val PlayJdbcProject = PlayRuntimeProject("Play-JDBC", "play-jdbc")
    .settings(libraryDependencies := jdbcDeps)
    .dependsOn(PlayProject)

  lazy val PlayJavaJdbcProject = PlayRuntimeProject("Play-Java-JDBC", "play-java-jdbc")
    .dependsOn(PlayJdbcProject, PlayJavaProject)

  lazy val PlayEbeanProject = PlayRuntimeProject("Play-Java-Ebean", "play-java-ebean")
    .settings(
      libraryDependencies := ebeanDeps ++ jpaDeps,
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
    .settings(libraryDependencies := jpaDeps)
    .dependsOn(PlayJavaJdbcProject)

  lazy val PlayTestProject = PlayRuntimeProject("Play-Test", "play-test")
    .settings(
      libraryDependencies := testDependencies,
      parallelExecution in Test := false
    ).dependsOn(PlayProject)

  lazy val PlayJavaProject = PlayRuntimeProject("Play-Java", "play-java")
    .settings(libraryDependencies := javaDeps)
    .dependsOn(PlayProject)
    .dependsOn(PlayTestProject % "test")
  
  import ScriptedPlugin._

  lazy val SbtPluginProject = PlaySbtProject("SBT-Plugin", "sbt-plugin")
    .settings(
      sbtPlugin := true,
      publishMavenStyle := false,
      libraryDependencies := sbtDependencies,
      // Remove once there are releases for IDE plugins against 0.13
      resolvers += sonatypeSnapshots,
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
      publishTo := Some(publishingIvyRepository)
    ).settings(scriptedSettings: _*)
    .settings(
      scriptedLaunchOpts <++= (baseDirectory in ThisBuild) { baseDir =>
        Seq(
          "-Dsbt.ivy.home=" + new File(baseDir.getParent, "repository"),
          "-Dsbt.boot.directory=" + new File(baseDir, "sbt/boot"),
          "-Dplay.home=" + System.getProperty("play.home"),
          "-XX:MaxPermSize=384M",
          "-Dperformance.log=" + new File(baseDir, "target/sbt-repcomile-performance.properties")
       )
      }
    ).dependsOn(SbtLinkProject, PlayExceptionsProject, RoutesCompilerProject, TemplatesCompilerProject, ConsoleProject)

  lazy val ConsoleProject = PlaySbtProject("Console", "console")
    .settings(
      resolvers += typesafeIvyReleases,
      libraryDependencies := consoleDependencies,
      sourceGenerators in Compile <+= sourceManaged in Compile map PlayVersion
    )

  lazy val PlayFiltersHelpersProject = PlayRuntimeProject("Filters-Helpers", "play-filters-helpers")
    .dependsOn(PlayProject)

  // This project is just for testing Play, not really a public artifact
  lazy val PlayIntegrationTestProject = PlayRuntimeProject("Play-Integration-Test", "play-integration-test")
    .dependsOn(PlayProject, PlayTestProject)

  lazy val PlayCacheProject = PlayRuntimeProject("Play-Cache", "play-cache")
    .settings(libraryDependencies := playCacheDeps)
    .dependsOn(PlayProject)

  import RepositoryBuilder._
  lazy val RepositoryProject = Project(
      "Play-Repository", file("repository"))
    .settings(localRepoCreationSettings:_*)
    .settings(
      localRepoProjectsPublished <<= (publishedProjects map (publishLocal in _)).dependOn,
      addProjectsToRepository(publishedProjects),
      localRepoArtifacts ++= Seq(
        "org.scala-lang" % "scala-compiler" % BuildSettings.buildScalaVersion,
        "org.scala-lang" % "scala-compiler" % BuildSettings.buildScalaVersionForSbt,
        "org.scala-sbt" % "sbt" % BuildSettings.buildSbtVersion
      )
    )
    
  lazy val publishedProjects = Seq[ProjectReference](
    PlayProject,
    SbtLinkProject,
    AnormProject,
    TemplatesProject,
    TemplatesCompilerProject,
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
    SbtPluginProject,
    ConsoleProject,
    PlayTestProject,
    PlayExceptionsProject,
    PlayFiltersHelpersProject,
    PlayIntegrationTestProject
  )
    
  lazy val Root = Project(
    "Root",
    file("."))
    .settings(playCommonSettings: _*)
    .settings(ApiDocs.settings: _*)
    .settings(
      libraryDependencies := (runtime ++ jdbcDeps),
      cleanFiles ++= Seq(file("../dist"), file("../repository/local")),
      publish := {},
      generateDistTask
    )
    .aggregate(publishedProjects: _*)
}
