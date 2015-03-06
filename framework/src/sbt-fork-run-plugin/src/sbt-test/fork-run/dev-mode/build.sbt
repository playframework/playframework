lazy val root = (project in file(".")).enablePlugins(PlayScala)

DevModeBuild.settings

fork in run := true

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.10.4")
