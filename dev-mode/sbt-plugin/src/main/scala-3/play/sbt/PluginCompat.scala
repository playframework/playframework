/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import java.io.File
import java.nio.file.Path as NioPath

import scala.language.implicitConversions

import sbt.*
import sbt.Def.Classpath
import sbt.Defaults.files
import sbt.ProjectExtra.extract
import sbt.ProjectExtra.projectToLocalProject

import play.sbt.routes.RoutesKeys.LazyProjectReference
import xsbti.FileConverter
import xsbti.HashedVirtualFileRef
import xsbti.VirtualFileRef

object PluginCompat:
  export sbt.Def.uncached

  type MainClass     = sbt.PackageOption.MainClass
  type FileRef       = xsbti.HashedVirtualFileRef
  type PathFinderRef = sbt.io.PathFinder

  val Inc   = sbt.Result.Inc
  val Value = sbt.Result.Value
  /**
   * Shim for runTask. Project.runTask is removed in sbt 2.0.
   *
   * This will be replaced when Extracted.runTask with the same signature
   * is supported in sbt 2.0.
   */
  def runTask[T](taskKey: TaskKey[T], state: State): Option[(State, Result[T])] =
      Project.extract(state).runTaskUnhandled(taskKey, state)

  // File converter shims
  inline def toFileRef(file: File)(using fc: FileConverter): FileRef             = fc.toVirtualFile(file.toPath)
  inline def toFileRef(path: NioPath)(using fc: FileConverter): FileRef          = fc.toVirtualFile(path)
  inline def toNioPath(hvf: VirtualFileRef)(using fc: FileConverter): NioPath    = fc.toPath(hvf)
  inline def getFiles(c: Classpath)(implicit fc: FileConverter): Seq[File]       = c.files.map(_.toFile)
  def toFinder(s: Seq[FileRef])(using fc: FileConverter): PathFinderRef          = PathFinder(s.map(toNioPath(_).toFile))

  inline def fileName(file: FileRef): String                           = file.name
  inline def createLazyProjectRef(p: Project): LazyProjectReference    = new LazyProjectReference(p)
  inline def getAttributeMap(t: Task[?]): AttributeMap                 = t.attributes
  inline def toKey(settingKey: SettingKey[String]): StringAttributeKey = StringAttributeKey(settingKey.key.label)
end PluginCompat
