/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import java.io.File
import java.nio.file.{ Path => NioPath }

import sbt.*
import sbt.Def.Classpath

import play.sbt.routes.RoutesKeys.LazyProjectReference
import xsbti.FileConverter

object PluginCompat {
  type MainClass     = sbt.Package.MainClass
  type FileRef       = File
  type PathFinderRef = Seq[File]

  def runTask[T](taskKey: TaskKey[T], state: State): Option[(State, Result[T])] =
    Project.runTask(taskKey, state)

  def toFileRef(file: java.io.File)(implicit fc: FileConverter): FileRef     = file
  def toFileRef(path: NioPath)(implicit fc: FileConverter): FileRef          = path.toFile
  def toFileRefs(files: Seq[File])(implicit fc: FileConverter): Seq[FileRef] = files.map(toFileRef)
  def fileName(file: FileRef): String                                        = file.getName
  def toNioPath(f: File)(implicit conv: FileConverter): NioPath              = f.toPath
  def getFiles(c: Classpath)(implicit conv: FileConverter): Seq[File]        = c.files
  def createLazyProjectRef(p: Project): LazyProjectReference                 = new LazyProjectReference(p)
  def getAttributeMap(t: Task[?]): AttributeMap                              = t.info.attributes
  def toKey(settingKey: SettingKey[String]): AttributeKey[String]            = settingKey.key
  def toFinder(s: Seq[FileRef])(implicit fc: FileConverter): PathFinderRef   = s
  def uncached[T](value: T): T                                               = value
}
