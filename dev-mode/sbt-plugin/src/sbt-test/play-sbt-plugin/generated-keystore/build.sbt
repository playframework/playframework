//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

lazy val root = (project in file("."))
  .enablePlugins(PlayService)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(
    libraryDependencies += guice,
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    scalaVersion := sys.props.get("scala.version").getOrElse("2.12.9"),
    InputKey[Unit]("makeRequest") := {
      val args                      = Def.spaceDelimited("<path> <status> ...").parsed
      val path :: status :: headers = args
      DevModeBuild.verifyResourceContains(path, status.toInt, Seq.empty, 0)
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
