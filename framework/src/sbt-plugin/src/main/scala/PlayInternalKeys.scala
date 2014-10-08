/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play

import sbt._
import sbt.Keys._
import play.runsupport.protocol.PlayForkSupportResult

trait PlayInternalKeys {
  type ClassLoaderCreator = (String, Array[URL], ClassLoader) => ClassLoader

  val playNotifyServerStart = inputKey[Unit]("Sends an event when the forked dev-server has started")
  val playBackgroundRunTaskBuilder = TaskKey[Seq[String] => BackgroundJobHandle]("play-background-run-task-builder")
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
  val playPrefixAndPipeline = TaskKey[(String, Seq[(File, String)])]("play-prefix-and-pipeline")
  val playAssetsClassLoader = TaskKey[ClassLoader => ClassLoader]("play-assets-classloader")
  val playPackageAssetsMappings = TaskKey[Seq[(File, String)]]("play-package-assets-mappings")

  val playDefaultForkRunSupport = TaskKey[PlayForkSupportResult]("play-default-fork-run-support")

  @deprecated(message = "Use PlayKeys.playMonitoredFiles instead", since = "2.3.2")
  val playMonitoredFiles = PlayImport.PlayKeys.playMonitoredFiles
}
