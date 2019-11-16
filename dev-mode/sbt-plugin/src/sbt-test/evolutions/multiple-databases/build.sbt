//
// Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
//
name := """multiple-databases"""
organization := "com.lightbend.play"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(
    scalaVersion := "2.12.9",
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    libraryDependencies ++= Seq(
      guice,
      javaJdbc,
      evolutions,
      "com.h2database" % "h2" % "1.4.197"
    ),
    InputKey[Unit]("applyEvolutions") := {
      val args        = Def.spaceDelimited("<path>").parsed
      val path :: Nil = args
      DevModeBuild.applyEvolutions(path)
    },
    InputKey[Unit]("verifyResourceContains") := {
      val args                         = Def.spaceDelimited("<path> <status> <words> ...").parsed
      val path :: status :: assertions = args
      DevModeBuild.verifyResourceContains(path, status.toInt, assertions, 0)
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
