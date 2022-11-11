/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt._
import Keys._

import buildinfo.BuildInfo

object Dependencies {
  val akkaVersion: String = sys.props.getOrElse("akka.version", "2.6.20")
  val akkaHttpVersion     = sys.props.getOrElse("akka.http.version", "10.2.10")

  val sslConfig = "com.typesafe" %% "ssl-config-core" % "0.6.1"

  val playJsonVersion = "2.10.0-RC7"

  val logback = "ch.qos.logback" % "logback-classic" % "1.4.4"

  val specs2Version = "4.17.0"
  val specs2CoreDeps = Seq(
    "specs2-core",
    "specs2-junit"
  ).map("org.specs2" %% _ % specs2Version)
  val specs2Deps = specs2CoreDeps ++ Seq(
    "specs2-mock"
  ).map("org.specs2" %% _ % specs2Version)

  val specsMatcherExtra = "org.specs2" %% "specs2-matcher-extra" % specs2Version

  val scalacheckDependencies = Seq(
    "org.specs2"     %% "specs2-scalacheck" % specs2Version % Test,
    "org.scalacheck" %% "scalacheck"        % "1.17.0"      % Test
  )

  val jacksonVersion  = "2.13.4"
  val jacksonDatabind = Seq("com.fasterxml.jackson.core" % "jackson-databind" % "2.13.4.2")
  val jacksons = Seq(
    "com.fasterxml.jackson.core"     % "jackson-core",
    "com.fasterxml.jackson.core"     % "jackson-annotations",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"
  ).map(_ % jacksonVersion) ++ jacksonDatabind
  // Overrides additional jackson deps pulled in by akka-serialization-jackson
  // https://github.com/akka/akka/blob/v2.6.19/project/Dependencies.scala#L129-L137
  // https://github.com/akka/akka/blob/b08a91597e26056d9eea4a216e745805b9052a2a/build.sbt#L257
  // Can be removed as soon as akka upgrades to same jackson version like Play uses
  val akkaSerializationJacksonOverrides = Seq(
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor",
    "com.fasterxml.jackson.module"     % "jackson-module-parameter-names",
    "com.fasterxml.jackson.module"     %% "jackson-module-scala",
  ).map(_ % jacksonVersion)

  val playJson = "com.typesafe.play" %% "play-json" % playJsonVersion

  val slf4jVersion = "2.0.3"
  val slf4j        = Seq("slf4j-api", "jul-to-slf4j", "jcl-over-slf4j").map("org.slf4j" % _ % slf4jVersion)
  val slf4jSimple  = "org.slf4j" % "slf4j-simple" % slf4jVersion

  val guava      = "com.google.guava"         % "guava"        % "31.1-jre"
  val findBugs   = "com.google.code.findbugs" % "jsr305"       % "3.0.2" // Needed by guava
  val mockitoAll = "org.mockito"              % "mockito-core" % "4.8.0"

  val javaxInject = "javax.inject" % "javax.inject" % "1"

  val h2database = "com.h2database" % "h2" % "2.1.214"

  val derbyVersion = "10.15.2.0"
  val derbyDatabase = Seq(
    "org.apache.derby" % "derby",
    "org.apache.derby" % "derbytools"
  ).map(_ % derbyVersion)

  val acolyteVersion = "1.2.1"
  val acolyte        = "org.eu.acolyte" % "jdbc-driver" % acolyteVersion

  val jjwtVersion = "0.11.5"
  val jjwts = Seq(
    "io.jsonwebtoken" % "jjwt-api",
    "io.jsonwebtoken" % "jjwt-impl"
  ).map(_ % jjwtVersion) ++ Seq(
    ("io.jsonwebtoken" % "jjwt-jackson" % jjwtVersion).excludeAll(ExclusionRule("com.fasterxml.jackson.core"))
  )

  val jdbcDeps = Seq(
    "com.zaxxer"         % "HikariCP" % "5.0.1",
    "com.googlecode.usc" % "jdbcdslog" % "1.0.6.2",
    h2database           % Test,
    acolyte              % Test,
    logback              % Test,
    "tyrex"              % "tyrex" % "1.0.1"
  ) ++ specs2Deps.map(_  % Test)

  val jpaDeps = Seq(
    "org.hibernate.javax.persistence" % "hibernate-jpa-2.1-api" % "1.0.2.Final",
    "org.hibernate"                   % "hibernate-core"        % "5.4.32.Final" % "test"
  )

  def scalaReflect(scalaVersion: String) = "org.scala-lang" % "scala-reflect" % scalaVersion % "provided"
  def scalaParserCombinators(scalaVersion: String) =
    Seq("org.scala-lang.modules" %% "scala-parser-combinators" % {
      CrossVersion.partialVersion(scalaVersion) match {
        case Some((2, _)) => "1.1.2"
        case _            => "2.1.1"
      }
    })

  val springFrameworkVersion = "5.3.23"

  val javaDeps = Seq(
    // Used by the Java routing DSL
    "net.jodah"         % "typetools" % "0.6.3"
  ) ++ specs2Deps.map(_ % Test)

  val joda = Seq(
    "joda-time" % "joda-time"    % "2.11.2",
    "org.joda"  % "joda-convert" % "2.2.2"
  )

