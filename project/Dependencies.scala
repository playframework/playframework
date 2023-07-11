/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
import sbt._
import Keys._

import buildinfo.BuildInfo

object Dependencies {
  val akkaVersion: String = sys.props.getOrElse("akka.version", "2.6.21")
  val akkaHttpVersion     = sys.props.getOrElse("akka.http.version", "10.1.15")

  val sslConfig = "com.typesafe" %% "ssl-config-core" % "0.4.3"

  val playJsonVersion = "2.8.2"

  val logback = "ch.qos.logback" % "logback-classic" % "1.2.12"

  val specs2Version = "4.8.3"
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

  // We need to use an older version of specs2 for sbt
  // because we need Scala 2.10 support (sbt 0.13).
  val specs2VersionForSbt     = "3.10.0"
  val specs2DepsForSbt        = specs2Deps.map(_.withRevision(specs2VersionForSbt))
  val specsMatcherExtraForSbt = specsMatcherExtra.withRevision(specs2VersionForSbt)

  val jacksonVersion         = "2.11.4"
  val jacksonDatabindVersion = jacksonVersion

  val jacksonDatabind = Seq("com.fasterxml.jackson.core" % "jackson-databind" % jacksonDatabindVersion)
  val jacksons = Seq(
    "com.fasterxml.jackson.core"     % "jackson-core",
    "com.fasterxml.jackson.core"     % "jackson-annotations",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"
  ).map(_ % jacksonVersion) ++ jacksonDatabind

  val playJson = "com.typesafe.play" %% "play-json" % playJsonVersion

  val slf4jVersion = "1.7.36"
  val slf4j        = Seq("slf4j-api", "jul-to-slf4j", "jcl-over-slf4j").map("org.slf4j" % _ % slf4jVersion)
  val slf4jSimple  = "org.slf4j" % "slf4j-simple" % slf4jVersion

  val guava      = "com.google.guava"         % "guava"        % "30.1.1-jre"
  val findBugs   = "com.google.code.findbugs" % "jsr305"       % "3.0.2" // Needed by guava
  val mockitoAll = "org.mockito"              % "mockito-core" % "3.2.4"

  val h2database    = "com.h2database"   % "h2"    % "1.4.200"
  val derbyDatabase = "org.apache.derby" % "derby" % "10.13.1.1"

  val acolyteVersion = "1.0.57"
  val acolyte        = "org.eu.acolyte" % "jdbc-driver" % acolyteVersion

  val jettyAlpnAgent = "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.10"

  val jjwt = "io.jsonwebtoken" % "jjwt" % "0.9.1"
  // currently jjwt needs the JAXB Api package in JDK 9+
  // since it actually uses javax/xml/bind/DatatypeConverter
  // See: https://github.com/jwtk/jjwt/issues/317
  val jaxbApi = "jakarta.xml.bind" % "jakarta.xml.bind-api" % "2.3.3"

  val jdbcDeps = Seq(
    "com.zaxxer"         % "HikariCP"  % "3.4.5",
    "com.googlecode.usc" % "jdbcdslog" % "1.0.6.2",
    h2database           % Test,
    acolyte              % Test,
    logback              % Test,
    "tyrex"              % "tyrex"     % "1.0.1"
  ) ++ specs2Deps.map(_ % Test)

  val jpaDeps = Seq(
    "org.hibernate.javax.persistence" % "hibernate-jpa-2.1-api" % "1.0.2.Final",
    "org.hibernate"                   % "hibernate-core"        % "5.4.32.Final" % "test"
  )

