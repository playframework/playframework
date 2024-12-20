// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val root = (project in file(".")).enablePlugins(PlayScala)

name := "http-backend-system-property"

scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties()
updateOptions := updateOptions.value.withLatestSnapshots(false)
update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false))

// because the "test" directory clashes with the scripted test file
(Test / scalaSource) := (baseDirectory.value / "tests")

libraryDependencies ++= Seq(pekkoHttpServer, nettyServer, guice, ws, specs2 % Test)

Test / fork := true

Test / javaOptions += "-Dplay.server.provider=play.core.server.NettyServerProvider"

PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode

InputKey[Unit]("verifyResourceContains") := {
  val args       = Def.spaceDelimited("<path> <status> <words> ...").parsed
  val path       = args.head
  val status     = args.tail.head.toInt
  val assertions = args.tail.tail
  ScriptedTools.verifyResourceContains(path, status, assertions)
}
Global / resolvers += "scala-integration".at("https://scala-ci.typesafe.com/artifactory/scala-integration/")
