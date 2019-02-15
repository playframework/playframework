//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//

lazy val root = (project in file("."))
  .enablePlugins(PlayService)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(
    libraryDependencies += guice,
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    scalaVersion := sys.props.get("scala.version").getOrElse("2.12.8"),

    InputKey[Unit]("makeRequest") := {
      val args = Def.spaceDelimited("<path> <status> ...").parsed
      val path :: status :: headers = args
      DevModeBuild.verifyResourceContains(path, status.toInt, Seq.empty, 0)
    }
  )
