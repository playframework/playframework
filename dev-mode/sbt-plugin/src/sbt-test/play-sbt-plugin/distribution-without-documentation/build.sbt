//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(
    name := "dist-no-documentation-sample",
    version := "1.0-SNAPSHOT",
    // actually it should fail on any warning so that we can check that packageBin won't include any documentation
    scalacOptions in Compile := Seq("-Xfatal-warnings", "-deprecation"),
    libraryDependencies += guice,
    scalaVersion := sys.props.get("scala.version").getOrElse("2.12.9"),
    play.sbt.PlayImport.PlayKeys.includeDocumentationInBinary := false,
    packageDoc in Compile := { new File(".") }
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
