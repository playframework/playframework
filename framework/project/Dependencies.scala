/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
import sbt._
import buildinfo.BuildInfo

object Dependencies {

  val akkaVersion = "2.4.2"

  val specsVersion = "3.6.6"
  val specsBuild = Seq(
    "specs2-core",
    "specs2-junit",
    "specs2-mock"
  ).map("org.specs2" %% _ % specsVersion)

  val specsMatcherExtra = "org.specs2" %% "specs2-matcher-extra" % specsVersion

  val specsSbt = specsBuild

  val jacksons = Seq(
    "com.fasterxml.jackson.core" % "jackson-core",
    "com.fasterxml.jackson.core" % "jackson-annotations",
    "com.fasterxml.jackson.core" % "jackson-databind",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"
  ).map(_ % "2.7.1")

  val slf4j = Seq("slf4j-api", "jul-to-slf4j", "jcl-over-slf4j").map("org.slf4j" % _ % "1.7.16")
  val logback = Seq("logback-core", "logback-classic").map("ch.qos.logback" % _ % "1.1.4")

  val guava = "com.google.guava" % "guava" % "19.0"
  val findBugs = "com.google.code.findbugs" % "jsr305" % "3.0.1" // Needed by guava
  val mockitoAll = "org.mockito" % "mockito-all" % "1.10.19"

  val h2database = "com.h2database" % "h2" % "1.4.191"
  val derbyDatabase = "org.apache.derby" % "derby" % "10.12.1.1"

  val acolyteVersion = "1.0.34-j7p"
  val acolyte = "org.eu.acolyte" % "jdbc-driver" % acolyteVersion

  val jdbcDeps = Seq(
    "com.jolbox" % "bonecp" % "0.8.0.RELEASE",
    "com.zaxxer" % "HikariCP" % "2.4.3",
    "com.googlecode.usc" % "jdbcdslog" % "1.0.6.2",
    h2database,
    acolyte % Test,
    "tyrex" % "tyrex" % "1.0.1") ++ specsBuild.map(_ % Test) ++ logback.map(_ % Test)

  val javaJdbcDeps = Seq(acolyte % Test)

  val jpaDeps = Seq(
    "org.hibernate.javax.persistence" % "hibernate-jpa-2.1-api" % "1.0.0.Final",
    "org.hibernate" % "hibernate-entitymanager" % "5.1.0.Final" % "test"
  )