  def scalaReflect(scalaVersion: String) = "org.scala-lang" % "scala-reflect" % scalaVersion % "provided"
  def scalaJava8Compat(scalaVersion: String) =
    "org.scala-lang.modules" %% "scala-java8-compat" % (CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, major)) if major >= 13 => "1.0.2"
      case _                               => "0.9.1"
    })
  def scalaParserCombinators(scalaVersion: String) = CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, major)) if major >= 11 => Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2")
    case _                               => Nil
  }

  val springFrameworkVersion = "5.2.22.RELEASE"

  def javaDeps(scalaVersion: String) =
    Seq(
      scalaJava8Compat(scalaVersion),
      // Used by the Java routing DSL
      "net.jodah" % "typetools" % "0.5.0"
    ) ++ specs2Deps.map(_ % Test)

  val joda = Seq(
    "joda-time" % "joda-time"    % "2.10.14",
    "org.joda"  % "joda-convert" % "2.2.2"
  )

  val javaFormsDeps = Seq(
    "org.hibernate.validator" % "hibernate-validator" % "6.1.7.Final",
    ("org.springframework"    % "spring-context"      % springFrameworkVersion)
      .exclude("org.springframework", "spring-aop")
      .exclude("org.springframework", "spring-beans")
      .exclude("org.springframework", "spring-core")
      .exclude("org.springframework", "spring-expression")
      .exclude("org.springframework", "spring-asm"),
    ("org.springframework" % "spring-core" % springFrameworkVersion)
      .exclude("org.springframework", "spring-asm")
      .exclude("org.springframework", "spring-jcl")
      .exclude("commons-logging", "commons-logging"),
    ("org.springframework" % "spring-beans" % springFrameworkVersion)
      .exclude("org.springframework", "spring-core")
  ) ++ specs2Deps.map(_ % Test)

  val junitInterface = "com.novocode" % "junit-interface" % "0.11"
  val junit          = "junit"        % "junit"           % "4.13.2"

  val javaTestDeps = Seq(
    junit,
    junitInterface,
    "org.easytesting" % "fest-assert" % "1.4",
    mockitoAll,
    logback
  ).map(_ % Test)

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
        jjwt,
        jaxbApi,
        "jakarta.transaction" % "jakarta.transaction-api" % "1.3.3",
        "javax.inject"        % "javax.inject"            % "1",
        scalaReflect(scalaVersion),
        scalaJava8Compat(scalaVersion),
        sslConfig
      ) ++ scalaParserCombinators(scalaVersion) ++ specs2Deps.map(_ % Test) ++ javaTestDeps

  val nettyVersion = "4.1.87.Final"

  val netty = Seq(
    "com.typesafe.netty" % "netty-reactive-streams-http"  % "2.0.8",
    ("io.netty"          % "netty-transport-native-epoll" % nettyVersion).classifier("linux-x86_64")
  ) ++ specs2Deps.map(_ % Test)

  val cookieEncodingDependencies = slf4j

  val jimfs = "com.google.jimfs" % "jimfs" % "1.1"

  val okHttp = "com.squareup.okhttp3" % "okhttp" % "4.2.2"

  def routesCompilerDependencies(scalaVersion: String) = {
    val deps = CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, v)) if v >= 12 => specs2Deps.map(_ % Test) ++ Seq(specsMatcherExtra % Test)
      case _                       => specs2DepsForSbt.map(_ % Test) ++ Seq(specsMatcherExtraForSbt % Test)
    }
    deps ++ scalaParserCombinators(scalaVersion) ++ (logback % Test :: Nil)
  }

  private def sbtPluginDep(moduleId: ModuleID, sbtVersion: String, scalaVersion: String) = {
    Defaults.sbtPluginExtra(
      moduleId,
      CrossVersion.binarySbtVersion(sbtVersion),
      CrossVersion.binaryScalaVersion(scalaVersion)
    )
  }

  val playFileWatch = "com.lightbend.play" %% "play-file-watch" % "1.1.16"

  def runSupportDependencies(sbtVersion: String): Seq[ModuleID] = {
    (CrossVersion.binarySbtVersion(sbtVersion) match {
      case "1.0"  => specs2Deps.map(_ % Test)
      case "0.13" => specs2DepsForSbt.map(_ % Test)
    }) ++ Seq(
      playFileWatch,
      logback % Test
    )
  }

  val typesafeConfig = "com.typesafe" % "config" % "1.4.2"

  def sbtDependencies(sbtVersion: String, scalaVersion: String) = {
    def sbtDep(moduleId: ModuleID) = sbtPluginDep(moduleId, sbtVersion, scalaVersion)

    Seq(
      scalaReflect(scalaVersion),
      typesafeConfig,
      slf4jSimple,
      playFileWatch,
      sbtDep("com.typesafe.sbt"  % "sbt-twirl"           % BuildInfo.sbtTwirlVersion),
      sbtDep("com.typesafe.sbt"  % "sbt-native-packager" % BuildInfo.sbtNativePackagerVersion),
      sbtDep("com.lightbend.sbt" % "sbt-javaagent"       % BuildInfo.sbtJavaAgentVersion),
      sbtDep("com.typesafe.sbt"  % "sbt-web"             % "1.4.4"),
      sbtDep("com.typesafe.sbt"  % "sbt-js-engine"       % "1.2.3")
    ) ++ (CrossVersion.binarySbtVersion(sbtVersion) match {
      case "1.0"  => specs2Deps.map(_ % Test)
      case "0.13" => specs2DepsForSbt.map(_ % Test)
    }) :+ logback % Test
  }

  val playdocWebjarDependencies = Seq(
    "org.webjars" % "jquery"   % "3.6.0"        % "webjars",
    "org.webjars" % "prettify" % "4-Mar-2013-1" % "webjars"
  )

  val playDocVersion = "2.1.0"
  val playDocsDependencies = Seq(
    "com.typesafe.play" %% "play-doc" % playDocVersion
  ) ++ playdocWebjarDependencies

  def streamsDependencies(scalaVersion: String) =
    Seq(
      "org.reactivestreams" % "reactive-streams" % "1.0.3",
      "com.typesafe.akka"  %% "akka-stream"      % akkaVersion,
      scalaJava8Compat(scalaVersion)
    ) ++ specs2Deps.map(_ % Test) ++ javaTestDeps

  val playServerDependencies = specs2Deps.map(_ % Test) ++ Seq(
    guava   % Test,
    logback % Test
  )

  val clusterDependencies = Seq(
    "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion
  )

  val fluentleniumVersion = "3.7.1"
  // This is the selenium version compatible with the FluentLenium version declared above.
  // See http://mvnrepository.com/artifact/org.fluentlenium/fluentlenium-core/3.5.2
  val seleniumVersion = "3.141.59"

  val testDependencies = Seq(junit, junitInterface, guava, findBugs, logback) ++ Seq(
    ("org.fluentlenium" % "fluentlenium-core" % fluentleniumVersion).exclude("org.jboss.netty", "netty"),
    // htmlunit-driver uses an open range to selenium dependencies. This is slightly
    // slowing down the build. So the open range deps were removed and we can re-add
    // them using a specific version. Using an open range is also not good for the
    // local cache.
    ("org.seleniumhq.selenium" % "htmlunit-driver" % "2.36.0").excludeAll(
      ExclusionRule("org.seleniumhq.selenium", "selenium-api"),
      ExclusionRule("org.seleniumhq.selenium", "selenium-support")
    ),
    "org.seleniumhq.selenium" % "selenium-api"            % seleniumVersion,
    "org.seleniumhq.selenium" % "selenium-support"        % seleniumVersion,
    "org.seleniumhq.selenium" % "selenium-firefox-driver" % seleniumVersion
  ) ++ guiceDeps ++ specs2Deps.map(_ % Test)

  val playCacheDeps = specs2Deps.map(_ % Test) :+ logback % Test

  val jcacheApi = Seq(
    "javax.cache" % "cache-api" % "1.1.1"
  )

  val ehcacheVersion = "2.10.9.2"
  val playEhcacheDeps = Seq(
    "net.sf.ehcache" % "ehcache" % ehcacheVersion,
    "org.ehcache"    % "jcache"  % "1.0.1"
  ) ++ jcacheApi

  val caffeineVersion = "2.8.8"
  val playCaffeineDeps = Seq(
    "com.github.ben-manes.caffeine" % "caffeine" % caffeineVersion,
    "com.github.ben-manes.caffeine" % "jcache"   % caffeineVersion
  ) ++ jcacheApi

  val playWsStandaloneVersion = "2.1.10"
  val playWsDeps = Seq(
    "com.typesafe.play" %% "play-ws-standalone"      % playWsStandaloneVersion,
    "com.typesafe.play" %% "play-ws-standalone-xml"  % playWsStandaloneVersion,
    "com.typesafe.play" %% "play-ws-standalone-json" % playWsStandaloneVersion
  ) ++ (specs2Deps :+ specsMatcherExtra).map(_ % Test) :+ mockitoAll % Test

  // Must use a version of ehcache that supports jcache 1.0.0
  val playAhcWsDeps = Seq(
    "com.typesafe.play"            %% "play-ahc-ws-standalone" % playWsStandaloneVersion,
    "com.typesafe.play"             % "shaded-asynchttpclient" % playWsStandaloneVersion,
    "com.typesafe.play"             % "shaded-oauth"           % playWsStandaloneVersion,
    "com.github.ben-manes.caffeine" % "jcache"                 % caffeineVersion % Test,
    "net.sf.ehcache"                % "ehcache"                % ehcacheVersion  % Test,
    "org.ehcache"                   % "jcache"                 % "1.0.1"         % Test
  ) ++ jcacheApi

  val playDocsSbtPluginDependencies = Seq(
    "com.typesafe.play" %% "play-doc" % playDocVersion
  )

  val salvationVersion = "2.7.2"
  val playFilterDeps = Seq(
    "com.shapesecurity" % "salvation" % salvationVersion % Test
  )
}

/*
 * How to use this:
 *    $ sbt -J-XX:+UnlockCommercialFeatures -J-XX:+FlightRecorder -Dakka-http.sources=$HOME/code/akka-http '; project Play-Akka-Http-Server; test:run'
 *
 * Make sure Akka-HTTP has 2.12 as the FIRST version (or that scalaVersion := "2.12.16", otherwise it won't find the artifact
 *    crossScalaVersions := Seq("2.12.16", "2.11.12"),
 */
object AkkaDependency {
  // Needs to be a URI like git://github.com/akka/akka.git#main or file:///xyz/akka
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
