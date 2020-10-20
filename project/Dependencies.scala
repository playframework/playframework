/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
import sbt._
import Keys._

import buildinfo.BuildInfo

object Dependencies {
  val akkaVersion: String = sys.props.getOrElse("akka.version", "2.6.8")
  val akkaHttpVersion     = "10.1.12"

  val sslConfig = "com.typesafe" %% "ssl-config-core" % "0.4.2"

  val playJsonVersion = "2.9.1"

  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

  val specs2Version = "4.10.5"
  val specs2Deps = Seq(
    "specs2-core",
    "specs2-junit",
    "specs2-mock"
  ).map("org.specs2" %% _ % specs2Version)

  val specsMatcherExtra = "org.specs2" %% "specs2-matcher-extra" % specs2Version

  val scalacheckDependencies = Seq(
    "org.specs2"     %% "specs2-scalacheck" % specs2Version % Test,
    "org.scalacheck" %% "scalacheck"        % "1.14.3"      % Test
  )

  val jacksonVersion         = "2.10.5"
  val jacksonDatabindVersion = "2.10.5"
  val jacksonDatabind        = Seq("com.fasterxml.jackson.core" % "jackson-databind" % jacksonDatabindVersion)
  val jacksons = Seq(
    "com.fasterxml.jackson.core"     % "jackson-core",
    "com.fasterxml.jackson.core"     % "jackson-annotations",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"
  ).map(_ % jacksonVersion) ++ jacksonDatabind

  val playJson = "com.typesafe.play" %% "play-json" % playJsonVersion

  val slf4jVersion = "1.7.30"
  val slf4j        = Seq("slf4j-api", "jul-to-slf4j", "jcl-over-slf4j").map("org.slf4j" % _ % slf4jVersion)
  val slf4jSimple  = "org.slf4j" % "slf4j-simple" % slf4jVersion

  val guava      = "com.google.guava"         % "guava"        % "29.0-jre"
  val findBugs   = "com.google.code.findbugs" % "jsr305"       % "3.0.2" // Needed by guava
  val mockitoAll = "org.mockito"              % "mockito-core" % "3.5.15"

  val h2database    = "com.h2database"   % "h2"    % "1.4.200"

  val acolyteVersion = "1.0.56"
  val acolyte        = "org.eu.acolyte" % "jdbc-driver" % acolyteVersion

  val jettyAlpnAgent = "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.10"

  def scalaReflect(scalaVersion: String) = "org.scala-lang" % "scala-reflect" % scalaVersion % "provided"
  val scalaJava8Compat                   = "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.1"
  val scalaParserCombinators             = Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2")

  val springFrameworkVersion = "5.2.9.RELEASE"

  val joda = Seq(
    "joda-time" % "joda-time"    % "2.10.6",
    "org.joda"  % "joda-convert" % "2.2.1"
  )

  val junitInterface = "com.novocode" % "junit-interface" % "0.11"
  val junit          = "junit"        % "junit"           % "4.13.1"

  val guiceVersion = "4.2.3"
  val guiceDeps = Seq(
    "com.google.inject"            % "guice"                % guiceVersion,
    "com.google.inject.extensions" % "guice-assistedinject" % guiceVersion
  )

  def runtime(scalaVersion: String) =
    slf4j ++
      Seq("akka-actor", "akka-actor-typed", "akka-slf4j", "akka-serialization-jackson")
        .map("com.typesafe.akka" %% _ % akkaVersion) ++
      Seq("akka-testkit", "akka-actor-testkit-typed")
        .map("com.typesafe.akka" %% _ % akkaVersion % Test) ++
      jacksons ++
      Seq(
        playJson,
        guava,
        "jakarta.transaction" % "jakarta.transaction-api" % "1.3.3",
        "javax.inject"        % "javax.inject"            % "1",
        scalaReflect(scalaVersion),
        scalaJava8Compat,
        sslConfig
      ) ++ scalaParserCombinators ++ specs2Deps.map(_ % Test)

  val nettyVersion = "4.1.53.Final"

  val netty = Seq(
    "com.typesafe.netty" % "netty-reactive-streams-http" % "2.0.5",
    ("io.netty" % "netty-transport-native-epoll" % nettyVersion).classifier("linux-x86_64")
  ) ++ specs2Deps.map(_ % Test)

  val cookieEncodingDependencies = slf4j

  val jimfs = "com.google.jimfs" % "jimfs" % "1.1"

  def routesCompilerDependencies(scalaVersion: String) = {
    specs2Deps.map(_ % Test) ++ Seq(specsMatcherExtra % Test) ++ scalaParserCombinators ++ (logback % Test :: Nil)
  }

  private def sbtPluginDep(moduleId: ModuleID, sbtVersion: String, scalaVersion: String) = {
    Defaults.sbtPluginExtra(
      moduleId,
      CrossVersion.binarySbtVersion(sbtVersion),
      CrossVersion.binaryScalaVersion(scalaVersion)
    )
  }

  val playFileWatch = "com.lightbend.play" %% "play-file-watch" % "1.1.13"

