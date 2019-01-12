//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//
import Common._

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)

libraryDependencies ++= Seq(guice, specs2 % Test)

scalaVersion := sys.props.get("scala.version").getOrElse("2.12.8")

// can't use test directory since scripted calls its script "test"
sourceDirectory in Test := baseDirectory.value / "tests"

scalaSource in Test := baseDirectory.value / "tests"

// Generate a js router so we can test it with mocha
val generateJsRouter = TaskKey[Seq[File]]("generate-js-router")
val generateJsRouterBadHost = TaskKey[Seq[File]]("generate-js-router-bad-host")

generateJsRouter := {
  (runMain in Compile).toTask(" utils.JavaScriptRouterGenerator target/web/jsrouter/jsRoutes.js").value
  Seq(target.value / "web" / "jsrouter" / "jsRoutes.js")
}

generateJsRouterBadHost := {
  (runMain in Compile).toTask(
    """ utils.JavaScriptRouterGenerator target/web/jsrouter/jsRoutesBadHost.js "'}}};alert(1);a={a:{a:{a:'" """).value
  Seq(target.value / "web" / "jsrouter" / "jsRoutesBadHost.js")
}

resourceGenerators in TestAssets += generateJsRouter.taskValue
resourceGenerators in TestAssets += generateJsRouterBadHost.taskValue

managedResourceDirectories in TestAssets += target.value / "web" / "jsrouter"

// We don't want source position mappers is this will make it very hard to debug
sourcePositionMappers := Nil

routesGenerator := play.routes.compiler.InjectedRoutesGenerator

play.sbt.routes.RoutesKeys.routesImport := Seq()

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

scalacOptions ++= {
  Seq(
    "-deprecation",
    "-encoding", "UTF-8",
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
