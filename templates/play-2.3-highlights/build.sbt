
name := "play-2.3-highlights"

version := "2.3-SNAPSHOT"

lazy val root = (project in file(".")).addPlugins(PlayScala)

libraryDependencies ++= Seq(
  "org.webjars" % "bootstrap" % "3.1.1",
  "org.webjars" % "jquery" % "2.1.0-2",
  "org.webjars" % "requirejs" % "2.1.11-1",
  // Test dependencies
  "org.webjars" % "rjs" % "2.1.11-1-trireme" % "test",
  "org.webjars" % "squirejs" % "0.1.0" % "test"
)

// JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

MochaKeys.requires += "./Setup"