  val typesafeConfig = "com.typesafe" % "config" % "1.4.0"

  def sbtDependencies(sbtVersion: String, scalaVersion: String) = {
    def sbtDep(moduleId: ModuleID) = sbtPluginDep(moduleId, sbtVersion, scalaVersion)

    Seq(
      scalaReflect(scalaVersion),
      typesafeConfig,
      slf4jSimple,
      playFileWatch,
      sbtDep("com.typesafe.sbt"  % "sbt-twirl"           % BuildInfo.sbtTwirlVersion),
      sbtDep("com.typesafe.sbt"  % "sbt-native-packager" % BuildInfo.sbtNativePackagerVersion),
      sbtDep("com.typesafe.sbt"  % "sbt-web"             % "1.4.4"),
      logback             % Test
    ) ++ specs2Deps.map(_ % Test)
  }

  val streamsDependencies = Seq(
    "org.reactivestreams" % "reactive-streams" % "1.0.3",
    "com.typesafe.akka"   %% "akka-stream"     % akkaVersion,
    scalaJava8Compat
  ) ++ specs2Deps.map(_ % Test)

  val playServerDependencies = specs2Deps.map(_ % Test) ++ Seq(
    guava   % Test,
    logback % Test
  )

  val playCacheDeps = specs2Deps.map(_ % Test) :+ logback % Test

  val jcacheApi = Seq(
    "javax.cache" % "cache-api" % "1.1.1"
  )

  val ehcacheVersion = "2.10.6"
  val playEhcacheDeps = Seq(
    "net.sf.ehcache" % "ehcache" % ehcacheVersion,
    "org.ehcache"    % "jcache"  % "1.0.1"
  ) ++ jcacheApi

  val caffeineVersion = "2.8.5"
  val playCaffeineDeps = Seq(
    "com.github.ben-manes.caffeine" % "caffeine" % caffeineVersion,
    "com.github.ben-manes.caffeine" % "jcache"   % caffeineVersion
  ) ++ jcacheApi

  val playWsStandaloneVersion = "2.1.2"
  val playWsDeps = Seq(
    "com.typesafe.play" %% "play-ws-standalone"      % playWsStandaloneVersion,
    "com.typesafe.play" %% "play-ws-standalone-xml"  % playWsStandaloneVersion,
    "com.typesafe.play" %% "play-ws-standalone-json" % playWsStandaloneVersion,
    // Update transitive Akka version as needed:
    "com.typesafe.akka"                        %% "akka-stream" % akkaVersion
  ) ++ (specs2Deps :+ specsMatcherExtra).map(_ % Test) :+ mockitoAll % Test

  // Must use a version of ehcache that supports jcache 1.0.0
  val playAhcWsDeps = Seq(
    "com.typesafe.play"             %% "play-ahc-ws-standalone" % playWsStandaloneVersion,
    "com.typesafe.play"             % "shaded-asynchttpclient"  % playWsStandaloneVersion,
    "com.typesafe.play"             % "shaded-oauth"            % playWsStandaloneVersion,
    "com.github.ben-manes.caffeine" % "jcache"                  % caffeineVersion % Test,
    "net.sf.ehcache"                % "ehcache"                 % ehcacheVersion % Test,
    "org.ehcache"                   % "jcache"                  % "1.0.1" % Test
  ) ++ jcacheApi

  val salvationVersion = "2.7.2"
  val playFilterDeps = Seq(
    "com.shapesecurity" % "salvation" % salvationVersion % Test
  )
}

/*
 * How to use this:
 *    $ sbt -J-XX:+UnlockCommercialFeatures -J-XX:+FlightRecorder -Dakka-http.sources=$HOME/code/akka-http '; project Play-Akka-Http-Server; test:run'
 *
 * Make sure Akka-HTTP has 2.12 as the FIRST version (or that scalaVersion := "2.12.11", otherwise it won't find the artifact
 *    crossScalaVersions := Seq("2.12.11", "2.11.12"),
 */
object AkkaDependency {
  // Needs to be a URI like git://github.com/akka/akka.git#master or file:///xyz/akka
  val akkaSourceDependencyUri   = sys.props.getOrElse("akka-http.sources", "")
  val shouldUseSourceDependency = akkaSourceDependencyUri != ""
  val akkaRepository            = uri(akkaSourceDependencyUri)

  implicit class RichProject(project: Project) {

    /** Adds either a source or a binary dependency, depending on whether the above settings are set */
    def addAkkaModuleDependency(module: String, config: String = ""): Project =
      if (shouldUseSourceDependency) {
        val moduleRef = ProjectRef(akkaRepository, module)
        val withConfig: ClasspathDependency =
          if (config == "") {
            println("  Using Akka-HTTP directly from sources, from: " + akkaSourceDependencyUri)
            moduleRef
          } else moduleRef % config

        project.dependsOn(withConfig)
      } else {
        project.settings(libraryDependencies += {
          val dep = "com.typesafe.akka" %% module % Dependencies.akkaHttpVersion
          if (config == "") dep else dep % config
        })
      }
  }
}
