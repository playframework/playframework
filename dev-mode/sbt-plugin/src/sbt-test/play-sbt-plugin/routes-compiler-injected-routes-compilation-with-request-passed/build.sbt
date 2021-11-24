//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//
lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .enablePlugins(MediatorWorkaroundPlugin)

libraryDependencies ++= Seq(guice, specs2 % Test)
libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.3.2" // can be removed when dropping Scala 2.12

scalaVersion := sys.props("scala.version")
updateOptions := updateOptions.value.withLatestSnapshots(false)
evictionWarningOptions in update ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false))

// can't use test directory since scripted calls its script "test"
sourceDirectory in Test := baseDirectory.value / "tests"

scalaSource in Test := baseDirectory.value / "tests"

// Generate a js router so we can test it with mocha
val generateJsRouter        = TaskKey[Seq[File]]("generate-js-router")
val generateJsRouterBadHost = TaskKey[Seq[File]]("generate-js-router-bad-host")

generateJsRouter := {
  (runMain in Compile).toTask(" utils.JavaScriptRouterGenerator target/web/jsrouter/jsRoutes.js").value
  Seq(target.value / "web" / "jsrouter" / "jsRoutes.js")
}

generateJsRouterBadHost := {
  (runMain in Compile)
    .toTask(""" utils.JavaScriptRouterGenerator target/web/jsrouter/jsRoutesBadHost.js "'}}};alert(1);a={a:{a:{a:'" """)
    .value
  Seq(target.value / "web" / "jsrouter" / "jsRoutesBadHost.js")
}

resourceGenerators in TestAssets += generateJsRouter.taskValue
resourceGenerators in TestAssets += generateJsRouterBadHost.taskValue

managedResourceDirectories in TestAssets += target.value / "web" / "jsrouter"

// We don't want source position mappers is this will make it very hard to debug
sourcePositionMappers := Nil

routesGenerator := play.routes.compiler.InjectedRoutesGenerator

play.sbt.routes.RoutesKeys.routesImport := Nil
ScriptedTools.dumpRoutesSourceOnCompilationFailure

scalacOptions ++= {
  Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xlint",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  )
}
