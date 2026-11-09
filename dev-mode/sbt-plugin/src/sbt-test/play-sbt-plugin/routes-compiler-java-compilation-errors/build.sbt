// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

@transient val compileIgnoreErrors  = taskKey[Unit]("Compile and capture the expected errors")
@transient val checkCompilerProblem = inputKey[Unit]("Check the captured compiler errors")

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(
    scalaVersion := ScriptedTools.scalaVersionFromJavaProperties(),
    routesLang   := play.routes.compiler.Language.JAVA,
    libraryDependencies += guice,
    compileIgnoreErrors  := ScriptedTools.compileIgnoringErrors(state.value, fileConverter.value),
    checkCompilerProblem := {
      import sbt.complete.DefaultParsers._
      val expected = (Space ~> any.+.map(_.mkString(""))).parsed
      ScriptedTools.assertCompilerProblemContains(expected)
    },
  )
