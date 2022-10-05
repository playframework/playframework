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
  scalaVersion := (sys.props("scala.crossversions").split(" ").toSeq.filter(v => SemanticSelector(sys.props("scala.version")).matches(VersionNumber(v))) match {
    case Nil => sys.error("Unable to detect scalaVersion! Did you pass scala.crossversions and scala.version Java properties?")
    case Seq(version) => version
    case multiple => sys.error(s"Multiple crossScalaVersions matched query '${sys.props("scala.version")}': ${multiple.mkString(", ")}")
  }),
  updateOptions := updateOptions.value.withLatestSnapshots(false),
  update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
  // This makes it possible to run tests on the output regardless of scala version
  crossPaths := false
)

lazy val `sub-project-inside` = (project in file("./modules/sub-project-inside"))
  .settings(commonSettings: _*)

lazy val `sub-project-outside` = ProjectRef(file("./dev-mode-runtime-error-source-sub-project-outside"), "sub-project-outside")
