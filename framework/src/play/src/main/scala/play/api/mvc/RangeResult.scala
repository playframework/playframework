/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

import akka.util.ByteString
import play.api.http.{ ContentTypes, HttpEntity, Status, HeaderNames }
import play.api.libs.concurrent.Execution
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Traversable
import play.api.libs.streams.Streams

/** Utility to help send content using range headers. */
object RangeResult {

  /** The HTTP Range header pattern */
  private lazy val RangeHeader = """^bytes=(\d*)-?(\d*)$""".r

  /**
   * Stream path using range headers.
   *
   * @param path The path.
   * @param rangeHeader The HTTP Range header from user's request.
   * @param contentType The HTTP Content Type header for the response.
   */
  def ofPath(path: java.nio.file.Path, rangeHeader: Option[String], contentType: Option[String]): Result = {
    ofPath(path, rangeHeader, path.getFileName.toString, contentType)
  }

  /**
   * Stream path using range headers.
   *
   * @param path The path.
   * @param rangeHeader The HTTP Range header from user's request.
   * @param fileName The file name for the HTTP Content-Disposition header as attachment attribute.
   * @param contentType The HTTP Content Type header for the response.
   */
  def ofPath(path: java.nio.file.Path, rangeHeader: Option[String], fileName: String, contentType: Option[String]): Result = {
    val stream = java.nio.file.Files.newInputStream(path)
    ofStream(stream, rangeHeader, Option(fileName), contentType)
  }

  /**
   * Stream file using range headers.
   *
   * @param file The file.
   * @param rangeHeader The HTTP Range header from user's request.
   * @param contentType The HTTP Content Type header for the response.
   */
  def ofFile(file: java.io.File, rangeHeader: Option[String], contentType: Option[String]): Result = {
    ofFile(file, rangeHeader, file.getName, contentType)
  }

  /**
   * Stream file using range headers.
   *
   * @param file The file.
   * @param rangeHeader The HTTP Range header from user's request.
   * @param fileName The file name for the HTTP Content-Disposition header as attachment attribute.
   * @param contentType The HTTP Content Type header for the response.
   */
  def ofFile(file: java.io.File, rangeHeader: Option[String], fileName: String, contentType: Option[String]): Result = {
    val stream = new java.io.FileInputStream(file)
    ofStream(stream, rangeHeader, Option(fileName), contentType)
  }

  /**
   * Stream content using range headers.
   *
   * @param stream The stream content.
   * @param rangeHeader The HTTP Range header from user's request.
   * @param contentType The HTTP Content Type header for the response.
   */
  def ofStream(stream: java.io.InputStream, rangeHeader: Option[String], contentType: Option[String]): Result = {
    ofStream(stream, rangeHeader, None, contentType)
  }

  /**
   * Stream content using range headers.
   *
   * @param stream The stream content.
   * @param rangeHeader The HTTP Range header from user's request.
   * @param fileName The file name for the HTTP Content-Disposition header as attachment attribute.
   * @param contentType The HTTP Content Type header for the response.
   */
  def ofStream(stream: java.io.InputStream, rangeHeader: Option[String], fileName: Option[String], contentType: Option[String]): Result = {

    val (rangedStatus: Int, rangedHeaders: Map[String, String], range: Option[(Long, Long)]) = rangeHeader match {
      case Some(RangeHeader(f, l)) =>
        // serving a partial content request
        val firstBytePos = f.toLong
        val lastBytePos = if (l != "") l.toLong else stream.available - 1
        val headers = Map(
          HeaderNames.CONTENT_RANGE -> s"bytes $firstBytePos-$lastBytePos/${stream.available}",
          HeaderNames.CONTENT_LENGTH -> s"${lastBytePos - firstBytePos + 1}"
        )
        (Status.PARTIAL_CONTENT, headers, Option(firstBytePos, lastBytePos))
      case _ =>
        // serving a non-partial content request
        val headers: Map[String, String] = Map(
          HeaderNames.ACCEPT_RANGES -> "bytes",
          HeaderNames.CONTENT_LENGTH -> s"${stream.available}"
        )
        (Status.OK, headers, None)
    }

    val allHeaders = rangedHeaders ++ Seq(
      Some(HeaderNames.CONTENT_TYPE -> contentType.getOrElse(ContentTypes.BINARY)),
      fileName.map(f => HeaderNames.CONTENT_DISPOSITION -> ("attachment; filename=\"" + f + "\""))
    ).flatten.toMap

    // enumerate the content
    val (enumerator, length) = range match {
      case Some((firstBytePos, lastBytePos)) =>
        (enumerate(stream, firstBytePos, lastBytePos), lastBytePos - firstBytePos)
      case _ =>
        (Enumerator.fromStream(stream)(Execution.defaultContext), stream.available().toLong)
    }
    val data = akka.stream.scaladsl.Source.fromPublisher(Streams.enumeratorToPublisher(enumerator)).map(ByteString.apply)

    Result(
      ResponseHeader(rangedStatus, allHeaders),
      HttpEntity.Streamed(data, Option(length), contentType)
    )
  }

  private def enumerate(is: java.io.InputStream, firstBytePos: Long, lastBytePos: Long): Enumerator[Array[Byte]] = {
    is.skip(firstBytePos)
    Enumerator.fromStream(is)(Execution.defaultContext) &> Traversable.takeUpTo(lastBytePos - firstBytePos + 1)
  }

}
