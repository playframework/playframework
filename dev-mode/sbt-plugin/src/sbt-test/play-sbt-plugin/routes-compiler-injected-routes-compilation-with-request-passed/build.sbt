// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)

libraryDependencies ++= Seq(guice, specs2 % Test)

scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties()
updateOptions := updateOptions.value.withLatestSnapshots(false)
update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false))

// can't use test directory since scripted calls its script "test"
Test / sourceDirectory := baseDirectory.value / "tests"

Test / scalaSource := baseDirectory.value / "tests"

// Generate a js router so we can test it with mocha
val generateJsRouter        = TaskKey[Seq[File]]("generate-js-router")
val generateJsRouterBadHost = TaskKey[Seq[File]]("generate-js-router-bad-host")

generateJsRouter := {
  (Compile / runMain).toTask(" utils.JavaScriptRouterGenerator target/web/jsrouter/jsRoutes.js").value
  Seq(target.value / "web" / "jsrouter" / "jsRoutes.js")
}

generateJsRouterBadHost := {
  (Compile / runMain)
    .toTask(""" utils.JavaScriptRouterGenerator target/web/jsrouter/jsRoutesBadHost.js "'}}};alert(1);a={a:{a:{a:'" """)
    .value
  Seq(target.value / "web" / "jsrouter" / "jsRoutesBadHost.js")
}

TestAssets / resourceGenerators += generateJsRouter.taskValue
TestAssets / resourceGenerators += generateJsRouterBadHost.taskValue

(TestAssets / managedResourceDirectories) += target.value / "web" / "jsrouter"

// We don't want source position mappers is this will make it very hard to debug
sourcePositionMappers := Nil

routesGenerator := play.routes.compiler.InjectedRoutesGenerator

play.sbt.routes.RoutesKeys.routesImport := Nil
ScriptedTools.dumpRoutesSourceOnCompilationFailure

scalacOptions ++= {
  Seq(
    // "-deprecation",
    // "-encoding",
    // "UTF-8",
    // "-unchecked", // all of them are set in interplay, Scala 3 complains about duplicates
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-Xfatal-warnings",
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) =>
      Seq(
        "-Xlint",
        "-Ywarn-dead-code",
        "-Ywarn-numeric-widen",
      )
    case _ =>
      Seq(
      )
  })
}
