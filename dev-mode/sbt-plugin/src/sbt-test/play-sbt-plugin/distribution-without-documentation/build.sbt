//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(
    name := "dist-no-documentation-sample",
    version := "1.0-SNAPSHOT",
    scalaVersion := sys.props("scala.version"),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    evictionWarningOptions in update ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    // actually it should fail on any warning so that we can check that packageBin won't include any documentation
    scalacOptions in Compile := Seq("-Xfatal-warnings", "-deprecation"),
    libraryDependencies += guice,
    play.sbt.PlayImport.PlayKeys.includeDocumentationInBinary := false,
    packageDoc in Compile := file(".")
  )
