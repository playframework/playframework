/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._

object Dependencies {

  // Some common dependencies here so they don't need to be declared over and over
  val specsVersion = "2.3.7"
  val specsBuild = Seq(
    "org.specs2" %% "specs2-core" % specsVersion,
    "org.specs2" %% "specs2-junit" % specsVersion,
    "org.specs2" %% "specs2-mock" % specsVersion
  )
  val specsSbt = specsBuild
  val scalaIoFile = "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2"

  val guava = "com.google.guava" % "guava" % "16.0.1"
  val findBugs = "com.google.code.findbugs" % "jsr305" % "2.0.2" // Needed by guava
  val mockitoAll = "org.mockito" % "mockito-all" % "1.9.5"

  val h2database = "com.h2database" % "h2" % "1.3.174"

  val jdbcDeps = Seq(
    "com.jolbox" % "bonecp" % "0.8.0.RELEASE",

    h2database,

    "tyrex" % "tyrex" % "1.0.1") ++ specsBuild.map(_ % "test")

  val ebeanDeps = Seq(
    "org.avaje.ebeanorm" % "avaje-ebeanorm" % "3.2.2" exclude ("javax.persistence", "persistence-api"),
    "org.avaje.ebeanorm" % "avaje-ebeanorm-agent" % "3.2.1" exclude ("javax.persistence", "persistence-api")
  )


  val jpaDeps = Seq(
    "org.hibernate.javax.persistence" % "hibernate-jpa-2.0-api" % "1.0.1.Final")

  val javaDeps = Seq(

    "org.yaml" % "snakeyaml" % "1.13",
    "org.hibernate" % "hibernate-validator" % "5.0.1.Final",

    ("org.springframework" % "spring-context" % "3.2.3.RELEASE" notTransitive ())
      .exclude("org.springframework", "spring-aop")
      .exclude("org.springframework", "spring-beans")
      .exclude("org.springframework", "spring-core")
      .exclude("org.springframework", "spring-expression")
      .exclude("org.springframework", "spring-asm"),

    ("org.springframework" % "spring-core" % "3.2.3.RELEASE" notTransitive ())
      .exclude("org.springframework", "spring-asm")
      .exclude("commons-logging", "commons-logging"),

    ("org.springframework" % "spring-beans" % "3.2.3.RELEASE" notTransitive ())
      .exclude("org.springframework", "spring-core"),

    "org.javassist" % "javassist" % "3.18.0-GA",

    ("org.reflections" % "reflections" % "0.9.8" notTransitive ())
      .exclude("javassist", "javassist"),

    guava,
    findBugs,

    "javax.servlet" % "javax.servlet-api" % "3.0.1") ++
    specsBuild.map(_ % "test")

  val javaTestDeps = Seq(
    "junit" % "junit" % "4.11" % "test",
    "com.novocode" % "junit-interface" % "0.11-RC1" % "test",
    "org.easytesting" % "fest-assert" % "1.4" % "test",
    mockitoAll % "test")

  val runtime = Seq(
    "io.netty" % "netty" % "3.7.0.Final",

    "com.typesafe.netty" % "netty-http-pipelining" % "1.1.2",

    "org.slf4j" % "slf4j-api" % "1.7.5",
    "org.slf4j" % "jul-to-slf4j" % "1.7.5",
    "org.slf4j" % "jcl-over-slf4j" % "1.7.5",

    "ch.qos.logback" % "logback-core" % "1.0.13",
    "ch.qos.logback" % "logback-classic" % "1.0.13",

    scalaIoFile,

    "com.typesafe.akka" %% "akka-actor" % "2.3.0",
    "com.typesafe.akka" %% "akka-slf4j" % "2.3.0",

    "org.scala-stm" %% "scala-stm" % "0.7",
    "commons-codec" % "commons-codec" % "1.9",

    "joda-time" % "joda-time" % "2.3",
    "org.joda" % "joda-convert" % "1.5",

    "org.apache.commons" % "commons-lang3" % "3.1",

    "com.fasterxml.jackson.core" % "jackson-core" % "2.3.1",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.3.0",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.3.0",

    "xerces" % "xercesImpl" % "2.11.0",

    "javax.transaction" % "jta" % "1.1",

    guava % "test",

    "org.scala-lang" % "scala-reflect" % BuildSettings.buildScalaVersion) ++
    specsBuild.map(_ % "test") ++
    javaTestDeps


  val link = Seq(
    "org.javassist" % "javassist" % "3.18.0-GA")

