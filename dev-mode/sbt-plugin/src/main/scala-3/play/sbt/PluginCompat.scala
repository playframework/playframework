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

  private def execValue[T](t: T) = sbt.Result.Value(t)

  /**
   * Shim for runTask. Project.runTask is removed in sbt 2.0.
   *
   * This will be replaced when Extracted.runTask with the same signature
   * is supported in sbt 2.0.
   */
  def runTask[T](taskKey: TaskKey[T], state: State): Option[(State, Result[T])] =
    Some(
      Project.extract(state).runTask(taskKey, state) match {
        case (state, t) => (state, execValue(t))
      }
    )
  inline def toFileRef(file: File)(using conv: FileConverter): FileRef          = conv.toVirtualFile(file.toPath)
  inline def toFileRef(path: NioPath)(using conv: FileConverter): FileRef       = conv.toVirtualFile(path)
  def toFileRefs(files: Seq[File])(using conv: FileConverter): Seq[FileRef]     = files.map(toFileRef)
  inline def fileName(file: FileRef): String                                    = file.name
  inline def toNioPath(hvf: VirtualFileRef)(using conv: FileConverter): NioPath = conv.toPath(hvf)
  inline def getFiles(c: Classpath)(implicit conv: FileConverter): Seq[File]    = c.files.map(_.toFile)
  inline def createLazyProjectRef(p: Project): LazyProjectReference             = new LazyProjectReference(p)
  def getAttributeMap(t: Task[?]): AttributeMap                                 = t.attributes
  inline def toKey(settingKey: SettingKey[String]): StringAttributeKey          = StringAttributeKey(settingKey.key.label)
  def toFinder(s: Seq[FileRef])(using conv: FileConverter): PathFinderRef       = PathFinder(s.map(toNioPath(_).toFile))
end PluginCompat
