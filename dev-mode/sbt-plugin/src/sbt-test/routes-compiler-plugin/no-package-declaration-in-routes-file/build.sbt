//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//
import Common._

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)

libraryDependencies ++= Seq(guice, specs2 % Test)

scalaVersion := sys.props.get("scala.version").getOrElse("2.12.9")

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

compile in Compile := {
  (compile in Compile).result.value match {
    case Inc(inc) =>
      // If there was a compilation error, dump generated routes files so we can read them
      allFiles((target in routes in Compile).value).map { file =>
        println("Dumping " + file + ":")
        IO.readLines(file).zipWithIndex.foreach {
          case (line, index) => println("%4d".format(index + 1) + ": " + line)
        }
        println()
      }
      throw inc
    case Value(v) => v
  }
}

play.sbt.routes.RoutesKeys.routesImport := Seq()

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
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture"
  )
}

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