  val routersCompilerDependencies = scalaIoFile +:
    specsSbt.map(_ % "test")

  val templatesCompilerDependencies = scalaIoFile +:
    specsSbt.map(_ % "test")

  val sbtDependencies = Seq(
    "org.scala-lang" % "scala-reflect" % BuildSettings.buildScalaVersionForSbt % "provided",
    "com.typesafe" % "config" % "1.0.2",
    "org.mozilla" % "rhino" % "1.7R4",

    ("com.google.javascript" % "closure-compiler" % "v20130603")
      .exclude("args4j", "args4j")
      .exclude("com.google.protobuf", "protobuf-java")
      .exclude("com.google.code.findbugs", "jsr305"),

    guava,
    scalaIoFile,

    "org.avaje.ebeanorm" % "avaje-ebeanorm-agent" % "3.2.1" exclude ("javax.persistence", "persistence-api"),

    h2database,
    "org.javassist" % "javassist" % "3.18.0-GA",

    "net.contentobjects.jnotify" % "jnotify" % "0.94",

    "com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.4.0" extra("sbtVersion" -> BuildSettings.buildSbtVersionBinaryCompatible, "scalaVersion" -> BuildSettings.buildScalaBinaryVersionForSbt),
    "com.github.mpeltonen" % "sbt-idea" % "1.5.1" extra("sbtVersion" -> BuildSettings.buildSbtVersionBinaryCompatible, "scalaVersion" -> BuildSettings.buildScalaBinaryVersionForSbt),
    "com.typesafe.sbt" % "sbt-native-packager" % "0.6.4" extra("sbtVersion" ->  BuildSettings.buildSbtVersionBinaryCompatible, "scalaVersion" -> BuildSettings.buildScalaBinaryVersionForSbt)) ++
    specsSbt

  val playDocsDependencies = Seq(
    "com.typesafe.play" %% "play-doc" % "1.0.3"
  )

  val consoleDependencies = Seq(
    scalaIoFile,
    "org.scala-sbt" % "launcher-interface" % BuildSettings.buildSbtVersion,
    "jline" % "jline" % "2.11"
  )

  val templatesDependencies = scalaIoFile +:
    specsBuild.map(_ % "test")

  val iterateesDependencies = Seq(
    "org.scala-stm" %% "scala-stm" % "0.7",
    "com.typesafe" % "config" % "1.0.2") ++
    specsBuild.map(_ % "test")

  val jsonDependencies = Seq(
    "joda-time" % "joda-time" % "2.2",
    "org.joda" % "joda-convert" % "1.3.1",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.3.0",
    "com.fasterxml.jackson.core" % "jackson-core" % "2.3.1",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.3.0",
    "org.scala-lang" % "scala-reflect" % BuildSettings.buildScalaVersion) ++
    specsBuild.map(_ % "test")

  val scalacheckDependencies = Seq(
    "org.specs2" %% "specs2-scalacheck" % specsVersion % "test",
    "org.scalacheck" %% "scalacheck" % "1.11.3" % "test"
  )

  val testDependencies = Seq("junit" % "junit" % "4.11") ++ specsBuild ++ Seq(
    "com.novocode" % "junit-interface" % "0.10" exclude("junit", "junit-dep"),
    guava,
    findBugs,
    ("org.fluentlenium" % "fluentlenium-festassert" % "0.9.2")
      .exclude("org.jboss.netty", "netty")
  )

  val integrationTestDependencies = scalacheckDependencies ++ Seq(
    "org.databene" % "contiperf" % "2.2.0" % "test"
  )

  val playCacheDeps = "net.sf.ehcache" % "ehcache-core" % "2.6.6" +: 
    specsBuild.map(_ % "test")

  val playWsDeps = Seq(
    guava,
    ("com.ning" % "async-http-client" % "1.7.23" notTransitive ())
      .exclude("org.jboss.netty", "netty"),
    "oauth.signpost" % "signpost-core" % "1.2.1.2",
    "oauth.signpost" % "signpost-commonshttp4" % "1.2.1.2") ++
    specsBuild.map(_ % "test") :+
    mockitoAll % "test"

  val anormDependencies = specsBuild.map(_ % "test") ++ Seq(
    h2database % "test",
    "org.eu.acolyte" %% "jdbc-scala" % "1.0.16-2" % "test",
    "com.chuusai" % "shapeless_2.10.2" % "2.0.0-M1" % "test"
  )

}
