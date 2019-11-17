/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

import Common._

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(
    name := "secret-sample",
    version := "1.0-SNAPSHOT",
    scalaVersion := sys.props.get("scala.version").getOrElse("2.12.9"),
    libraryDependencies += guice,
    extraLoggers := {
      val currentFunction = extraLoggers.value
      (key: ScopedKey[_]) => bufferLogger +: currentFunction(key)
    },
    InputKey[Boolean]("checkLogContains") := {
      InputTask.separate[String, Boolean](simpleParser _)(state(s => checkLogContains)).evaluated
    },
    TaskKey[Unit]("compileIgnoreErrors") := state.map { state =>
      Project.runTask(compile in Compile, state)
    }.value
  )

// This is copy/pasted from AkkaSnapshotRepositories since scripted tests also need
// the snapshot resolvers in `cron` builds.
// If this is a cron job in Travis:
// https://docs.travis-ci.com/user/cron-jobs/#detecting-builds-triggered-by-cron
resolvers in ThisBuild ++= (sys.env.get("TRAVIS_EVENT_TYPE").filter(_.equalsIgnoreCase("cron")) match {
  case Some(_) =>
    Seq(
      "akka-snapshot-repository".at("https://repo.akka.io/snapshots"),
      "akka-http-snapshot-repository".at("https://dl.bintray.com/akka/snapshots/")
    )
  case None => Seq.empty
})
