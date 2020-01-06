//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

import java.net.URLClassLoader
import com.typesafe.sbt.packager.Keys.executableScriptName

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(
    name := "assets-sample",
    version := "1.0-SNAPSHOT",
    scalaVersion := sys.props.get("scala.version").getOrElse("2.12.9"),
    includeFilter in (Assets, LessKeys.less) := "*.less",
    excludeFilter in (Assets, LessKeys.less) := "_*.less",
    PlayKeys.generateAssetsJar := false
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
