/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.j

import java.io.{ InputStream, File }
import java.nio.file.Path

import play.api.mvc.{ RangeResult, Result }

/**
 * Java compatible RangeResult
 */
object JavaRangeResult {

  def ofPath(path: Path, rangeHeader: String): Result = RangeResult.ofPath(path, Option(rangeHeader), None)
  def ofPath(path: Path, rangeHeader: String, fileName: String): Result = RangeResult.ofPath(path, Option(rangeHeader), fileName, None)
  def ofPath(path: Path, rangeHeader: String, fileName: String, contentType: String): Result = RangeResult.ofPath(path, Option(rangeHeader), fileName, Option(contentType))
  def ofFile(file: File, rangeHeader: String): Result = RangeResult.ofFile(file, Option(rangeHeader), None)
  def ofFile(file: File, rangeHeader: String, fileName: String): Result = RangeResult.ofFile(file, Option(rangeHeader), fileName, None)
  def ofFile(file: File, rangeHeader: String, fileName: String, contentType: String): Result = RangeResult.ofFile(file, Option(rangeHeader), fileName, Option(contentType))
  def ofStream(stream: InputStream, rangeHeader: String): Result = RangeResult.ofStream(stream, Option(rangeHeader), None)
  def ofStream(stream: InputStream, rangeHeader: String, fileName: String): Result = RangeResult.ofStream(stream, Option(rangeHeader), Option(fileName), None)
  def ofStream(stream: InputStream, rangeHeader: String, fileName: String, contentType: String): Result = RangeResult.ofStream(stream, Option(rangeHeader), Option(fileName), Option(contentType))

}
