/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(
    name := "secret-sample",
    version := "1.0-SNAPSHOT",
    libraryDependencies += guice,
    TaskKey[Unit]("checkSecret") := {
      val file: File     = baseDirectory.value / "conf/application.conf"
      val config: Config = ConfigFactory.parseFileAnySyntax(file)
      if (!config.hasPath("play.http.secret.key")) {
        throw new RuntimeException("secret not found!!\n" + file)
      } else {
        config.getString("play.http.secret.key") match {
          case "changeme" => throw new RuntimeException("secret not changed!!\n" + file)
          case _          =>
        }
      }
    }
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
