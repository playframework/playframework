name := """twirl-reload"""
organization := "com.example"

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.8"

libraryDependencies += guice

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin)
  .settings(
    libraryDependencies += guice,
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    InputKey[Unit]("verifyResourceContains") := {
      val args                         = Def.spaceDelimited("<path> <status> <words> ...").parsed
      val path :: status :: assertions = args
      DevModeBuild.verifyResourceContains(path, status.toInt, assertions, 0)
    }
  )


