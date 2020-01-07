//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

lazy val root = (project in file(".")).enablePlugins(PlayScala)

name := "http-backend-system-property"

scalaVersion := sys.props("scala.version")
updateOptions := updateOptions.value.withLatestSnapshots(false)
evictionWarningOptions in update ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false))

// because the "test" directory clashes with the scripted test file
scalaSource in Test := (baseDirectory.value / "tests")

libraryDependencies ++= Seq(akkaHttpServer, nettyServer, guice, ws, specs2 % Test)

fork in Test := true

javaOptions in Test += "-Dplay.server.provider=play.core.server.NettyServerProvider"

PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode

InputKey[Unit]("verifyResourceContains") := {
  val args       = Def.spaceDelimited("<path> <status> <words> ...").parsed
  val path       = args.head
  val status     = args.tail.head.toInt
  val assertions = args.tail.tail
  ScriptedTools.verifyResourceContains(path, status, assertions)
}
