import sbt._

object Dependencies {

  // Some common dependencies here so they don't need to be declared over and over
  val specsBuild = "org.specs2" %% "specs2" % "1.14"
  val scalaIoFileBuild = "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2"
  val scalaIoFileSbt = "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.1" exclude ("javax.transaction", "jta")
  val guava = "com.google.guava" % "guava" % "14.0.1"


  val jdbcDeps = Seq(
    "com.jolbox" % "bonecp" % "0.7.1.RELEASE" exclude ("com.google.guava", "guava"),

    // bonecp needs it, but due to guavas stupid version numbering of older versions ("r08"), we need to explicitly
    // declare a dependency on the newer version so that ivy can know which one to include
    guava,

    "com.h2database" % "h2" % "1.3.171",

    "tyrex" % "tyrex" % "1.0.1",

    specsBuild % "test")

  val ebeanDeps = Seq(
    "org.avaje.ebeanorm" % "avaje-ebeanorm" % "3.1.2" exclude ("javax.persistence", "persistence-api"))

  val jpaDeps = Seq(
    "org.hibernate.javax.persistence" % "hibernate-jpa-2.0-api" % "1.0.1.Final")

  val javaDeps = Seq(

    "org.yaml" % "snakeyaml" % "1.10",
    "org.hibernate" % "hibernate-validator" % "4.3.0.Final",

    ("org.springframework" % "spring-context" % "3.1.2.RELEASE" notTransitive ())
      .exclude("org.springframework", "spring-aop")
      .exclude("org.springframework", "spring-beans")
      .exclude("org.springframework", "spring-core")
      .exclude("org.springframework", "spring-expression")
      .exclude("org.springframework", "spring-asm"),

    ("org.springframework" % "spring-core" % "3.1.2.RELEASE" notTransitive ())
      .exclude("org.springframework", "spring-asm")
      .exclude("commons-logging", "commons-logging"),

    ("org.springframework" % "spring-beans" % "3.1.2.RELEASE" notTransitive ())
      .exclude("org.springframework", "spring-core"),

    "org.javassist" % "javassist" % "3.16.1-GA",

    ("org.reflections" % "reflections" % "0.9.8" notTransitive ())
      .exclude("com.google.guava", "guava")
      .exclude("javassist", "javassist"),

    guava,

    "com.google.code.findbugs" % "jsr305" % "2.0.1",

    "javax.servlet" % "javax.servlet-api" % "3.0.1",

    specsBuild % "test")

  val runtime = Seq(
    "io.netty" % "netty" % "3.6.3.Final",

    "com.typesafe.netty" % "netty-http-pipelining" % "1.0.0",

    "org.slf4j" % "slf4j-api" % "1.6.6",
    "org.slf4j" % "jul-to-slf4j" % "1.6.6",
    "org.slf4j" % "jcl-over-slf4j" % "1.6.6",

    "ch.qos.logback" % "logback-core" % "1.0.7",
    "ch.qos.logback" % "logback-classic" % "1.0.7",

    scalaIoFileBuild,

    "com.typesafe.akka" %% "akka-actor" % "2.1.0",
    "com.typesafe.akka" %% "akka-slf4j" % "2.1.0",

    "org.scala-stm" % "scala-stm_2.10.0" % "0.6",

    "joda-time" % "joda-time" % "2.1",
    "org.joda" % "joda-convert" % "1.2",

    "org.apache.commons" % "commons-lang3" % "3.1",

    ("com.ning" % "async-http-client" % "1.7.6" notTransitive ())
      .exclude("org.jboss.netty", "netty"),

    "oauth.signpost" % "signpost-core" % "1.2.1.2",
    "oauth.signpost" % "signpost-commonshttp4" % "1.2.1.2",

    "com.fasterxml.jackson.core" % "jackson-core" % "2.1.1",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.1.1",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.1.1",

    "javax.transaction" % "jta" % "1.1",

    specsBuild % "test",

    "org.mockito" % "mockito-all" % "1.9.0" % "test",
    "com.novocode" % "junit-interface" % "0.10-M4" % "test",

   ("org.fluentlenium" % "fluentlenium-festassert" % "0.8.0" % "test").exclude("org.jboss.netty", "netty").exclude("comm.google.guava","guava"),
    "org.scala-lang" % "scala-reflect" % "2.10.0",

    "org.databene" % "contiperf" % "2.2.0" % "test",
    "junit" % "junit" % "4.11" % "test")

  val link = Seq(
    "org.javassist" % "javassist" % "3.16.1-GA")

  val routersCompilerDependencies = Seq(
    scalaIoFileSbt,
    "org.specs2" %% "specs2" % "1.12.3" % "test" exclude ("javax.transaction", "jta"))

  val templatesCompilerDependencies = Seq(
    scalaIoFileSbt,
    "org.specs2" %% "specs2" % "1.12.3" % "test"
      exclude ("javax.transaction", "jta"))

  val sbtDependencies = Seq(
    "com.typesafe" % "config" % "1.0.0",
    "org.mozilla" % "rhino" % "1.7R4",

    ("com.google.javascript" % "closure-compiler" % "rr2079.1" notTransitive ())
      .exclude("args4j", "args4j")
      .exclude("com.google.guava", "guava")
      .exclude("org.json", "json")
      .exclude("com.google.protobuf", "protobuf-java")
      .exclude("org.apache.ant", "ant")
      .exclude("com.google.code.findbugs", "jsr305")
      .exclude("com.googlecode.jarjar", "jarjar")
      .exclude("junit", "junit"),

    ("com.google.guava" % "guava" % "10.0.1" notTransitive ())
      .exclude("com.google.code.findbugs", "jsr305"),

    "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.1" exclude ("javax.transaction", "jta"),

    "org.avaje.ebeanorm" % "avaje-ebeanorm" % "3.1.2" exclude ("javax.persistence", "persistence-api"),

    "com.h2database" % "h2" % "1.3.168",
    "org.javassist" % "javassist" % "3.16.1-GA",
    "org.pegdown" % "pegdown" % "1.2.1-withplugins-1",

    "net.contentobjects.jnotify" % "jnotify" % "0.94")

  val consoleDependencies = Seq(
    scalaIoFileSbt,
    "org.scala-sbt" % "launcher-interface" % BuildSettings.buildSbtVersion,
    "jline" % "jline" % "1.0"
  )

  val templatesDependencies = Seq(
    scalaIoFileBuild,
    specsBuild % "test")

  val iterateesDependencies = Seq(
    "org.scala-stm" % "scala-stm_2.10.0" % "0.6",
    "com.typesafe" % "config" % "1.0.0",
    specsBuild % "test")

  val jsonDependencies = Seq(
    "joda-time" % "joda-time" % "2.1",
    "org.joda" % "joda-convert" % "1.2",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.1.1",
    "com.fasterxml.jackson.core" % "jackson-core" % "2.1.1",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.1.1",
    "org.scala-lang" % "scala-reflect" % "2.10.0",
    specsBuild % "test")

  val testDependencies = Seq(
    "junit" % "junit" % "4.11",
    specsBuild,
    "com.novocode" % "junit-interface" % "0.10-M4",
   ("com.google.guava" % "guava" % "10.0.1" notTransitive()),
    ("org.fluentlenium" % "fluentlenium-festassert" % "0.8.0").exclude("org.jboss.netty", "netty").exclude("com.google.guava","guava"))

  val playCacheDeps = Seq(
    "net.sf.ehcache" % "ehcache-core" % "2.6.0",
    specsBuild % "test"
  )

}
