//
// Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
//

lazy val root = (project in file("."))
  .enablePlugins(PlayService)
  .enablePlugins(RoutesCompiler)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(
    libraryDependencies += guice,
    scalaVersion := sys.props.get("scala.version").getOrElse("2.12.6"),

    InputKey[Unit]("verifyResourceContains") := {
      val args = Def.spaceDelimited("<path> <status> <words> ...").parsed
      val path = args.head
      val status = args.tail.head.toInt
      val assertions = args.tail.tail
      DevModeBuild.verifyResourceContains(path, status, assertions, 0)
    }
  )
