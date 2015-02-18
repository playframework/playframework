/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.docs

import java.io.InputStream

import play.doc.{ FileHandle, FileRepository }

/**
 * A file repository that aggregates multiple file repositories
 *
 * @param repos The repositories to aggregate
 */
class AggregateFileRepository(repos: Seq[FileRepository]) extends FileRepository {

  def this(repos: Array[FileRepository]) = this(repos.toSeq)

  private def fromFirstRepo[A](load: FileRepository => Option[A]) = repos.collectFirst(Function.unlift(load))

  def loadFile[A](path: String)(loader: (InputStream) => A) = fromFirstRepo(_.loadFile(path)(loader))

  def handleFile[A](path: String)(handler: (FileHandle) => A) = fromFirstRepo(_.handleFile(path)(handler))

  def findFileWithName(name: String) = fromFirstRepo(_.findFileWithName(name))
}
