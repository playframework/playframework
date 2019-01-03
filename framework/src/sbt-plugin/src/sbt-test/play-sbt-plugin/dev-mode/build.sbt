//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(
    libraryDependencies += guice,
    scalaVersion := sys.props.get("scala.version").getOrElse("2.12.8"),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,

    PlayKeys.fileWatchService := DevModeBuild.initialFileWatchService,

    TaskKey[Unit]("resetReloads") := {
      (target.value / "reload.log").delete()
    },

    InputKey[Unit]("verifyReloads") := {
      val expected = Def.spaceDelimited().parsed.head.toInt
      val actual = IO.readLines(target.value / "reload.log").count(_.nonEmpty)
      if (expected == actual) {
        println(s"Expected and got $expected reloads")
      } else {
        throw new RuntimeException(s"Expected $expected reloads but got $actual")
      }
    },

    InputKey[Unit]("makeRequestWithHeader") := {
      val args = Def.spaceDelimited("<path> <status> <headers> ...").parsed
      val path :: status :: headers = args
      val headerName = headers.mkString
      DevModeBuild.verifyResourceContains(path, status.toInt, Seq.empty, 0, headerName -> "Header-Value")
    },

    InputKey[Unit]("verifyResourceContains") := {
      val args = Def.spaceDelimited("<path> <status> <words> ...").parsed
      val path :: status :: assertions = args
      DevModeBuild.verifyResourceContains(path, status.toInt, assertions, 0)
    }
  )
