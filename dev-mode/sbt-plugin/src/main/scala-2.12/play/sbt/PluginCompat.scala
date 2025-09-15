/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import java.io.File
import java.nio.file.{ Path => NioPath }

import sbt.Def.Classpath
import sbt.Project
import sbt.Result
import sbt.State
import sbt.TaskKey
import sbt.Value

import xsbti.FileConverter

object PluginCompat {
  type MainClass = sbt.Package.MainClass
  type FileRef   = File

  def toFileRef(file: java.io.File)(implicit fileConverter: FileConverter): FileRef     = file
  def toFileRef(path: NioPath)(implicit fileConverter: FileConverter): FileRef          = path.toFile
  def toFileRefs(files: Seq[File])(implicit fileConverter: FileConverter): Seq[FileRef] = files.map(toFileRef)
  def fileName(file: FileRef): String                                                   = file.getName
  def toNioPath(f: File)(implicit conv: FileConverter): NioPath                         = f.toPath
  def getFiles(c: Classpath)(implicit conv: FileConverter): Seq[File]                   = c.files
}
