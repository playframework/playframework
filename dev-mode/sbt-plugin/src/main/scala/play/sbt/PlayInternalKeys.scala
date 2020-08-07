/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import sbt._
import sbt.Keys._
import sbt.internal.inc.Analysis

object PlayInternalKeys {

  val playDependencyClasspath = taskKey[Classpath](
    "The classpath containing all the jar dependencies of the project"
  )
  val playCommonClassloader = taskKey[ClassLoader](
    "The common classloader, is used to hold H2 to ensure in memory databases don't get lost between invocations of run"
  )
  val playAssetsClassLoader = taskKey[ClassLoader => ClassLoader](
    "Function that creates a classloader from a given parent that contains all the assets."
  )
  val playCompileEverything = taskKey[Seq[Analysis]](
    "Compiles this project and every project it depends on."
  )
  val playAssetsWithCompilation = taskKey[Analysis](
    "The task that's run on a particular project to compile it. By default, builds assets and runs compile."
  )

  val playStop = taskKey[Unit]("Stop Play, if it has been started in non blocking mode")
}