  val javaFormsDeps = Seq(
    "org.hibernate.validator" % "hibernate-validator" % "6.2.5.Final",
    ("org.springframework" % "spring-context" % springFrameworkVersion)
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

  val junitInterface = "com.github.sbt" % "junit-interface" % "0.13.3"
  val junit          = "junit"          % "junit"           % "4.13.2"

  val javaTestDeps = Seq(
    junit,
    junitInterface,
    "org.easytesting" % "fest-assert" % "1.4",
    mockitoAll,
    logback
  ).map(_ % Test)

  val guiceVersion = "5.1.0"
  val guiceDeps = Seq(
    "com.google.inject"            % "guice"                % guiceVersion,
    "com.google.inject.extensions" % "guice-assistedinject" % guiceVersion
  )

  def runtime(scalaVersion: String) =
    slf4j ++
      Seq("akka-actor", "akka-actor-typed", "akka-slf4j", "akka-serialization-jackson")
        .map("com.typesafe.akka" %% _ % akkaVersion) ++
      Seq("akka-testkit", "akka-actor-testkit-typed", "akka-stream-testkit")
        .map("com.typesafe.akka" %% _ % akkaVersion % Test) ++
      jacksons ++
      akkaSerializationJacksonOverrides ++
      jjwts ++
      Seq(
        playJson,
        guava,
        "jakarta.transaction" % "jakarta.transaction-api" % "2.0.1",
        javaxInject,
        scalaReflect(scalaVersion),
        sslConfig
      ) ++ scalaParserCombinators(scalaVersion) ++ specs2Deps.map(_ % Test) ++ javaTestDeps

  val nettyVersion = "4.1.84.Final"

  val netty = Seq(
    "com.typesafe.netty" % "netty-reactive-streams-http" % "2.0.7",
    ("io.netty" % "netty-transport-native-epoll" % nettyVersion).classifier("linux-x86_64")
  ) ++ specs2Deps.map(_ % Test)

  val akkaHttp = "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion

  val akkaHttp2Support = "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion

  val cookieEncodingDependencies = slf4j

  val jimfs = "com.google.jimfs" % "jimfs" % "1.2"

  val okHttp = "com.squareup.okhttp3" % "okhttp" % "4.10.0"

  def routesCompilerDependencies(scalaVersion: String) = {
    specs2CoreDeps.map(_ % Test) ++ Seq(specsMatcherExtra % Test) ++ scalaParserCombinators(scalaVersion) ++ (logback % Test :: Nil)
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
    Seq(playFileWatch, logback % Test) ++ specs2Deps.map(_ % Test)
  }

  val typesafeConfig = "com.typesafe" % "config" % "1.4.2"

  def sbtDependencies(sbtVersion: String, scalaVersion: String) = {
    def sbtDep(moduleId: ModuleID) = sbtPluginDep(moduleId, sbtVersion, scalaVersion)

    Seq(
      scalaReflect(scalaVersion),
      typesafeConfig,
      slf4jSimple,
      playFileWatch,
      sbtDep("com.typesafe.play" % "sbt-twirl"           % BuildInfo.sbtTwirlVersion),
      sbtDep("com.github.sbt"    % "sbt-native-packager" % BuildInfo.sbtNativePackagerVersion),
      sbtDep("com.typesafe.sbt"  % "sbt-web"             % "1.4.4"),
      sbtDep("com.typesafe.sbt"  % "sbt-js-engine"       % "1.2.3"),
      logback             % Test
    ) ++ specs2Deps.map(_ % Test)
  }

  val playdocWebjarDependencies = Seq(
    "org.webjars" % "jquery"   % "3.6.1"        % "webjars",
    "org.webjars" % "prettify" % "4-Mar-2013-1" % "webjars"
  )

  val playDocVersion = "2.2.0-M1"
  val playDocsDependencies = Seq(
    "com.typesafe.play" %% "play-doc" % playDocVersion
  ) ++ playdocWebjarDependencies

  val streamsDependencies = Seq(
    "org.reactivestreams"   % "reactive-streams" % "1.0.4",
    "com.typesafe.akka"     %% "akka-stream" % akkaVersion,
  ) ++ specs2CoreDeps.map(_ % Test) ++ javaTestDeps

  val playServerDependencies = specs2Deps.map(_ % Test) ++ Seq(
    guava   % Test,
    logback % Test
  )

  val clusterDependencies = Seq(
    "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion
  )

  val fluentleniumVersion = "5.0.4"
  // This is the selenium version compatible with the FluentLenium version declared above.
  // See http://mvnrepository.com/artifact/org.fluentlenium/fluentlenium-core/5.0.4
  val seleniumVersion = "4.1.4"

  val testDependencies = Seq(junit, junitInterface, guava, findBugs, logback) ++ Seq(
    ("org.fluentlenium" % "fluentlenium-core" % fluentleniumVersion).exclude("org.jboss.netty", "netty"),
    // htmlunit-driver uses an open range to selenium dependencies. This is slightly
    // slowing down the build. So the open range deps were removed and we can re-add
    // them using a specific version. Using an open range is also not good for the
    // local cache.
    ("org.seleniumhq.selenium" % "htmlunit-driver" % "3.64.0").excludeAll(
      ExclusionRule("org.seleniumhq.selenium", "selenium-api"),
      ExclusionRule("org.seleniumhq.selenium", "selenium-support")
    ),
    "org.seleniumhq.selenium"        % "selenium-api" % seleniumVersion,
    "org.seleniumhq.selenium"        % "selenium-support" % seleniumVersion,
    "org.seleniumhq.selenium"        % "selenium-firefox-driver" % seleniumVersion
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

  val caffeineVersion = "3.0.6"
  val playCaffeineDeps = Seq(
    "com.github.ben-manes.caffeine" % "caffeine" % caffeineVersion,
    "com.github.ben-manes.caffeine" % "jcache"   % caffeineVersion
  ) ++ jcacheApi

  val playWsStandaloneVersion = "2.2.0-M2"
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

  val playDocsSbtPluginDependencies = Seq(
    "com.typesafe.play" %% "play-doc" % playDocVersion
  )

  val salvationVersion = "2.7.2"
  val playFilterDeps = Seq(
    "com.shapesecurity" % "salvation" % salvationVersion % Test
  )
}
