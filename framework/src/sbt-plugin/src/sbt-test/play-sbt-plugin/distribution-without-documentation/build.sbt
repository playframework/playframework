//
// Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
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
    scalaVersion := sys.props.get("scala.version").getOrElse("2.12.6"),
    play.sbt.PlayImport.PlayKeys.includeDocumentationInBinary := false,
    packageDoc in Compile := { new File(".") }
  )
