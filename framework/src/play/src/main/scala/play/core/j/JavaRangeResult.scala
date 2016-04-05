/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.j

import java.io.{ InputStream, File }
import java.nio.file.Path

import java.util.Optional
import play.mvc.Result

import scala.compat.java8.OptionConverters._

import akka.stream.javadsl.Source
import akka.util.ByteString
import play.api.mvc.RangeResult

/**
 * Java compatible RangeResult
 */
object JavaRangeResult {

  private type OptString = Optional[String]

  def ofPath(path: Path, rangeHeader: OptString, contentType: OptString): Result = {
    RangeResult.ofPath(path, rangeHeader.asScala, contentType.asScala).asJava
  }

  def ofPath(path: Path, rangeHeader: OptString, fileName: String, contentType: OptString): Result = {
    RangeResult.ofPath(path, rangeHeader.asScala, fileName, contentType.asScala).asJava
  }

  def ofFile(file: File, rangeHeader: OptString, contentType: OptString): Result = {
    RangeResult.ofFile(file, rangeHeader.asScala, contentType.asScala).asJava
  }

  def ofFile(file: File, rangeHeader: OptString, fileName: String, contentType: OptString): Result = {
    RangeResult.ofFile(file, rangeHeader.asScala, fileName, contentType.asScala).asJava
  }

  def ofSource(entityLength: Long, source: Source[ByteString, _], rangeHeader: OptString, fileName: OptString, contentType: OptString): Result = {
    RangeResult.ofSource(entityLength, source.asScala, rangeHeader.asScala, fileName.asScala, contentType.asScala).asJava
  }
}