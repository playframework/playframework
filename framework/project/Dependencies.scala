/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import buildinfo.BuildInfo

object Dependencies {

  val specsVersion = "2.4.9"
  val specsBuild = Seq(
    "specs2-core",
    "specs2-junit",
    "specs2-mock"
  ).map("org.specs2" %% _ % specsVersion)

  val specsMatcherExtra = "org.specs2" %% "specs2-matcher-extra" % specsVersion

  val specsSbt = specsBuild

  val jacksons = Seq(
    "jackson-core",
    "jackson-annotations",
    "jackson-databind"
  ).map("com.fasterxml.jackson.core" % _ % "2.4.4")

  val guava = "com.google.guava" % "guava" % "18.0"
  val findBugs = "com.google.code.findbugs" % "jsr305" % "2.0.3" // Needed by guava
  val mockitoAll = "org.mockito" % "mockito-all" % "1.10.8"

  val h2database = "com.h2database" % "h2" % "1.4.182"

  val acolyteVersion = "1.0.30"
  val acolyte = "org.eu.acolyte" % "jdbc-driver" % acolyteVersion

  val jdbcDeps = Seq(
    "com.jolbox" % "bonecp" % "0.8.0.RELEASE",
    h2database,
    acolyte % Test,
    "tyrex" % "tyrex" % "1.0.1") ++ specsBuild.map(_ % Test)

  val javaJdbcDeps = Seq(acolyte % Test)

  val jpaDeps = Seq(
    "org.hibernate.javax.persistence" % "hibernate-jpa-2.1-api" % "1.0.0.Final",
    "org.hibernate" % "hibernate-entitymanager" % "4.3.7.Final" % "test"
  )

  val link = Seq(
    "org.javassist" % "javassist" % "3.18.2-GA"
  )
  val javassist = link

  val javaDeps = Seq(
    "org.yaml" % "snakeyaml" % "1.13",
    // 5.1.0 upgrade notes: need to add JEE dependencies, eg EL
    "org.hibernate" % "hibernate-validator" % "5.0.3.Final",
    // This is depended on by hibernate validator, we upgrade to 3.2.0 to avoid LGPL license of 3.1.x
    "org.jboss.logging" % "jboss-logging" % "3.2.0.Final",

    ("org.springframework" % "spring-context" % "4.1.1.RELEASE" notTransitive ())
      .exclude("org.springframework", "spring-aop")
      .exclude("org.springframework", "spring-beans")
      .exclude("org.springframework", "spring-core")
      .exclude("org.springframework", "spring-expression")
      .exclude("org.springframework", "spring-asm"),

    ("org.springframework" % "spring-core" % "4.1.1.RELEASE" notTransitive ())
      .exclude("org.springframework", "spring-asm")
      .exclude("commons-logging", "commons-logging"),

    ("org.springframework" % "spring-beans" % "4.1.1.RELEASE" notTransitive ())
      .exclude("org.springframework", "spring-core"),

    ("org.reflections" % "reflections" % "0.9.8" notTransitive ())
      .exclude("javassist", "javassist"),

    guava,
    findBugs,

    "org.apache.tomcat" % "tomcat-servlet-api" % "8.0.14"
  ) ++ javassist ++ specsBuild.map(_ % Test)

  val junitInterface = "com.novocode" % "junit-interface" % "0.11"
  val junit = "junit" % "junit" % "4.11"

  val javaTestDeps = Seq(
    junit,
    junitInterface,
    "org.easytesting" % "fest-assert"     % "1.4",
    mockitoAll
  ).map(_ % Test)

  val jodatime = "joda-time" % "joda-time" % "2.6"
  val jodaConvert = "org.joda" % "joda-convert" % "1.7"

  val runtime = Seq("slf4j-api", "jul-to-slf4j", "jcl-over-slf4j").map("org.slf4j" % _ % "1.7.6") ++
    Seq("logback-core", "logback-classic").map("ch.qos.logback" % _ % "1.1.1") ++
    Seq("akka-actor", "akka-slf4j").map("com.typesafe.akka" %% _ % "2.3.7") ++
    jacksons ++
    Seq(
      "org.scala-stm" %% "scala-stm" % "0.7",
      "commons-codec" % "commons-codec" % "1.9",

      jodatime,
      jodaConvert,

      "org.apache.commons" % "commons-lang3" % "3.3.2",

      "xerces" % "xercesImpl" % "2.11.0",

      "javax.transaction" % "jta" % "1.1",

      // Since we don't use any of the AOP features of guice, we exclude cglib.
      // This solves issues later where cglib depends on an older version of asm,
      // and other libraries (pegdown) depend on a newer version with a different groupId,
      // and this causes binary issues.
      "com.google.inject" % "guice" % "3.0" exclude("org.sonatype.sisu.inject", "cglib"),

      guava % Test,

      "org.scala-lang" % "scala-reflect" % BuildSettings.buildScalaVersion
    ) ++
    specsBuild.map(_ % Test) ++
    javaTestDeps

