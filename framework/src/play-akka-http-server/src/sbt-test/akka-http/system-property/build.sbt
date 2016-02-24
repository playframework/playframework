//
// Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
//

lazy val root = (project in file(".")).enablePlugins(PlayScala)

name := "system-property"

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.11.7")

// because the "test" directory clashes with the scripted test file
scalaSource in Test <<= baseDirectory(_ / "tests")

libraryDependencies += "com.typesafe.play" %% "play-akka-http-server-experimental" % sys.props("project.version")

libraryDependencies += ws

libraryDependencies += specs2 % Test

fork in Test := true

javaOptions in Test += "-Dplay.server.provider=play.core.server.akkahttp.AkkaHttpServerProvider"

PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode

InputKey[Unit]("verifyResourceContains") := {
  val args = Def.spaceDelimited("<path> <status> <words> ...").parsed
  val path = args.head
  val status = args.tail.head.toInt
  val assertions = args.tail.tail
  DevModeBuild.verifyResourceContains(path, status, assertions, 0)
}
