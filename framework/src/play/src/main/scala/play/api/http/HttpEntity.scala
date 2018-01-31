/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.mvc.Headers
import play.http.{ HttpEntity => JHttpEntity }

import scala.compat.java8.OptionConverters
import scala.concurrent.Future

/**
 * An HTTP entity.
 *
 * HTTP entities come in three flavors, [[HttpEntity.Strict]], [[HttpEntity.Streamed]] and [[HttpEntity.Chunked]].
 */
sealed trait HttpEntity {

  /**
   * The content type of the entity, if known.
   */
  def contentType: Option[String]

  /**
   * Whether it is known if this entity is empty or not.
   *
   * If this returns true, then the entity is definitely empty. If it returns false, the entity may or may not be empty.
   */
  def isKnownEmpty: Boolean

  /**
   * The content length of the entity, if known.
   */
  def contentLength: Option[Long]

  /**
   * The entity as a data stream.
   */
  def dataStream: Source[ByteString, _]

  /**
   * Consume the data from this entity.
   */
  def consumeData(implicit mat: Materializer): Future[ByteString] = {
    dataStream.runFold(ByteString.empty)(_ ++ _)
  }

  /**
   * Convert this entity to its Java counterpart.
   */
  def asJava: JHttpEntity

  /**
   * Return this entity as the given content type.
   */
  def as(contentType: String): HttpEntity
}

object HttpEntity {

  /**
   * No entity.
   */
  val NoEntity = Strict(ByteString.empty, None)

  /**
   * A strict entity.
   *
   * Strict entities are contained entirely in memory.
   *
   * @param data The data contained within this entity.
   * @param contentType The content type, if known.
   */
  final case class Strict(data: ByteString, contentType: Option[String]) extends HttpEntity {
    def isKnownEmpty = data.isEmpty
    def contentLength = Some(data.size)
    def dataStream = if (data.isEmpty) Source.empty[ByteString] else Source.single(data)
    override def consumeData(implicit mat: Materializer) = Future.successful(data)
    def asJava = new JHttpEntity.Strict(data, OptionConverters.toJava(contentType))
    def as(contentType: String) = copy(contentType = Some(contentType))
  }

  /**
   * A streamed entity.
   *
   * @param data The stream of data for this entity.
   * @param contentLength The content length, if known. If no content length is set, then this entity will be close
   *                      delimited.
   * @param contentType The content type, if known.
   */
  final case class Streamed(data: Source[ByteString, _], contentLength: Option[Long], contentType: Option[String]) extends HttpEntity {
    def isKnownEmpty = false
    def dataStream = data
    def asJava = new JHttpEntity.Streamed(
      data.asJava,
      OptionConverters.toJava(contentLength.asInstanceOf[Option[java.lang.Long]]),
      OptionConverters.toJava(contentType))
    def as(contentType: String) = copy(contentType = Some(contentType))
  }

  /**
   * A chunked entity.
   *
   * @param chunks The stream of chunks for this entity. Must be zero or more [[HttpChunk.Chunk]] elements, followed
   *               by zero or one [[HttpChunk.LastChunk]] elements. Any elements after the [[HttpChunk.LastChunk]]
   *               element will be ignored. If no [[HttpChunk.LastChunk]] element is sent, then the last chunk will
   *               contain no trailers.
   * @param contentType The content type, if known.
   */
  final case class Chunked(chunks: Source[HttpChunk, _], contentType: Option[String]) extends HttpEntity {
    def isKnownEmpty = false
    def contentLength = None
    def dataStream = chunks.collect {
      case HttpChunk.Chunk(data) => data
    }
    def asJava = new JHttpEntity.Chunked(chunks.asJava, OptionConverters.toJava(contentType))
    def as(contentType: String) = copy(contentType = Some(contentType))
  }
}

/**
 * An HTTP chunk.
 *
 * May either be a [[HttpChunk.Chunk]] containing data, or a [[HttpChunk.LastChunk]], signifying the last chunk in
 * a stream, optionally with trailing headers.
 */
sealed trait HttpChunk {

}

object HttpChunk {

  /**
   * A chunk.
   *
   * @param data The data for the chunk.
   */
  final case class Chunk(data: ByteString) extends HttpChunk {
    assert(data.nonEmpty, "Http chunks must not be empty")
  }

  /**
   * The last chunk.
   *
   * @param trailers The trailers.
   */
  final case class LastChunk(trailers: Headers) extends HttpChunk
}
