import sbt._
import sbt.Keys._

object Dependencies {

  // Some common dependencies here so they don't need to be declared over and over
  val specsBuild = "org.specs2" %% "specs2" % "2.1.1"
  val specsSbt = specsBuild
  val scalaIoFile = "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2"

  val guava = "com.google.guava" % "guava" % "14.0.1"
  // Needed by guava
  val findBugs = "com.google.code.findbugs" % "jsr305" % "2.0.1"


  val jdbcDeps = Seq(
    "com.jolbox" % "bonecp" % "0.8.0.RELEASE" exclude ("com.google.guava", "guava"),

    // bonecp needs it, but due to guavas stupid version numbering of older versions ("r08"), we need to explicitly
    // declare a dependency on the newer version so that ivy can know which one to include
    guava,

    "com.h2database" % "h2" % "1.3.172",

    "tyrex" % "tyrex" % "1.0.1",

    specsBuild % "test")

  val ebeanDeps = Seq(
    "org.avaje.ebeanorm" % "avaje-ebeanorm" % "3.2.2" exclude ("javax.persistence", "persistence-api"),
    "org.avaje.ebeanorm" % "avaje-ebeanorm-agent" % "3.2.1" exclude ("javax.persistence", "persistence-api")
  )


  val jpaDeps = Seq(
    "org.hibernate.javax.persistence" % "hibernate-jpa-2.0-api" % "1.0.1.Final")

  val javaDeps = Seq(

    "org.yaml" % "snakeyaml" % "1.12",
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
      .exclude("com.google.guava", "guava")
      .exclude("javassist", "javassist"),

    guava,
    findBugs,

    "javax.servlet" % "javax.servlet-api" % "3.0.1",

    specsBuild % "test")

  val runtime = Seq(
    "io.netty" % "netty" % "3.7.0.Final",

    "com.typesafe.netty" % "netty-http-pipelining" % "1.1.2",

    "org.slf4j" % "slf4j-api" % "1.7.5",
    "org.slf4j" % "jul-to-slf4j" % "1.7.5",
    "org.slf4j" % "jcl-over-slf4j" % "1.7.5",

    "ch.qos.logback" % "logback-core" % "1.0.13",
    "ch.qos.logback" % "logback-classic" % "1.0.13",

    scalaIoFile,

    "com.typesafe.akka" %% "akka-actor" % "2.2.0",
    "com.typesafe.akka" %% "akka-slf4j" % "2.2.0",

    "org.scala-stm" %% "scala-stm" % "0.7",


    "joda-time" % "joda-time" % "2.2",
    "org.joda" % "joda-convert" % "1.3.1",

    "org.apache.commons" % "commons-lang3" % "3.1",

    ("com.ning" % "async-http-client" % "1.7.18" notTransitive ())
      .exclude("org.jboss.netty", "netty"),

    "oauth.signpost" % "signpost-core" % "1.2.1.2",
    "oauth.signpost" % "signpost-commonshttp4" % "1.2.1.2",

    "com.fasterxml.jackson.core" % "jackson-core" % "2.2.2",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.2.2",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.2",

    "xerces" % "xercesImpl" % "2.11.0",

    "javax.transaction" % "jta" % "1.1",

    specsBuild % "test",

    "org.mockito" % "mockito-all" % "1.9.5" % "test",
    "com.novocode" % "junit-interface" % "0.10" % "test" exclude("junit", "junit-dep"),

    ("org.fluentlenium" % "fluentlenium-festassert" % "0.9.0" % "test")
      .exclude("org.jboss.netty", "netty"),

    "org.scala-lang" % "scala-reflect" % BuildSettings.buildScalaVersion,

    "org.databene" % "contiperf" % "2.2.0" % "test",
    "junit" % "junit" % "4.11" % "test")

  val link = Seq(
    "org.javassist" % "javassist" % "3.18.0-GA")

  val routersCompilerDependencies = Seq(
    scalaIoFile,
    specsSbt % "test"
  )

  val templatesCompilerDependencies = Seq(
    scalaIoFile,
    specsSbt % "test"
  )

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

    "com.h2database" % "h2" % "1.3.172",
    "org.javassist" % "javassist" % "3.18.0-GA",

    "net.contentobjects.jnotify" % "jnotify" % "0.94",

    "com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.4.0" extra("sbtVersion" -> BuildSettings.buildSbtVersionBinaryCompatible, "scalaVersion" -> BuildSettings.buildScalaBinaryVersionForSbt),
    "com.github.mpeltonen" % "sbt-idea" % "1.5.1" extra("sbtVersion" -> BuildSettings.buildSbtVersionBinaryCompatible, "scalaVersion" -> BuildSettings.buildScalaBinaryVersionForSbt),
    "com.typesafe.sbt" % "sbt-native-packager" % "0.6.4" extra("sbtVersion" ->  BuildSettings.buildSbtVersionBinaryCompatible, "scalaVersion" -> BuildSettings.buildScalaBinaryVersionForSbt),

    specsSbt
  )

  val playDocsDependencies = Seq(
    "com.typesafe.play" %% "play-doc" % "1.0.3"
  )

  val consoleDependencies = Seq(
    scalaIoFile,
    "org.scala-sbt" % "launcher-interface" % BuildSettings.buildSbtVersion,
    "jline" % "jline" % "2.11"
  )

  val templatesDependencies = Seq(
    scalaIoFile,
    specsBuild % "test")

  val iterateesDependencies = Seq(
    "org.scala-stm" %% "scala-stm" % "0.7",
    "com.typesafe" % "config" % "1.0.2",
    specsBuild % "test")

  val jsonDependencies = Seq(
    "joda-time" % "joda-time" % "2.2",
    "org.joda" % "joda-convert" % "1.3.1",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.2.2",
    "com.fasterxml.jackson.core" % "jackson-core" % "2.2.2",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.2",
    "org.scala-lang" % "scala-reflect" % BuildSettings.buildScalaVersion,
    specsBuild % "test")

  val testDependencies = Seq(
    "junit" % "junit" % "4.11",
    specsBuild,
    "com.novocode" % "junit-interface" % "0.10" exclude("junit", "junit-dep"),
    guava,
    findBugs,
    ("org.fluentlenium" % "fluentlenium-festassert" % "0.8.0")
      .exclude("org.jboss.netty", "netty")
      .exclude("com.google.guava","guava"))

  val playCacheDeps = Seq(
    "net.sf.ehcache" % "ehcache-core" % "2.6.6",
    specsBuild % "test"
  )

}
