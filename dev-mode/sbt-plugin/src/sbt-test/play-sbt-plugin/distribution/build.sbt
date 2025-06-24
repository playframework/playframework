// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name          := "dist-sample",
    version       := "1.0-SNAPSHOT",
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    libraryDependencies += guice,
    routesGenerator := InjectedRoutesGenerator
  )

val checkStartScript = InputKey[Unit]("checkStartScript")

checkStartScript := {
  val args                                            = Def.spaceDelimited().parsed
  val startScript                                     = target.value / "universal/stage/bin/dist-sample"
  def startScriptError(contents: String, msg: String) = {
    println("Error in start script, dumping contents:")
    println(contents)
    sys.error(msg)
  }
  val contents = IO.read(startScript)
  val lines    = IO.readLines(startScript)
  if (!contents.contains("app_mainclass=('play.core.server.ProdServerStart')")) {
    startScriptError(contents, "Cannot find the declaration of the main class in the script")
  }
  val appClasspath = lines
    .find(_.startsWith("declare -r app_classpath"))
    .getOrElse(startScriptError(contents, "Start script doesn't declare app_classpath"))
  if (args.contains("no-conf")) {
    if (appClasspath.contains("../conf")) {
      startScriptError(contents, "Start script is adding conf directory to the classpath when it shouldn't be")
    }
  } else {
    if (!appClasspath.contains("../conf")) {
      startScriptError(contents, "Start script is not adding conf directory to the classpath when it should be")
    }
  }
}

def retry[B](max: Int = 20, sleep: Long = 500, current: Int = 1)(block: => B): B = {
  try {
    block
  } catch {
    case scala.util.control.NonFatal(e) =>
      if (current == max) {
        throw e
      } else {
        Thread.sleep(sleep)
        retry(max, sleep, current + 1)(block)
      }
  }
}

InputKey[Unit]("checkConfig") := {
  val expected = Def.spaceDelimited().parsed.head
  val config   = retry() {
    IO.readLinesURL(url("http://localhost:9000/config")).mkString("\n")
  }
  if (expected != config) {
    sys.error(s"Expected config $expected but got $config")
  }
}

InputKey[Unit]("countApplicationConf") := {
  val expected = Def.spaceDelimited().parsed.head
  val count    = retry() {
    IO.readLinesURL(url("http://localhost:9000/countApplicationConf")).mkString("\n")
  }
  if (expected != count) {
    sys.error(s"Expected application.conf to be $expected times on classpath, but it was there $count times")
  }
}
