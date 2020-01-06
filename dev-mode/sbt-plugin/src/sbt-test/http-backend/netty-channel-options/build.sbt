//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//
name := """netty-channel-options"""
organization := "com.lightbend.play"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(PlayNettyServer)
  .disablePlugins(PlayAkkaHttpServer)
  .settings(
    scalaVersion := sys.props.get("scala.version").getOrElse("2.12.9"),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    libraryDependencies ++= Seq(
      guice
    ),
    InputKey[Unit]("callIndex") := {
      DevModeBuild.callIndex()
    },
    InputKey[Unit]("checkLines") := {
      val args                  = Def.spaceDelimited("<source> <target>").parsed
      val source :: target :: _ = args
      DevModeBuild.checkLines(source, target)
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
