/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import buildinfo.BuildInfo

object Dependencies {

  val specsVersion = "3.1"
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
  ).map(_ % "2.5.2")

  val guava = "com.google.guava" % "guava" % "18.0"
  val findBugs = "com.google.code.findbugs" % "jsr305" % "2.0.3" // Needed by guava
  val mockitoAll = "org.mockito" % "mockito-all" % "1.10.8"

  val h2database = "com.h2database" % "h2" % "1.4.186"

  val acolyteVersion = "1.0.32"
  val acolyte = "org.eu.acolyte" % "jdbc-driver" % acolyteVersion

  val jdbcDeps = Seq(
    "com.jolbox" % "bonecp" % "0.8.0.RELEASE",
    "com.zaxxer" % "HikariCP" % "2.3.5",
    h2database,
    acolyte % Test,
    "tyrex" % "tyrex" % "1.0.1") ++ specsBuild.map(_ % Test)

  val javaJdbcDeps = Seq(acolyte % Test)

  val jpaDeps = Seq(
    "org.hibernate.javax.persistence" % "hibernate-jpa-2.1-api" % "1.0.0.Final",
    "org.hibernate" % "hibernate-entitymanager" % "4.3.8.Final" % "test"
  )

  val link = Seq(
    "org.javassist" % "javassist" % "3.19.0-GA"
  )
  val javassist = link

  val scalaJava8Compat = "org.scala-lang.modules" %% "scala-java8-compat" % "0.3.0"

  val springFrameworkVersion = "4.1.5.RELEASE"

  val javaDeps = Seq(
    scalaJava8Compat,

    "org.yaml" % "snakeyaml" % "1.15",
    // 5.1.0 upgrade notes: need to add JEE dependencies, eg EL
    "org.hibernate" % "hibernate-validator" % "5.0.3.Final",
    // This is depended on by hibernate validator, we upgrade to 3.2.0 to avoid LGPL license of 3.1.x
    "org.jboss.logging" % "jboss-logging" % "3.2.1.Final",

    ("org.springframework" % "spring-context" % springFrameworkVersion notTransitive ())
      .exclude("org.springframework", "spring-aop")
      .exclude("org.springframework", "spring-beans")
      .exclude("org.springframework", "spring-core")
      .exclude("org.springframework", "spring-expression")
      .exclude("org.springframework", "spring-asm"),

    ("org.springframework" % "spring-core" % springFrameworkVersion notTransitive ())
      .exclude("org.springframework", "spring-asm")
      .exclude("commons-logging", "commons-logging"),

    ("org.springframework" % "spring-beans" % springFrameworkVersion notTransitive ())
      .exclude("org.springframework", "spring-core"),

    ("org.reflections" % "reflections" % "0.9.9" notTransitive ())
      .exclude("javassist", "javassist"),

    // Used by the Java routing DSL
    "net.jodah" % "typetools" % "0.4.0",

    guava,
    findBugs,

    "org.apache.tomcat" % "tomcat-servlet-api" % "8.0.20"
  ) ++ javassist ++ specsBuild.map(_ % Test)

  val junitInterface = "com.novocode" % "junit-interface" % "0.11"
  val junit = "junit" % "junit" % "4.12"

  val javaTestDeps = Seq(
    junit,
    junitInterface,
    "org.easytesting" % "fest-assert"     % "1.4",
    mockitoAll
  ).map(_ % Test)

  val jodatime = "joda-time" % "joda-time" % "2.7"
  val jodaConvert = "org.joda" % "joda-convert" % "1.7"

  def runtime(scalaVersion: String) =
    Seq("slf4j-api", "jul-to-slf4j", "jcl-over-slf4j").map("org.slf4j" % _ % "1.7.10") ++
    Seq("logback-core", "logback-classic").map("ch.qos.logback" % _ % "1.1.2") ++
    Seq("akka-actor", "akka-slf4j").map("com.typesafe.akka" %% _ % "2.3.7") ++
    jacksons ++
    Seq(
      "org.scala-stm" %% "scala-stm" % "0.7",
      "commons-codec" % "commons-codec" % "1.10",

      jodatime,
      jodaConvert,

      "org.apache.commons" % "commons-lang3" % "3.3.2",

      "xerces" % "xercesImpl" % "2.11.0",

      "javax.transaction" % "jta" % "1.1",

      // Since we don't use any of the AOP features of guice, we exclude cglib.
      // This solves issues later where cglib depends on an older version of asm,
      // and other libraries (pegdown) depend on a newer version with a different groupId,
      // and this causes binary issues.
      "com.google.inject" % "guice" % "3.0" classifier "no_aop"
        exclude("org.sonatype.sisu.inject", "cglib") exclude("aopalliance", "aopalliance"),

      guava % Test,

      "org.scala-lang" % "scala-reflect" % scalaVersion
    ) ++
    specsBuild.map(_ % Test) ++
    javaTestDeps

  val netty = Seq(
    "io.netty"           % "netty"                 % "3.10.1.Final",
    "com.typesafe.netty" % "netty-http-pipelining" % "1.1.3"
  ) ++ specsBuild.map(_ % Test)

  val akkaHttp = Seq(
    "com.typesafe.akka" %% "akka-http-core-experimental" % "1.0-M4"
  )

  val routesCompilerDependencies =  Seq(
    "commons-io" % "commons-io" % "2.4"
  ) ++ specsBuild.map(_ % Test)

  private def sbtPluginDep(sbtVersion: String, scalaVersion: String, moduleId: ModuleID) = {
    moduleId.extra(
      "sbtVersion" -> CrossVersion.binarySbtVersion(sbtVersion),
      "scalaVersion" -> CrossVersion.binaryScalaVersion(scalaVersion)
    )
  }

  def runSupportDependencies(sbtVersion: String, scalaVersion: String) = Seq(
    sbtIO(sbtVersion, scalaVersion)
  ) ++ specsBuild.map(_ % Test)

  // use partial version so that non-standard scala binary versions from dbuild also work
  def sbtIO(sbtVersion: String, scalaVersion: String): ModuleID = CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, major)) if major >= 11 => "org.scala-sbt" %% "io" % "0.13.6" % "provided"
    case _ => "org.scala-sbt" % "io" % sbtVersion % "provided"
  }

  val jnotify = "net.contentobjects.jnotify" % "jnotify" % "0.94"

  val sbtRcVersion = "0.3.1"
  val sbtCoreNextVersion = "0.1.1"

  def forkRunProtocolDependencies(scalaBinaryVersion: String) = Seq(
    sbtRcClient(scalaBinaryVersion)
  ) ++ specsBuild.map(_ % "test")

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

  val typesafeConfig = "com.typesafe" % "config" % "1.3.0-M1"

  def sbtDependencies(sbtVersion: String, scalaVersion: String) = {
    def sbtDep(moduleId: ModuleID) = sbtPluginDep(sbtVersion, scalaVersion, moduleId)

    Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion % "provided",
      typesafeConfig,
      "org.mozilla" % "rhino" % "1.7R4",

      ("com.google.javascript" % "closure-compiler" % "v20140814")
        .exclude("args4j", "args4j")
        .exclude("com.google.protobuf", "protobuf-java")
        .exclude("com.google.code.findbugs", "jsr305"),

      guava,

      h2database,

      jnotify,

      sbtDep("com.typesafe.sbt" % "sbt-twirl" % BuildInfo.sbtTwirlVersion),
      sbtDep("com.typesafe.sbt" % "sbt-play-enhancer" % "1.0.1"),

      sbtDep("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "3.0.0"),
      sbtDep("com.typesafe.sbt" % "sbt-native-packager" % BuildInfo.sbtNativePackagerVersion),

      sbtDep("com.typesafe.sbt" % "sbt-web" % "1.1.1"),
      sbtDep("com.typesafe.sbt" % "sbt-js-engine" % "1.0.2"),
      sbtDep("com.typesafe.sbt" % "sbt-webdriver" % "1.0.0")
    ) ++ javassist ++ specsBuild.map(_ % Test)
  }

  val playdocWebjarDependencies = Seq(
    "org.webjars" % "jquery"   % "2.1.3"    % "webjars",
    "org.webjars" % "prettify" % "4-Mar-2013" % "webjars"
  )

  val playDocsDependencies = Seq(
    "com.typesafe.play" %% "play-doc" % "1.2.2"
  ) ++ playdocWebjarDependencies

  val iterateesDependencies = Seq(
    "org.scala-stm" %% "scala-stm" % "0.7",
    typesafeConfig
  ) ++ specsBuild.map(_ % Test)

  val streamsDependencies = Seq(
    "org.reactivestreams" % "reactive-streams" % "1.0.0.RC1"
  ) ++ specsBuild.map(_ % "test")

  def jsonDependencies(scalaVersion: String) = Seq(
    jodatime,
    jodaConvert,
    "org.scala-lang" % "scala-reflect" % scalaVersion) ++
  jacksons ++
  specsBuild.map(_ % Test)

  val scalacheckDependencies = Seq(
    "org.specs2"     %% "specs2-scalacheck" % specsVersion % Test,
    "org.scalacheck" %% "scalacheck"        % "1.12.2"     % Test
  )

  val playServerDependencies = Seq(
    guava % Test
  ) ++ specsBuild.map(_ % Test)

  val testDependencies = Seq(junit) ++ specsBuild.map(_ % Test) ++ Seq(
    junitInterface,
    guava,
    findBugs,
    ("org.fluentlenium" % "fluentlenium-core" % "0.10.3")
      .exclude("org.jboss.netty", "netty")
  )

  val playCacheDeps = "net.sf.ehcache" % "ehcache-core" % "2.6.10" +:
    specsBuild.map(_ % Test)

  val playWsDeps = Seq(
    guava,
    "com.ning" % "async-http-client" % "1.9.16"
  ) ++ Seq("signpost-core", "signpost-commonshttp4").map("oauth.signpost" % _  % "1.2.1.2") ++
  (specsBuild :+ specsMatcherExtra).map(_ % Test) :+
  mockitoAll % Test

  val playDocsSbtPluginDependencies = Seq(
    "com.typesafe.play" %% "play-doc" % "1.2.2"
  )

}
