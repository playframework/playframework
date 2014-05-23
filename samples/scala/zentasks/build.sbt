name := "zentask"

version := "1.0"

libraryDependencies ++= Seq(jdbc, anorm)

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.10.4")

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.3-M1",
  "org.webjars" % "jquery" % "1.7.2"
)

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"
