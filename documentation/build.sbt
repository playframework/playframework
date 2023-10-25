// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

import com.typesafe.play.docs.sbtplugin._
import com.typesafe.play.docs.sbtplugin.Imports._
import de.heikoseeberger.sbtheader.CommentStyle
import de.heikoseeberger.sbtheader.FileType
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderPattern.commentBetween
import de.heikoseeberger.sbtheader.LineCommentCreator
import play.core.PlayVersion
import playbuild.CrossJava
import playbuild.JavaVersion

val DocsApplication = config("docs").hide

lazy val main = Project("Play-Documentation", file("."))
  .enablePlugins(PlayDocsPlugin, SbtTwirl)
  .settings(
    // Avoid the use of deprecated APIs in the docs
    scalacOptions ++= Seq("-deprecation") ++
      (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 13)) => Seq("-Xsource:3")
        case _             => Seq.empty
      }),
    javacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-parameters",
      "-Xlint:unchecked",
      "-Xlint:deprecation",
      "--release",
      "11",
    ),
    ivyConfigurations += DocsApplication,
    // We need to publishLocal playDocs since its jar file is
    // a dependency of `docsJarFile` setting.
    Test / test := (Test / test).dependsOn(playDocs / publishLocal).value,
    resolvers += Resolver
      .sonatypeRepo(
        "releases"
      ), // TODO: Delete this eventually, just needed for lag between deploying to sonatype and getting on maven central
    version := PlayVersion.current,
    libraryDependencies ++= Seq(
      "com.typesafe"   % "config"       % "1.4.2"   % Test,
      "com.h2database" % "h2"           % "2.2.224" % Test,
      "org.mockito"    % "mockito-core" % "5.6.0"   % Test,
      // https://github.com/logstash/logstash-logback-encoder/tree/logstash-logback-encoder-4.9#including
      "net.logstash.logback" % "logstash-logback-encoder" % "7.4" % Test
    ),
    PlayDocsKeys.docsJarFile := Some((playDocs / Compile / packageBin).value),
    PlayDocsKeys.playDocsValidationConfig := PlayDocsValidation.ValidationConfig(
      downstreamWikiPages = Set(
        "JavaEbean",
        "ScalaAnorm",
        "PlaySlick",
        "PlaySlickMigrationGuide",
        "ScalaTestingWithScalaTest",
        "ScalaFunctionalTestingWithScalaTest",
        "ScalaJson",
        "ScalaJsonAutomated",
        "ScalaJsonCombinators",
        "ScalaJsonTransformers",
        // These are not downstream pages, but they were renamed
        // and are still linked in old migration guides.
        "JavaDatabase",
        "ScalaDatabase"
      )
    ),
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
    Test / unmanagedSourceDirectories ++= (baseDirectory.value / "manual" / "detailedTopics" ** "code").get,
    Test / unmanagedResourceDirectories ++= (baseDirectory.value / "manual" / "detailedTopics" ** "code").get,
    // Don't include sbt files in the resources
    Test / unmanagedResources / excludeFilter := (Test / unmanagedResources / excludeFilter).value || "*.sbt",
    crossScalaVersions                        := Seq("2.13.12", "3.3.1"),
    scalaVersion                              := "2.13.12",
    Test / fork                               := true,
    Test / javaOptions ++= Seq("-Xmx512m", "-Xms128m"),
    headerLicense := Some(
      HeaderLicense.Custom(
        "Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>"
      )
    ),
    headerMappings ++= Map(
      FileType.xml                 -> CommentStyle.xmlStyleBlockComment,
      FileType.conf                -> CommentStyle.hashLineComment,
      FileType("sbt")              -> HeaderCommentStyle.cppStyleLineComment,
      FileType("routes")           -> HeaderCommentStyle.hashLineComment,
      FileType("", None, "routes") -> HeaderCommentStyle.hashLineComment,
      FileType("default")          -> HeaderCommentStyle.hashLineComment,
      FileType("properties")       -> HeaderCommentStyle.hashLineComment,
      FileType("js")               -> HeaderCommentStyle.cStyleBlockComment,
      FileType("css")              -> HeaderCommentStyle.cStyleBlockComment,
      FileType("less")             -> HeaderCommentStyle.cStyleBlockComment,
      FileType("md")               -> CommentStyle(new LineCommentCreator("<!---", "-->"), commentBetween("<!---", "*", "-->")),
    ),
    Compile / headerSources ++=
      ((baseDirectory.value ** ("*.default" || "*.properties" || "*.md" || "*.sbt" || "*.routes" || "routes" || "*.js" || "*.less" || "*.css"))
        --- (baseDirectory.value ** "target" ** "*")).get ++
        (baseDirectory.value / "project" ** "*.scala" --- (baseDirectory.value ** "target" ** "*")).get,
    Test / headerSources ++= (baseDirectory.value ** "*.md").get,
    Test / javafmt / sourceDirectories ++= (Test / unmanagedSourceDirectories).value,
    Test / javafmt / sourceDirectories ++= (Test / unmanagedResourceDirectories).value,
    // No need to show eviction warnings for Play documentation.
    update / evictionWarningOptions := EvictionWarningOptions.default
      .withWarnTransitiveEvictions(false)
      .withWarnDirectEvictions(false)
  )
  .dependsOn(
    playDocs,
    playProject("Play")                       % "test",
    playProject("Play-Specs2")                % "test",
    playProject("Play-Java")                  % "test",
    playProject("Play-Java-Forms")            % "test",
    playProject("Play-Java-JPA")              % "test",
    playProject("Play-Guice")                 % "test",
    playProject("Play-Caffeine-Cache")        % "test",
    playProject("Play-AHC-WS")                % "test",
    playProject("Play-OpenID")                % "test",
    playProject("Play-Filters-Helpers")       % "test",
    playProject("Play-JDBC-Evolutions")       % "test",
    playProject("Play-JDBC")                  % "test",
    playProject("Play-Logback")               % "test",
    playProject("Play-Java-JDBC")             % "test",
    playProject("Play-Akka-Http-Server")      % "test",
    playProject("Play-Netty-Server")          % "test",
    playProject("Play-Cluster-Sharding")      % "test",
    playProject("Play-Java-Cluster-Sharding") % "test"
  )

lazy val playDocs = playProject("Play-Docs")

def playProject(name: String) = ProjectRef(Path.fileProperty("user.dir").getParentFile, name)

val _ = sys.props += ("sbt_validateCode" -> List(
  "evaluateSbtFiles",
  "validateDocs",
).mkString(";"))
