// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name          := "dist-no-documentation-sample",
    version       := "1.0-SNAPSHOT",
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    // actually it should fail on any warning so that we can check that packageBin won't include any documentation
    Compile / scalacOptions := Seq("-Xfatal-warnings", "-deprecation"),
    libraryDependencies += guice,
    play.sbt.PlayImport.PlayKeys.includeDocumentationInBinary := false,
    Compile / packageDoc                                      := file(".")
  )
Global / resolvers += "scala-integration".at("https://scala-ci.typesafe.com/artifactory/scala-integration/")
