import sbt._
import Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact

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

  val buildOrganization = "play"
  val buildVersion = propOr("play.version", "2.2-SNAPSHOT")
  val buildWithDoc = boolProp("generate.doc")
  val previousVersion = "2.1.0"
  val buildScalaVersion = propOr("scala.version", "2.10.0")
  // TODO - Try to compute this from SBT...
  val buildScalaVersionForSbt = propOr("play.sbt.scala.version", "2.9.2")
  val buildSbtVersion = propOr("play.sbt.version", "0.12.2")
  val buildSbtMajorVersion = "0.12"
  val buildSbtVersionBinaryCompatible = "0.12"

  val playCommonSettings = Seq(
    organization := buildOrganization,
    version := buildVersion,
    scalaVersion := buildScalaVersion,
    scalaBinaryVersion := CrossVersion.binaryScalaVersion(buildScalaVersion),
    ivyLoggingLevel := UpdateLogging.DownloadOnly,
    publishTo := Some(playRepository),
    javacOptions ++= Seq("-source", "1.6", "-target", "1.6", "-encoding", "UTF-8"),
    javacOptions in doc := Seq("-source", "1.6"),
    resolvers += typesafe)

  def PlaySharedJavaProject(name: String, dir: String, testBinaryCompatibility: Boolean = false): Project = {
    val bcSettings: Seq[Setting[_]] = if (testBinaryCompatibility) {
      mimaDefaultSettings ++ Seq(previousArtifact := Some("play" % name % previousVersion))
    } else Nil
    Project(name, file("src/" + dir))
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
      .settings(playCommonSettings: _*)
      .settings(mimaDefaultSettings: _*)
      .settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)
      .settings(playRuntimeSettings(name): _*)
  }

  def playRuntimeSettings(name: String): Seq[Setting[_]] = Seq(
    previousArtifact := Some("play" %% name % previousVersion),
    scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature"),
    publishArtifact in packageDoc := buildWithDoc,
    publishArtifact in (Compile, packageSrc) := true)

  def PlaySbtProject(name: String, dir: String): Project = {
    Project(name, file("src/" + dir))
      .settings(playCommonSettings: _*)
      .settings(com.typesafe.sbtscalariform.ScalariformPlugin.defaultScalariformSettings: _*)
      .settings(
        scalaVersion := buildScalaVersionForSbt,
        scalaBinaryVersion := CrossVersion.binaryScalaVersion(buildScalaVersionForSbt),
        publishTo := Some(playRepository),
        publishArtifact in packageDoc := false,
        publishArtifact in (Compile, packageSrc) := false,
        scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked"))

  }

}

object Resolvers {

  import BuildSettings._

