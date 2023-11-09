/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import java.net.URL

import sbt._
import sbt.internal.inc.Analysis
import sbt.Keys._

object PlayInternalKeys {
  type ClassLoaderCreator = (String, Array[URL], ClassLoader) => ClassLoader

  val playDependencyClasspath = taskKey[Classpath](
    "The classpath containing all the jar dependencies of the project"
  )
  val playReloaderClasspath = taskKey[Classpath](
    "The application classpath, containing all projects in this build that are dependencies of this project, including this project"
  )
  val playCommonClassloader = taskKey[ClassLoader](
    "The common classloader, is used to hold H2 to ensure in memory databases don't get lost between invocations of run"
  )
  val playDependencyClassLoader = taskKey[ClassLoaderCreator](
    "A function to create the dependency classloader from a name, set of URLs and parent classloader"
  )
  val playReloaderClassLoader = taskKey[ClassLoaderCreator](
    "A function to create the application classloader from a name, set of URLs and parent classloader"
  )
  val playAssetsClassLoader = taskKey[ClassLoader => ClassLoader](
    "Function that creates a classloader from a given parent that contains all the assets."
  )
  val playReload = taskKey[Analysis](
    "Executed when sources of changed, to recompile (and possibly reload) the app"
  )
  val playCompileEverything = taskKey[Seq[Analysis]](
    "Compiles this project and every project it depends on."
  )
  val playAssetsWithCompilation = taskKey[Analysis](
    "The task that's run on a particular project to compile it. By default, builds assets and runs compile."
  )

  val playStop = taskKey[Unit]("Stop Play, if it has been started in non blocking mode")

  val playAllAssets       = taskKey[Seq[(String, File)]]("Compiles all assets for all projects")
  val playPrefixAndAssets = taskKey[(String, File)]("Gets all the assets with their associated prefixes")
}
