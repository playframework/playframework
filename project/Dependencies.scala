/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt._
import sbt.librarymanagement.CrossVersion

import buildinfo.BuildInfo
import Keys._

object Dependencies {

  /**
   * Temporary workarounds while using akka-http 10.2.x which does not provide Scala 3 artifacts.
   */
  private implicit class AkkaHttpScala3Workarounds(module: ModuleID) {
    private def sysPropsCheck(m: => ModuleID) = if (sys.props.getOrElse("scala3Tests", "") == "true") m else module
    def forScala3TestsUse2_13()               = sysPropsCheck(module.cross(CrossVersion.for3Use2_13))
    def forScala3TestsExcludeScalaParserCombinators_3() = sysPropsCheck(
      module.exclude("org.scala-lang.modules", "scala-parser-combinators_3")
    )
    def forScala3TestsExcludeSslConfigCore_213() = sysPropsCheck(module.exclude("com.typesafe", "ssl-config-core_2.13"))
    def forScala3TestsExcludeAkkaOrganization()  = sysPropsCheck(module.excludeAll(ExclusionRule("com.typesafe.akka")))
  }

  val akkaVersion: String = sys.props.getOrElse("akka.version", "2.6.21")
  val akkaHttpVersion     = sys.props.getOrElse("akka.http.version", "10.2.10")

  val sslConfigCoreVersion = "0.6.1"
  val sslConfig            = "com.typesafe" %% "ssl-config-core" % sslConfigCoreVersion

  val playJsonVersion = "2.10.0-RC9"

  val logback = "ch.qos.logback" % "logback-classic" % "1.4.11"

  val specs2Version = "4.20.2"
  val specs2Deps = Seq(
    "specs2-core",
    "specs2-junit"
  ).map("org.specs2" %% _ % specs2Version)
  val specs2Mock = "org.specs2" %% "specs2-mock" % specs2Version // Be aware: This lib is only published for Scala 2

  val specsMatcherExtra = "org.specs2" %% "specs2-matcher-extra" % specs2Version

  val scalacheckDependencies = Seq(
    "org.specs2"     %% "specs2-scalacheck" % specs2Version % Test,
    "org.scalacheck" %% "scalacheck"        % "1.17.0"      % Test
  )

