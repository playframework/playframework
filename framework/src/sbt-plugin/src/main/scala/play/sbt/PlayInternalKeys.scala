/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.sbt

import sbt._
import sbt.Keys._

object PlayInternalKeys {
  type ClassLoaderCreator = play.runsupport.Reloader.ClassLoaderCreator

  val playDependencyClasspath = TaskKey[Classpath]("play-dependency-classpath")
  val playReloaderClasspath = TaskKey[Classpath]("play-reloader-classpath")
  val playCommonClassloader = TaskKey[ClassLoader]("play-common-classloader")
  val playDependencyClassLoader = TaskKey[ClassLoaderCreator]("play-dependency-classloader")
  val playReloaderClassLoader = TaskKey[ClassLoaderCreator]("play-reloader-classloader")
  val playReload = TaskKey[sbt.inc.Analysis]("play-reload")
  val buildRequire = TaskKey[Seq[(File, File)]]("play-build-require-assets")
  val playCompileEverything = TaskKey[Seq[sbt.inc.Analysis]]("play-compile-everything")
  val playAssetsWithCompilation = TaskKey[sbt.inc.Analysis]("play-assets-with-compilation")

  val playStop = TaskKey[Unit]("play-stop", "Stop Play, if it has been started in non blocking mode")

  val playAllAssets = TaskKey[Seq[(String, File)]]("play-all-assets")
  val playPrefixAndAssets = TaskKey[(String, File)]("play-prefix-and-assets")
  val playAssetsClassLoader = TaskKey[ClassLoader => ClassLoader]("play-assets-classloader")
}
