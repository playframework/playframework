/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play

import sbt._
import sbt.Keys._

trait PlayInternalKeys {
  type ClassLoaderCreator = (String, Array[URL], ClassLoader) => ClassLoader

  val playDependencyClasspath = TaskKey[Classpath]("play-dependency-classpath")
  val playReloaderClasspath = TaskKey[Classpath]("play-reloader-classpath")
  val playCommonClassloader = TaskKey[ClassLoader]("play-common-classloader")
  val playDependencyClassLoader = TaskKey[ClassLoaderCreator]("play-dependency-classloader")
  val playReloaderClassLoader = TaskKey[ClassLoaderCreator]("play-reloader-classloader")
  val playReload = TaskKey[sbt.inc.Analysis]("play-reload")
  val buildRequire = TaskKey[Seq[(File, File)]]("play-build-require-assets")
  val playCompileEverything = TaskKey[Seq[sbt.inc.Analysis]]("play-compile-everything")
  val playAssetsWithCompilation = TaskKey[sbt.inc.Analysis]("play-assets-with-compilation")
}
