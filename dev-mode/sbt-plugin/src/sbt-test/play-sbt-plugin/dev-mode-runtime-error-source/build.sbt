//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies += guice,
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
  )
  .dependsOn(`sub-project-inside`, `sub-project-outside`)
  .aggregate(`sub-project-inside`, `sub-project-outside`)

def commonSettings: Seq[Setting[_]] = Seq(
  scalaVersion := sys.props("scala.version"),
  updateOptions := updateOptions.value.withLatestSnapshots(false),
  evictionWarningOptions in update ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
  // This makes it possible to run tests on the output regardless of scala version
  crossPaths := false
)

lazy val `sub-project-inside` = (project in file("./modules/sub-project-inside"))
  .settings(commonSettings: _*)

lazy val `sub-project-outside` = ProjectRef(file("./dev-mode-runtime-error-source-sub-project-outside"), "sub-project-outside")
