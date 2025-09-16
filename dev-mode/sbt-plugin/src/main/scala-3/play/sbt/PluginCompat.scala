/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import java.io.File
import java.nio.file.Path as NioPath

import sbt.{ ScopedKey, Task, Project, State, Result, TaskKey }
import sbt.Def.Classpath
import sbt.Defaults.files
import sbt.ProjectExtra.extract
import sbt.ProjectExtra.projectToLocalProject

import scala.language.implicitConversions

import play.sbt.routes.RoutesKeys.LazyProjectReference
import xsbti.{ FileConverter, HashedVirtualFileRef, VirtualFileRef }

object PluginCompat:
  type MainClass    = sbt.PackageOption.MainClass
  type FileRef      = xsbti.HashedVirtualFileRef

  inline def toFileRef(file: File)(using conv: FileConverter): FileRef = conv.toVirtualFile(file.toPath)
  inline def toFileRef(path: NioPath)(using conv: FileConverter): FileRef = conv.toVirtualFile(path)
  def toFileRefs(files: Seq[File])(using conv: FileConverter): Seq[FileRef] = files.map(toFileRef)
  inline def fileName(file: FileRef): String = file.name
  inline def toNioPath(hvf: VirtualFileRef)(using conv: FileConverter): NioPath = conv.toPath(hvf)
  def getFiles(c: Classpath)(implicit conv: FileConverter): Seq[File] = c.files.map(_.toFile)
  def createLazyProjectRef(p: Project): LazyProjectReference = new LazyProjectReference(p)
end PluginCompat