  val playLocalRepository = Resolver.file("Play Local Repository", file("../repository/local"))(Resolver.ivyStylePatterns)
  val typesafe = "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
  val typesafeReleases = "Typesafe Releases Repository" at "https://typesafe.artifactoryonline.com/typesafe/maven-releases/"
  val typesafeSnapshot = "Typesafe Snapshots Repository" at "https://typesafe.artifactoryonline.com/typesafe/maven-snapshots/"
  val playRepository = if (buildVersion.endsWith("SNAPSHOT")) typesafeSnapshot else typesafeReleases
  val typesafeIvyReleases = Resolver.url("Typesafe Ivy Releases Repository", url("https://typesafe.artifactoryonline.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)
  val typesafeIvySnapshot = Resolver.url("Typesafe Ivy Snapshots Repository", url("https://typesafe.artifactoryonline.com/typesafe/ivy-snapshots/"))(Resolver.ivyStylePatterns)
  val playIvyRepository = if (buildVersion.endsWith("SNAPSHOT")) typesafeIvySnapshot else typesafeIvyReleases
}


object PlayBuild extends Build {

  import Resolvers._
  import Dependencies._
  import BuildSettings._
  import Generators._
  import LocalSBT._
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
      unmanagedJars in Compile <+= (baseDirectory) map {
        b => compilerJar(b / "../..")
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
      parallelExecution in Test := false,
      sourceGenerators in Compile <+= (dependencyClasspath in TemplatesCompilerProject in Runtime, packageBin in TemplatesCompilerProject in Compile, scalaSource in Compile, sourceManaged in Compile, streams) map ScalaTemplates
    ).dependsOn(SbtLinkProject, PlayExceptionsProject, TemplatesProject, IterateesProject, JsonProject)

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

  lazy val PlayJavaProject = PlayRuntimeProject("Play-Java", "play-java")
    .settings(libraryDependencies := javaDeps)
    .dependsOn(PlayProject)

  lazy val PlayTestProject = PlayRuntimeProject("Play-Test", "play-test")
    .settings(
      libraryDependencies := testDependencies,
      parallelExecution in Test := false
    ).dependsOn(PlayProject)

  lazy val SbtPluginProject = PlaySbtProject("SBT-Plugin", "sbt-plugin")
    .settings(
      sbtPlugin := true,
      publishMavenStyle := false,
      libraryDependencies := sbtDependencies,
      libraryDependencies += "com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.1" extra("sbtVersion" -> buildSbtVersionBinaryCompatible, "scalaVersion" -> buildScalaVersionForSbt),
      libraryDependencies += "com.typesafe.sbtidea" % "sbt-idea" % "1.1.1" extra("sbtVersion" -> buildSbtVersionBinaryCompatible, "scalaVersion" -> buildScalaVersionForSbt),
      libraryDependencies += "org.specs2" %% "specs2" % "1.12.3" % "test" exclude("javax.transaction", "jta"),
      unmanagedJars in Compile <++= (baseDirectory) map {
        b => sbtJars(b / "../..")
      },
      publishTo := Some(playIvyRepository)
    ).dependsOn(SbtLinkProject, PlayExceptionsProject, RoutesCompilerProject, TemplatesCompilerProject, ConsoleProject)

  // todo this can be 2.10
  lazy val ConsoleProject = PlaySbtProject("Console", "console")
    .settings(
      libraryDependencies := consoleDependencies,
      sourceGenerators in Compile <+= sourceManaged in Compile map PlayVersion,
      unmanagedJars in Compile <++= (baseDirectory) map {
        b => sbtJars(b / "../..")
      }
    )

  lazy val PlayFiltersHelpersProject = PlayRuntimeProject("Filters-Helpers", "play-filters-helpers")
    .dependsOn(PlayProject)

  val Root = Project(
    "Root",
    file("."))
    .settings(playCommonSettings: _*)
    .settings(
      libraryDependencies := (runtime ++ jdbcDeps),
      cleanFiles ++= Seq(file("../dist"), file("../repository/local")),
      resetRepositoryTask,
      buildRepositoryTask,
      distTask,
      generateAPIDocsTask,
      publish := {}
    ).aggregate(
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
    PlayProject,
    PlayJdbcProject,
    PlayJavaProject,
    PlayJavaJdbcProject,
    PlayEbeanProject,
    PlayJpaProject,
    SbtPluginProject,
    ConsoleProject,
    PlayTestProject,
    PlayExceptionsProject,
    PlayFiltersHelpersProject
  )

  object LocalSBT {

    import BuildSettings._

    def isJar(f: java.io.File) = f.getName.endsWith(".jar")

    def sbtJars(baseDirectory: File): Seq[java.io.File] = {
      (baseDirectory / ("sbt/boot/scala-" + buildScalaVersionForSbt + "/org.scala-sbt/sbt/" + buildSbtVersion)).listFiles.filter(isJar) ++
        (baseDirectory / ("sbt/boot/scala-" + buildScalaVersionForSbt + "/org.scala-sbt/sbt/" + buildSbtVersion + "/xsbti")).listFiles.filter(isJar) ++
        Seq(baseDirectory / ("sbt/boot/scala-" + buildScalaVersionForSbt + "/lib/jline.jar"))
    }

    def compilerJar(baseDirectory: File): java.io.File = {
      baseDirectory / ("sbt/boot/scala-" + buildScalaVersionForSbt + "/lib/scala-compiler.jar")
    }
  }

}
