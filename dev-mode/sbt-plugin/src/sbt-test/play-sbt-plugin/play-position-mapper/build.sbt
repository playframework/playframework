// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

@transient val compileIgnoreErrors        = taskKey[Unit]("Compile and capture the expected errors")
@transient val checkCompilerProblem       = inputKey[Unit]("Check the captured compiler errors")
@transient val checkCompilerProblemScala2 = inputKey[Unit]("Check a Scala 2 compiler error")
@transient val checkCompilerProblemScala3 = inputKey[Unit]("Check a Scala 3 compiler error")

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name          := "secret-sample",
    version       := "1.0-SNAPSHOT",
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    libraryDependencies += guice,
    compileIgnoreErrors  := ScriptedTools.compileIgnoringErrors(state.value, fileConverter.value),
    checkCompilerProblem := {
      import sbt.complete.DefaultParsers._
      val expected = (Space ~> any.+.map(_.mkString(""))).parsed
      ScriptedTools.assertCompilerProblemContains(expected)
    },
    checkCompilerProblemScala2 := {
      import sbt.complete.DefaultParsers._
      val expected = (Space ~> any.+.map(_.mkString(""))).parsed
      if (scalaVersion.value.startsWith("2")) ScriptedTools.assertCompilerProblemContains(expected)
    },
    checkCompilerProblemScala3 := {
      import sbt.complete.DefaultParsers._
      val expected = (Space ~> any.+.map(_.mkString(""))).parsed
      if (scalaVersion.value.startsWith("3")) ScriptedTools.assertCompilerProblemContains(expected)
    }
  )
