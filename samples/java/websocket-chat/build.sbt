name := "websocket-chat"

version := "1.0"

javacOptions += "-Xlint:deprecation"     

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.10.4")