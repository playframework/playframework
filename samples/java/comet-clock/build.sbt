name := "comet-clock"

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.10.4")

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.3.0",
  "org.webjars" % "jquery" % "1.7.2"
)