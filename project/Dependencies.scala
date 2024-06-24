/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt._

import buildinfo.BuildInfo
import Keys._

object Dependencies {
  val pekkoVersion: String = sys.props.getOrElse("pekko.version", "1.0.3")
  val pekkoHttpVersion     = sys.props.getOrElse("pekko.http.version", "1.0.1")

  val sslConfig = "com.typesafe" %% "ssl-config-core" % "0.6.1"

  val playJsonVersion = "3.0.4"

  val logback = "ch.qos.logback" % "logback-classic" % "1.5.6"

  val specs2Version = "4.20.7"
  val specs2Deps = Seq(
    "specs2-core",
    "specs2-junit"
  ).map("org.specs2" %% _ % specs2Version)
  val specs2Mock = "org.specs2" %% "specs2-mock" % specs2Version // Be aware: This lib is only published for Scala 2

  val specsMatcherExtra = "org.specs2" %% "specs2-matcher-extra" % specs2Version

  val scalacheckDependencies = Seq(
    "org.specs2"     %% "specs2-scalacheck" % specs2Version % Test,
    "org.scalacheck" %% "scalacheck"        % "1.18.0"      % Test
  )

  val jacksonVersion  = "2.14.3"
  val jacksonDatabind = Seq("com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion)
  val jacksons = Seq(
    "com.fasterxml.jackson.core"     % "jackson-core",
    "com.fasterxml.jackson.core"     % "jackson-annotations",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"
  ).map(_ % jacksonVersion) ++ jacksonDatabind
  // Overrides additional jackson deps pulled in by pekko-serialization-jackson
  // https://github.com/apache/pekko/blob/v1.0.1/project/Dependencies.scala#L117-L125
  // https://github.com/apache/pekko/blob/v1.0.1/build.sbt#L273
  // Can be removed as soon as pekko upgrades to same jackson version like Play uses
  val pekkoSerializationJacksonOverrides = Seq(
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor",
    "com.fasterxml.jackson.module"     % "jackson-module-parameter-names",
    "com.fasterxml.jackson.module"    %% "jackson-module-scala",
  ).map(_ % jacksonVersion)

  val playJson = "org.playframework" %% "play-json" % playJsonVersion

  val slf4jVersion = "2.0.13"
  val slf4j        = Seq("slf4j-api", "jul-to-slf4j", "jcl-over-slf4j").map("org.slf4j" % _ % slf4jVersion)
  val slf4jApi     = "org.slf4j" % "slf4j-api"    % slf4jVersion
  val slf4jSimple  = "org.slf4j" % "slf4j-simple" % slf4jVersion

  val guava      = "com.google.guava" % "guava"        % "33.2.1-jre"
  val mockitoAll = "org.mockito"      % "mockito-core" % "5.12.0"

  val javaxInject = "javax.inject" % "javax.inject" % "1"

  val h2database = "com.h2database" % "h2" % "2.2.224"

  val derbyVersion = "10.16.1.1"
  val derbyDatabase = Seq(
    "org.apache.derby" % "derby",
    "org.apache.derby" % "derbytools"
  ).map(_ % derbyVersion)

  val acolyteVersion = "1.2.9"
  val acolyte        = "org.eu.acolyte" % "jdbc-driver" % acolyteVersion

  val jjwtVersion = "0.12.5"
  val jjwts = Seq(
    "io.jsonwebtoken" % "jjwt-api",
    "io.jsonwebtoken" % "jjwt-impl"
  ).map(_ % jjwtVersion) ++ Seq(
    ("io.jsonwebtoken" % "jjwt-jackson" % jjwtVersion).excludeAll(ExclusionRule("com.fasterxml.jackson.core"))
  )

  val jdbcDeps = Seq(
    "com.zaxxer"         % "HikariCP"  % "5.1.0",
    "com.googlecode.usc" % "jdbcdslog" % "1.0.6.2",
    h2database           % Test,
    acolyte              % Test,
    logback              % Test,
    "tyrex"              % "tyrex"     % "1.0.1"
  ) ++ specs2Deps.map(_ % Test)

  val jpaDeps = Seq(
    "jakarta.persistence" % "jakarta.persistence-api" % "3.2.0",
    "org.hibernate"       % "hibernate-core"          % "6.5.2.Final" % "test"
  )

  def scalaReflect(scalaVersion: String) = CrossVersion.partialVersion(scalaVersion) match {
    case Some((3, _)) => Seq()
    case _            => Seq("org.scala-lang" % "scala-reflect" % scalaVersion % "provided")
  }
  def scalaParserCombinators(scalaVersion: String) =
    Seq("org.scala-lang.modules" %% "scala-parser-combinators" % {
      CrossVersion.partialVersion(scalaVersion) match {
        case Some((2, _)) => "1.1.2"
        case _            => "2.4.0"
      }
    })

  val springFrameworkVersion = "6.1.9"

  val javaDeps = Seq(
    // Used by the Java routing DSL
    "net.jodah" % "typetools" % "0.6.3"
  ) ++ specs2Deps.map(_ % Test)

  val joda = Seq(
    "joda-time" % "joda-time"    % "2.12.7",
    "org.joda"  % "joda-convert" % "2.2.3"
  )

  val javaFormsDeps = Seq(
    "org.hibernate.validator" % "hibernate-validator" % "8.0.1.Final",
    ("org.springframework"    % "spring-context"      % springFrameworkVersion)
      .exclude("org.springframework", "spring-aop")
      .exclude("org.springframework", "spring-beans")
      .exclude("org.springframework", "spring-core")
      .exclude("org.springframework", "spring-expression")
      .exclude("io.micrometer", "micrometer-observation"),
    ("org.springframework" % "spring-core" % springFrameworkVersion)
      .exclude("org.springframework", "spring-jcl"),
    ("org.springframework" % "spring-beans" % springFrameworkVersion)
      .exclude("org.springframework", "spring-core")
  ) ++ specs2Deps.map(_ % Test)

  val junitInterface = "com.github.sbt" % "junit-interface" % "0.13.3"
  val junit          = "junit"          % "junit"           % "4.13.2"

  val assertj = "org.assertj" % "assertj-core" % "3.26.0"

  val javaTestDeps = Seq(
    junit,
    junitInterface,
    assertj,
    mockitoAll,
    logback
  ).map(_ % Test)

  val guiceVersion = "6.0.0"
  val guiceDeps = Seq(
    "com.google.inject"            % "guice"                % guiceVersion,
    "com.google.inject.extensions" % "guice-assistedinject" % guiceVersion
  )

  def runtime(scalaVersion: String) =
    slf4j ++
      Seq("pekko-actor", "pekko-actor-typed", "pekko-slf4j", "pekko-serialization-jackson")
        .map("org.apache.pekko" %% _ % pekkoVersion) ++
      Seq("pekko-testkit", "pekko-actor-testkit-typed")
        .map("org.apache.pekko" %% _ % pekkoVersion % Test) ++
      jacksons ++
      pekkoSerializationJacksonOverrides ++
      jjwts ++
      Seq(
        playJson,
        guava,
        javaxInject,
        sslConfig
      ) ++ scalaParserCombinators(scalaVersion) ++ specs2Deps.map(_ % Test) ++ javaTestDeps ++
      scalaReflect(scalaVersion)

  val nettyVersion = "4.1.111.Final"

  val netty = Seq(
    "org.playframework.netty" % "netty-reactive-streams-http" % "3.0.2",
    "io.netty"                % "netty-codec-http"            % nettyVersion, // increases transitive Netty dependency version ...
    "io.netty"                % "netty-handler"               % nettyVersion, // ... pulled in by netty-reactive-streams-http
    // Provide various Linux native transport dependencies. Netty automatically loads the correct native library
    // depending on the architecture; the ones that don't match are simply ignored.
    // Of course this all works only when enabled in the config via: play.server.netty.transport = "native"
    ("io.netty" % "netty-transport-native-epoll" % nettyVersion).classifier("linux-x86_64"),
    ("io.netty" % "netty-transport-native-epoll" % nettyVersion).classifier("linux-aarch_64"),
    ("io.netty" % "netty-transport-native-epoll" % nettyVersion).classifier("linux-riscv64"),
  ) ++ specs2Deps.map(_ % Test)

  val pekkoHttp = "org.apache.pekko" %% "pekko-http-core" % pekkoHttpVersion

  val cookieEncodingDependencies = slf4j

  val jimfs = "com.google.jimfs" % "jimfs" % "1.3.0"

  val okHttp = "com.squareup.okhttp3" % "okhttp" % "4.12.0"

  def routesCompilerDependencies(scalaVersion: String) = {
    specs2Deps.map(_ % Test) ++ Seq(specsMatcherExtra % Test) ++ scalaParserCombinators(
      scalaVersion
    ) ++ (logback % Test :: Nil)
  }

  private def sbtPluginDep(moduleId: ModuleID, sbtVersion: String, scalaVersion: String) = {
    Defaults.sbtPluginExtra(
      moduleId,
      CrossVersion.binarySbtVersion(sbtVersion),
      CrossVersion.binaryScalaVersion(scalaVersion)
    )
  }

  val playFileWatch = "org.playframework" % "play-file-watch" % "3.0.0-M3"

  val runSupportDeps: Seq[ModuleID] = Seq(playFileWatch) ++ javaTestDeps

  val typesafeConfig = "com.typesafe" % "config" % "1.4.3"

  def sbtDependencies(sbtVersion: String, scalaVersion: String) = {
    def sbtDep(moduleId: ModuleID) = sbtPluginDep(moduleId, sbtVersion, scalaVersion)

    Seq(
      typesafeConfig,
      slf4jSimple,
      playFileWatch,
      sbtDep("org.playframework.twirl" % "sbt-twirl"           % BuildInfo.sbtTwirlVersion),
      sbtDep("com.github.sbt"          % "sbt-native-packager" % BuildInfo.sbtNativePackagerVersion),
      sbtDep("com.github.sbt"          % "sbt-web"             % "1.5.8"),
      sbtDep("com.github.sbt"          % "sbt-js-engine"       % "1.3.9"),
      logback % Test
    ) ++ specs2Deps.map(_ % Test) ++ scalaReflect(scalaVersion)
  }

  val playdocWebjarDependencies = Seq(
    "org.webjars" % "jquery"   % "3.7.1"        % "webjars",
    "org.webjars" % "prettify" % "4-Mar-2013-1" % "webjars"
  )

  val playDocVersion = "3.0.1"
  val playDocsDependencies = Seq(
    "org.playframework" %% "play-doc" % playDocVersion
  ) ++ playdocWebjarDependencies

  val streamsDependencies = Seq(
    "org.reactivestreams" % "reactive-streams" % "1.0.4",
    "org.apache.pekko"   %% "pekko-stream"     % pekkoVersion,
  ) ++ specs2Deps.map(_ % Test) ++ javaTestDeps

  val playServerDependencies = specs2Deps.map(_ % Test) ++ Seq(
    mockitoAll % Test,
    guava      % Test,
    logback    % Test
  )

  val clusterDependencies = Seq(
    "org.apache.pekko" %% "pekko-cluster-sharding-typed" % pekkoVersion
  )

  val fluentleniumVersion = "6.0.0"
  // This is the selenium version compatible with the FluentLenium version declared above.
  // See https://repo1.maven.org/maven2/io/fluentlenium/fluentlenium-parent/6.0.0/fluentlenium-parent-6.0.0.pom
  val seleniumVersion = "4.14.1"
  val htmlunitVersion = "4.13.0"

  val testDependencies = Seq(junit, junitInterface, guava, logback) ++ Seq(
    ("io.fluentlenium" % "fluentlenium-core" % fluentleniumVersion).exclude("org.jboss.netty", "netty"),
    // htmlunit-driver uses an open range to selenium dependencies. This is slightly
    // slowing down the build. So the open range deps were removed and we can re-add
    // them using a specific version. Using an open range is also not good for the
    // local cache.
    ("org.seleniumhq.selenium" % "htmlunit-driver" % htmlunitVersion).excludeAll(
      ExclusionRule("org.seleniumhq.selenium", "selenium-api"),
      ExclusionRule("org.seleniumhq.selenium", "selenium-support")
    ),
    "org.seleniumhq.selenium" % "selenium-api"            % seleniumVersion,
    "org.seleniumhq.selenium" % "selenium-support"        % seleniumVersion,
    "org.seleniumhq.selenium" % "selenium-firefox-driver" % seleniumVersion
  ) ++ guiceDeps ++ specs2Deps.map(_ % Test) :+ mockitoAll % Test

  val playCacheDeps = specs2Deps.map(_ % Test) :+ logback % Test

  val jcacheApi = Seq(
    "javax.cache" % "cache-api" % "1.1.1"
  )

  val ehcacheVersion = "2.10.9.2"
  val playEhcacheDeps = Seq(
    "net.sf.ehcache" % "ehcache" % ehcacheVersion,
    "org.ehcache"    % "jcache"  % "1.0.1"
  ) ++ jcacheApi

  val caffeineVersion = "3.1.8"
  val playCaffeineDeps = Seq(
    "com.github.ben-manes.caffeine" % "caffeine" % caffeineVersion,
    "com.github.ben-manes.caffeine" % "jcache"   % caffeineVersion
  ) ++ jcacheApi ++ Seq(assertj % Test)

  val playWsStandaloneVersion = "3.0.3"
  val playWsDeps = Seq(
    "org.playframework" %% "play-ws-standalone"      % playWsStandaloneVersion,
    "org.playframework" %% "play-ws-standalone-xml"  % playWsStandaloneVersion,
    "org.playframework" %% "play-ws-standalone-json" % playWsStandaloneVersion,
    // Update transitive Pekko version as needed:
    "org.apache.pekko" %% "pekko-stream" % pekkoVersion
  ) ++ (specs2Deps :+ specsMatcherExtra).map(_ % Test) :+ mockitoAll % Test

  // Must use a version of ehcache that supports jcache 1.0.0
  val playAhcWsDeps = Seq(
    "org.playframework"            %% "play-ahc-ws-standalone" % playWsStandaloneVersion,
    "org.playframework"             % "shaded-asynchttpclient" % playWsStandaloneVersion,
    "org.playframework"             % "shaded-oauth"           % playWsStandaloneVersion,
    "com.github.ben-manes.caffeine" % "jcache"                 % caffeineVersion % Test,
    "net.sf.ehcache"                % "ehcache"                % ehcacheVersion  % Test,
    "org.ehcache"                   % "jcache"                 % "1.0.1"         % Test
  ) ++ jcacheApi

  val playDocsSbtPluginDependencies = Seq(
    "org.playframework" %% "play-doc" % playDocVersion
  )

  val salvationVersion = "2.7.2"
  val playFilterDeps = Seq(
    "com.shapesecurity" % "salvation" % salvationVersion % Test
  )
}