  val jacksonVersion  = "2.14.3"
  val jacksonDatabind = Seq("com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion)
  val jacksons = Seq(
    "com.fasterxml.jackson.core"     % "jackson-core",
    "com.fasterxml.jackson.core"     % "jackson-annotations",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"
  ).map(_ % jacksonVersion) ++ jacksonDatabind
  // Overrides additional jackson deps pulled in by akka-serialization-jackson
  // https://github.com/akka/akka/blob/v2.6.21/project/Dependencies.scala#L145-L153
  // https://github.com/akka/akka/blob/v2.6.21/build.sbt#L258
  // Can be removed as soon as akka upgrades to same jackson version like Play uses
  val akkaSerializationJacksonOverrides = Seq(
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor",
    "com.fasterxml.jackson.module"     % "jackson-module-parameter-names",
  ).map(_ % jacksonVersion) ++
    Seq(("com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion).forScala3TestsUse2_13())

  val playJson = "com.typesafe.play" %% "play-json" % playJsonVersion

  val slf4jVersion = "2.0.7"
  val slf4j        = Seq("slf4j-api", "jul-to-slf4j", "jcl-over-slf4j").map("org.slf4j" % _ % slf4jVersion)
  val slf4jApi     = "org.slf4j" % "slf4j-api"    % slf4jVersion
  val slf4jSimple  = "org.slf4j" % "slf4j-simple" % slf4jVersion

  val guava      = "com.google.guava" % "guava"        % "32.1.2-jre"
  val mockitoAll = "org.mockito"      % "mockito-core" % "5.4.0"

  val javaxInject = "javax.inject" % "javax.inject" % "1"

  val h2database = "com.h2database" % "h2" % "2.2.220"

  val derbyVersion = "10.15.2.0"
  val derbyDatabase = Seq(
    "org.apache.derby" % "derby",
    "org.apache.derby" % "derbytools"
  ).map(_ % derbyVersion)

  val acolyteVersion = "1.2.8"
  val acolyte        = "org.eu.acolyte" % "jdbc-driver" % acolyteVersion

  val jjwtVersion = "0.11.5"
  val jjwts = Seq(
    "io.jsonwebtoken" % "jjwt-api",
    "io.jsonwebtoken" % "jjwt-impl"
  ).map(_ % jjwtVersion) ++ Seq(
    ("io.jsonwebtoken" % "jjwt-jackson" % jjwtVersion).excludeAll(ExclusionRule("com.fasterxml.jackson.core"))
  )

  val jdbcDeps = Seq(
    "com.zaxxer"         % "HikariCP"  % "5.0.1",
    "com.googlecode.usc" % "jdbcdslog" % "1.0.6.2",
    h2database           % Test,
    acolyte              % Test,
    logback              % Test,
    "tyrex"              % "tyrex"     % "1.0.1"
  ) ++ specs2Deps.map(_ % Test)

  val jpaDeps = Seq(
    "jakarta.persistence" % "jakarta.persistence-api" % "3.1.0",
    "org.hibernate"       % "hibernate-core"          % "6.2.7.Final" % "test"
  )

  def scalaReflect(scalaVersion: String) = CrossVersion.partialVersion(scalaVersion) match {
    case Some((3, _)) => Seq()
    case _            => Seq("org.scala-lang" % "scala-reflect" % scalaVersion % "provided")
  }
  def scalaParserCombinators(scalaVersion: String) =
    Seq("org.scala-lang.modules" %% "scala-parser-combinators" % {
      CrossVersion.partialVersion(scalaVersion) match {
        case Some((2, _)) => "1.1.2"
        case _            => "2.3.0"
      }
    })

  val springFrameworkVersion = "5.3.29"

  val javaDeps = Seq(
    // Used by the Java routing DSL
    "net.jodah" % "typetools" % "0.6.3"
  ) ++ specs2Deps.map(_ % Test)

  val joda = Seq(
    "joda-time" % "joda-time"    % "2.12.5",
    "org.joda"  % "joda-convert" % "2.2.3"
  )

  val javaFormsDeps = Seq(
    "org.hibernate.validator" % "hibernate-validator" % "6.2.5.Final",
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

  val junitInterface = "com.github.sbt" % "junit-interface" % "0.13.3"
  val junit          = "junit"          % "junit"           % "4.13.2"

  val javaTestDeps = Seq(
    junit,
    junitInterface,
    "org.easytesting" % "fest-assert" % "1.4",
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
      Seq("akka-actor", "akka-actor-typed", "akka-slf4j", "akka-serialization-jackson")
        .map("com.typesafe.akka" %% _ % akkaVersion)
        .map(_.forScala3TestsUse2_13()) ++
      Seq("akka-testkit", "akka-actor-testkit-typed")
        .map("com.typesafe.akka" %% _ % akkaVersion % Test)
        .map(_.forScala3TestsUse2_13()) ++
      jacksons ++
      akkaSerializationJacksonOverrides ++
      jjwts ++
      Seq(
        playJson,
        guava,
        javaxInject,
        sslConfig
      ) ++ scalaParserCombinators(scalaVersion).map(_.forScala3TestsUse2_13()) ++ specs2Deps.map(
        _ % Test
      ) ++ javaTestDeps ++
      scalaReflect(scalaVersion)

  val nettyVersion = "4.1.96.Final"

  val netty = Seq(
    "com.typesafe.netty" % "netty-reactive-streams-http"  % "2.0.9",
    ("io.netty"          % "netty-transport-native-epoll" % nettyVersion).classifier("linux-x86_64")
  ) ++ specs2Deps.map(_ % Test)

  val akkaHttp = ("com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion).cross(CrossVersion.for3Use2_13)

  val akkaHttp2Support = ("com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion).cross(CrossVersion.for3Use2_13)

  val cookieEncodingDependencies = slf4j

  val jimfs = "com.google.jimfs" % "jimfs" % "1.3.0"

  val okHttp = "com.squareup.okhttp3" % "okhttp" % "4.11.0"

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

  val playFileWatch = "com.typesafe.play" %% "play-file-watch" % "1.2.0-M2"

  def runSupportDependencies(sbtVersion: String): Seq[ModuleID] = {
    Seq(playFileWatch, logback % Test) ++ specs2Deps.map(_ % Test)
  }

  val typesafeConfig = "com.typesafe" % "config" % "1.4.2"

  def sbtDependencies(sbtVersion: String, scalaVersion: String) = {
    def sbtDep(moduleId: ModuleID) = sbtPluginDep(moduleId, sbtVersion, scalaVersion)

    Seq(
      typesafeConfig,
      slf4jSimple,
      playFileWatch,
      sbtDep("com.typesafe.play" % "sbt-twirl"           % BuildInfo.sbtTwirlVersion),
      sbtDep("com.github.sbt"    % "sbt-native-packager" % BuildInfo.sbtNativePackagerVersion),
      sbtDep("com.github.sbt"    % "sbt-web"             % "1.5.0-M1"),
      sbtDep("com.github.sbt"    % "sbt-js-engine"       % "1.3.0-M4"),
      logback % Test
    ) ++ specs2Deps.map(_ % Test) ++ scalaReflect(scalaVersion)
  }

  val playdocWebjarDependencies = Seq(
    "org.webjars" % "jquery"   % "3.7.0"        % "webjars",
    "org.webjars" % "prettify" % "4-Mar-2013-1" % "webjars"
  )

  val playDocVersion = "2.2.0-M3"
  val playDocsDependencies = Seq(
    "com.typesafe.play" %% "play-doc" % playDocVersion
  ) ++ playdocWebjarDependencies

  val streamsDependencies = Seq(
    "org.reactivestreams" % "reactive-streams" % "1.0.4",
    ("com.typesafe.akka" %% "akka-stream"      % akkaVersion)
      .forScala3TestsUse2_13()
      .forScala3TestsExcludeSslConfigCore_213()
  ) ++ specs2Deps.map(_ % Test) ++ javaTestDeps

  val playServerDependencies = specs2Deps.map(_ % Test) ++ Seq(
    mockitoAll % Test,
    guava      % Test,
    logback    % Test
  )

  val clusterDependencies = Seq(
    ("com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion)
      .forScala3TestsUse2_13()
      .forScala3TestsExcludeSslConfigCore_213()
  )

  val fluentleniumVersion = "6.0.0"
  // This is the selenium version compatible with the FluentLenium version declared above.
  // See https://repo1.maven.org/maven2/io/fluentlenium/fluentlenium-parent/6.0.0/fluentlenium-parent-6.0.0.pom
  val seleniumVersion = "4.11.0"

  val testDependencies = Seq(junit, junitInterface, guava, logback) ++ Seq(
    ("io.fluentlenium" % "fluentlenium-core" % fluentleniumVersion).exclude("org.jboss.netty", "netty"),
    // htmlunit-driver uses an open range to selenium dependencies. This is slightly
    // slowing down the build. So the open range deps were removed and we can re-add
    // them using a specific version. Using an open range is also not good for the
    // local cache.
    ("org.seleniumhq.selenium" % "htmlunit-driver" % seleniumVersion).excludeAll(
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
  ) ++ jcacheApi

  val playWsStandaloneVersion = "2.2.0-M4"
  val playWsDeps = Seq(
    ("com.typesafe.play" %% "play-ws-standalone"      % playWsStandaloneVersion).forScala3TestsExcludeAkkaOrganization(),
    ("com.typesafe.play" %% "play-ws-standalone-xml"  % playWsStandaloneVersion).forScala3TestsExcludeAkkaOrganization(),
    ("com.typesafe.play" %% "play-ws-standalone-json" % playWsStandaloneVersion)
      .forScala3TestsExcludeAkkaOrganization(),
    // Update transitive Akka version as needed:
    ("com.typesafe.akka" %% "akka-stream" % akkaVersion)
      .forScala3TestsUse2_13()
      .forScala3TestsExcludeSslConfigCore_213()
  ) ++ (specs2Deps :+ specsMatcherExtra.forScala3TestsExcludeScalaParserCombinators_3())
    .map(_ % Test) :+ mockitoAll % Test

  // Must use a version of ehcache that supports jcache 1.0.0
  val playAhcWsDeps = Seq(
    ("com.typesafe.play" %% "play-ahc-ws-standalone" % playWsStandaloneVersion)
      .forScala3TestsExcludeAkkaOrganization()
      .forScala3TestsExcludeScalaParserCombinators_3(),
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
