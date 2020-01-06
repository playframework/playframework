/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
import sbt._

object Common {
  def allFiles(file: File): Seq[File] = file.allPaths.filter(_.isFile).get
}