  val netty = Seq(
    "io.netty"           % "netty"                 % "3.9.3.Final",
    "com.typesafe.netty" % "netty-http-pipelining" % "1.1.2"
  ) ++ specsBuild.map(_ % Test)

  val akkaHttp = Seq(
    "com.typesafe.akka" %% "akka-http-core-experimental" % "1.0-M1"
  )

  val routersCompilerDependencies =  Seq(
    "commons-io" % "commons-io" % "2.0.1"
  ) ++ specsBuild.map(_ % Test)

  private def sbtPluginDep(moduleId: ModuleID) = {
    moduleId.extra(
      "sbtVersion" -> BuildSettings.buildSbtVersionBinaryCompatible,
      "scalaVersion" -> BuildSettings.buildScalaBinaryVersionForSbt
    )
  }

  def runSupportDependencies(scalaBinaryVersion: String) = Seq(
    sbtIO(scalaBinaryVersion)
  ) ++ specsBuild.map(_ % Test)

  def sbtIO(scalaBinaryVersion: String): ModuleID = scalaBinaryVersion match {
    case "2.10" => "org.scala-sbt" % "io" % BuildSettings.buildSbtVersion % "provided"
    case "2.11" => "org.scala-sbt" % "io_2.11" % "0.13.6" % "provided"
  }

  val typesafeConfig = "com.typesafe" % "config" % "1.2.1"

  val sbtDependencies = Seq(
    "org.scala-lang" % "scala-reflect" % BuildSettings.buildScalaVersionForSbt % "provided",
    typesafeConfig,
    "org.mozilla" % "rhino" % "1.7R4",

    ("com.google.javascript" % "closure-compiler" % "v20140814")
      .exclude("args4j", "args4j")
      .exclude("com.google.protobuf", "protobuf-java")
      .exclude("com.google.code.findbugs", "jsr305"),

    guava,

    h2database,

    "net.contentobjects.jnotify" % "jnotify" % "0.94",

    sbtPluginDep("com.typesafe.sbt" % "sbt-twirl" % BuildInfo.sbtTwirlVersion),
    sbtPluginDep("com.typesafe.sbt" % "sbt-play-enhancer" % "1.0.1"),

    sbtPluginDep("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.5.0"),
    sbtPluginDep("com.github.mpeltonen" % "sbt-idea" % "1.6.0"),
    sbtPluginDep("com.typesafe.sbt" % "sbt-native-packager" % BuildInfo.sbtNativePackagerVersion),

    sbtPluginDep("com.typesafe.sbt" % "sbt-web" % "1.1.1"),
    sbtPluginDep("com.typesafe.sbt" % "sbt-js-engine" % "1.0.2"),
    sbtPluginDep("com.typesafe.sbt" % "sbt-webdriver" % "1.0.0")
  ) ++ javassist ++ specsBuild.map(_ % Test)

  val playdocWebjarDependencies = Seq(
    "org.webjars" % "jquery"   % "2.1.0-2"    % "webjars",
    "org.webjars" % "prettify" % "4-Mar-2013" % "webjars"
  )

  val playDocsDependencies = Seq(
    "com.typesafe.play" %% "play-doc" % "1.2.0"
  ) ++ playdocWebjarDependencies

  val iterateesDependencies = Seq(
    "org.scala-stm" %% "scala-stm" % "0.7",
    typesafeConfig
  ) ++ specsBuild.map(_ % Test)

  val streamsDependencies = Seq(
    "org.reactivestreams" % "reactive-streams" % "1.0.0.M1"
  ) ++ specsBuild.map(_ % "test")

  val jsonDependencies = Seq(
    jodatime,
    jodaConvert,
    "org.scala-lang" % "scala-reflect" % BuildSettings.buildScalaVersion) ++
  jacksons ++
  specsBuild.map(_ % Test)

  val scalacheckDependencies = Seq(
    "org.specs2"     %% "specs2-scalacheck" % specsVersion % Test,
    "org.scalacheck" %% "scalacheck"        % "1.11.3"     % Test
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

  val playCacheDeps = "net.sf.ehcache" % "ehcache-core" % "2.6.9" +:
    specsBuild.map(_ % Test)

  val playWsDeps = Seq(
    guava,
    "com.ning" % "async-http-client" % "1.8.15"
  ) ++ Seq("signpost-core", "signpost-commonshttp4").map("oauth.signpost" % _  % "1.2.1.2") ++
  (specsBuild :+ specsMatcherExtra).map(_ % Test) :+
  mockitoAll % Test

  val playDocsSbtPluginDependencies = Seq(
    "com.typesafe.play" %% "play-doc" % "1.2.0"
  )

}
