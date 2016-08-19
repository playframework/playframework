name := "play-java-intro"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "%SCALA_VERSION%"

libraryDependencies ++= Seq(
  guice,
  // If you enable PlayEbean plugin you must remove these
  // JPA dependencies to avoid conflicts.
  javaJpa,
  "com.h2database" % "h2" % "1.4.191",
  "org.hibernate" % "hibernate-entitymanager" % "5.1.0.Final"
)

