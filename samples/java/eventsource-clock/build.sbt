name := "eventSource-clock"

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.10.4")