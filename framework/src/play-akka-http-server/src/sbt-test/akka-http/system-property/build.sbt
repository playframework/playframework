//
// Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
//

lazy val root = (project in file(".")).enablePlugins(PlayScala)

name := "system-property"

scalaVersion := sys.props.get("scala.version").getOrElse("2.12.6")

// because the "test" directory clashes with the scripted test file
scalaSource in Test := (baseDirectory.value / "tests")

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-akka-http-server" % sys.props("project.version"),
  guice,
  ws,
  specs2 % Test
)

fork in Test := true

javaOptions in Test += "-Dplay.server.provider=play.core.server.AkkaHttpServerProvider"

PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode

InputKey[Unit]("verifyResourceContains") := {
  val args = Def.spaceDelimited("<path> <status> <words> ...").parsed
  val path = args.head
  val status = args.tail.head.toInt
  val assertions = args.tail.tail
  DevModeBuild.verifyResourceContains(path, status, assertions, 0)
}