  val scalaJava8Compat = "org.scala-lang.modules" %% "scala-java8-compat" % "0.7.0"
  def scalaParserCombinators(scalaVersion: String) = CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, major)) if major >= 11 => Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4")
    case _ => Nil
  }

  val springFrameworkVersion = "4.2.4.RELEASE"

  val javaDeps = Seq(
    scalaJava8Compat,

    "org.yaml" % "snakeyaml" % "1.16",
    "org.hibernate" % "hibernate-validator" % "5.2.4.Final",
    "javax.el"      % "javax.el-api"        % "3.0.0", // required by hibernate-validator

    ("org.springframework" % "spring-context" % springFrameworkVersion)
      .exclude("org.springframework", "spring-aop")
      .exclude("org.springframework", "spring-beans")
      .exclude("org.springframework", "spring-core")
      .exclude("org.springframework", "spring-expression")
      .exclude("org.springframework", "spring-asm"),

    ("org.springframework" % "spring-core" % springFrameworkVersion)
      .exclude("org.springframework", "spring-asm")
      .exclude("commons-logging", "commons-logging"),

    ("org.springframework" % "spring-beans" % springFrameworkVersion)
      .exclude("org.springframework", "spring-core"),

    ("org.reflections" % "reflections" % "0.9.10")
      .exclude("com.google.code.findbugs", "annotations"),

    // Used by the Java routing DSL
    "net.jodah" % "typetools" % "0.4.4",

    guava,
    findBugs,

    "org.apache.tomcat" % "tomcat-servlet-api" % "8.0.32"
  ) ++ specsBuild.map(_ % Test) ++ logback.map(_ % Test)

  val junitInterface = "com.novocode" % "junit-interface" % "0.11"
  val junit = "junit" % "junit" % "4.12"

  val javaTestDeps = Seq(
    junit,
    junitInterface,
    "org.easytesting" % "fest-assert"     % "1.4",
    mockitoAll
  ).map(_ % Test)

  val jodatime = "joda-time" % "joda-time" % "2.9.2"
  val jodaConvert = "org.joda" % "joda-convert" % "1.8.1"

  def runtime(scalaVersion: String) =
    slf4j ++
    Seq("akka-actor", "akka-slf4j").map("com.typesafe.akka" %% _ % akkaVersion) ++
    jacksons ++
    Seq(
      "org.scala-stm" %% "scala-stm" % "0.7",
      "commons-codec" % "commons-codec" % "1.10",

      jodatime,
      jodaConvert,

      "org.apache.commons" % "commons-lang3" % "3.4",

      "xerces" % "xercesImpl" % "2.11.0",

      "javax.transaction" % "jta" % "1.1",

      "com.google.inject" % "guice" % "4.0",
      "com.google.inject.extensions" % "guice-assistedinject" % "4.0",

      guava % Test,

      "org.scala-lang" % "scala-reflect" % scalaVersion,
      scalaJava8Compat
    ) ++ scalaParserCombinators(scalaVersion) ++
    specsBuild.map(_ % Test) ++ logback.map(_ % Test) ++
    javaTestDeps

  val nettyVersion = "4.0.36.Final"

  val netty = Seq(
    "com.typesafe.netty" % "netty-reactive-streams-http" % "1.0.5",
    "io.netty" % "netty-transport-native-epoll" % nettyVersion classifier "linux-x86_64"
  ) ++ specsBuild.map(_ % Test)++ logback.map(_ % Test)

  val nettyUtilsDependencies = slf4j

  val akkaHttp = Seq(
    "com.typesafe.akka" %% "akka-http-core" % akkaVersion
  )

  def routesCompilerDependencies(scalaVersion: String) = Seq(
    "commons-io" % "commons-io" % "2.4",
    specsMatcherExtra % Test
  ) ++ specsBuild.map(_ % Test) ++ logback.map(_ % Test) ++ scalaParserCombinators(scalaVersion)

  private def sbtPluginDep(sbtVersion: String, scalaVersion: String, moduleId: ModuleID) = {
    moduleId.extra(
      "sbtVersion" -> CrossVersion.binarySbtVersion(sbtVersion),
      "scalaVersion" -> CrossVersion.binaryScalaVersion(scalaVersion)
    )
  }

  def runSupportDependencies(sbtVersion: String, scalaVersion: String) = Seq(
    sbtIO(sbtVersion, scalaVersion)
  ) ++ specsBuild.map(_ % Test) ++ logback.map(_ % Test)

  // use partial version so that non-standard scala binary versions from dbuild also work
  def sbtIO(sbtVersion: String, scalaVersion: String): ModuleID = CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, major)) if major >= 11 => "org.scala-sbt" %% "io" % "0.13.11" % "provided"
    case _ => "org.scala-sbt" % "io" % sbtVersion % "provided"
  }

  val jnotify = "net.contentobjects.jnotify" % "jnotify" % "0.94-play-1"

  val sbtRcVersion = "0.3.1"
  val sbtCoreNextVersion = "0.1.1"

  def forkRunProtocolDependencies(scalaBinaryVersion: String) = Seq(
    sbtRcClient(scalaBinaryVersion)
  ) ++ specsBuild.map(_ % "test") ++ logback.map(_ % Test)

  // use partial version so that non-standard scala binary versions from dbuild also work
  def sbtRcClient(scalaBinaryVersion: String): ModuleID = CrossVersion.partialVersion(scalaBinaryVersion) match {
    case Some((2, 10)) => "com.typesafe.sbtrc" % "client-2-10" % sbtRcVersion
    case Some((2, 11)) => "com.typesafe.sbtrc" % "client-2-11" % sbtRcVersion
    case _ => sys.error(s"Unsupported scala version: $scalaBinaryVersion")
  }

  def forkRunDependencies(scalaBinaryVersion: String) = Seq(
    sbtRcActorClient(scalaBinaryVersion),
    jnotify
  )

  // use partial version so that non-standard scala binary versions from dbuild also work
  def sbtRcActorClient(scalaBinaryVersion: String): ModuleID = CrossVersion.partialVersion(scalaBinaryVersion) match {
    case Some((2, 10)) => "com.typesafe.sbtrc" % "actor-client-2-10" % sbtRcVersion
    case Some((2, 11)) => "com.typesafe.sbtrc" % "actor-client-2-11" % sbtRcVersion
    case _ => sys.error(s"Unsupported scala version: $scalaBinaryVersion")
  }

  def sbtForkRunPluginDependencies(sbtVersion: String, scalaVersion: String) = Seq(
    sbtPluginDep(sbtVersion, scalaVersion, "org.scala-sbt" % "sbt-core-next" % sbtCoreNextVersion)
  )

  val typesafeConfig = "com.typesafe" % "config" % "1.3.0"

  def sbtDependencies(sbtVersion: String, scalaVersion: String) = {
    def sbtDep(moduleId: ModuleID) = sbtPluginDep(sbtVersion, scalaVersion, moduleId)

    Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion % "provided",
      typesafeConfig,

      jnotify,

      sbtDep("com.typesafe.sbt" % "sbt-twirl" % BuildInfo.sbtTwirlVersion),

      sbtDep("com.typesafe.sbt" % "sbt-native-packager" % BuildInfo.sbtNativePackagerVersion),

      sbtDep("com.typesafe.sbt" % "sbt-web" % "1.3.0"),
      sbtDep("com.typesafe.sbt" % "sbt-js-engine" % "1.1.3")
    ) ++ specsBuild.map(_ % Test) ++ logback.map(_ % Test)
  }

  val playdocWebjarDependencies = Seq(
    "org.webjars" % "jquery"   % "2.2.0"    % "webjars",
    "org.webjars" % "prettify" % "4-Mar-2013" % "webjars"
  )

  val playDocsDependencies = Seq(
    "com.typesafe.play" %% "play-doc" % "1.2.2"
  ) ++ playdocWebjarDependencies

  val iterateesDependencies = Seq(
    "org.scala-stm" %% "scala-stm" % "0.7",
    typesafeConfig
  ) ++ specsBuild.map(_ % Test) ++ logback.map(_ % Test)

  val streamsDependencies = Seq(
    "org.reactivestreams" % "reactive-streams" % "1.0.0",
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    scalaJava8Compat
  ) ++ specsBuild.map(_ % "test") ++ logback.map(_ % Test) ++ javaTestDeps

  def jsonDependencies(scalaVersion: String) = Seq(
    jodatime,
    jodaConvert,
    "org.scala-lang" % "scala-reflect" % scalaVersion) ++
  jacksons ++
  specsBuild.map(_ % Test) ++ logback.map(_ % Test)

  val scalacheckDependencies = Seq(
    "org.specs2"     %% "specs2-scalacheck" % specsVersion % Test,
    "org.scalacheck" %% "scalacheck"        % "1.12.2"     % Test
  )

  val playServerDependencies = Seq(
    guava % Test
  ) ++ specsBuild.map(_ % Test) ++ logback.map(_ % Test)

  val testDependencies = Seq(junit) ++ specsBuild.map(_ % Test) ++ logback.map(_ % Test) ++ Seq(
    junitInterface,
    guava,
    findBugs,
    "net.sourceforge.htmlunit" % "htmlunit" % "2.20", // adds support for jQuery 2.20; can be removed as soon as fluentlenium has it in it's own dependencies
    ("org.fluentlenium" % "fluentlenium-core" % "0.10.9")
      .exclude("org.jboss.netty", "netty")
  )

  val playCacheDeps = "net.sf.ehcache" % "ehcache-core" % "2.6.11" +:
    (specsBuild.map(_ % Test) ++ logback.map(_ % Test))

  val playWsDeps = Seq(
    guava,
    "org.asynchttpclient" % "async-http-client" % "2.0.0"
  ) ++
    Seq("signpost-core", "signpost-commonshttp4").map("oauth.signpost" % _  % "1.2.1.2") ++
    logback.map(_ % Test) ++
    (specsBuild :+ specsMatcherExtra).map(_ % Test) :+
    mockitoAll % Test

  val playDocsSbtPluginDependencies = Seq(
    "com.typesafe.play" %% "play-doc" % "1.3.0"
  )

}
