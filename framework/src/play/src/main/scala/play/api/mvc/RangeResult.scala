/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import akka.NotUsed
import akka.stream.scaladsl.{ Flow, Source, StreamConverters }
import akka.stream.stage._
import akka.util.ByteString
import play.api.http.HeaderNames._
import play.api.http.Status._
import play.api.http.{ ContentTypes, HttpEntity }

import scala.math.Ordered.orderingToOrdered

// Long should be good enough to represent even very large files
// considering that Long.MAX_VALUE is 9223372036854775807 which
// would be enough to represent Petabytes files. Also, consider
// that File.length() returns a long value.
private[mvc] case class ByteRange(start: Long, end: Long) extends Ordered[ByteRange] {

  override def compare(that: ByteRange): Int = {
    val startCompare = this.start - that.start
    if (startCompare != 0) startCompare.toInt
    else (this.end - that.end).toInt
  }

  def length: Long = this.end - this.start

  def distance(other: ByteRange): Long = {
    mergedEnd(other) - mergedStart(other) - (length + other.length)
  }

  private def mergedStart(other: ByteRange) = math.min(start, other.start)
  private def mergedEnd(other: ByteRange) = math.max(end, other.end)
}

private[mvc] case class Range(entityLength: Long, start: Option[Long], end: Option[Long]) extends Ordered[Range] {

  // Rules according to RFC 7233:
  // 1. If the last-byte-pos value is absent, or if the value is greater
  //    than or equal to the current length of the representation data,
  //    the byte range is interpreted as the remainder of the representation
  //    (i.e., the server replaces the value of last-byte-pos with a value
  //    that is one less than the current length of the selected representation)
  // 2. A client can request the last N bytes of the selected representation
  //    using a suffix-byte-range-spec. If the selected representation is shorter
  //    than the specified suffix-length, the entire representation is used.
  lazy val byteRange: ByteRange = {
    (start, end) match {
      case (Some(_start), Some(_end)) => ByteRange(_start, math.min(_end, entityLength - 1))
      case (Some(_start), None) => ByteRange(_start, entityLength - 1)
      case (None, Some(_end)) => ByteRange(math.max(0, entityLength - _end), entityLength - 1)
      case (None, None) => ByteRange(0, 0)
    }
  }

  // RFC 7233:
  // 1. A byte-range-spec is invalid if the last-byte-pos value is present
  //    and less than the first-byte-pos.
  // 2. For byte ranges, failing to overlap the current extent means that the
  //    first-byte-pos of all of the byte-range-spec values were greater than
  //    the current length of the selected representation.
  def isValid: Boolean = byteRange.start <= byteRange.end && byteRange.start < entityLength

  def length = byteRange.length + 1 // the range end is inclusive

  def merge(other: Range): Range = {
    val thisByteRange = this.byteRange
    val otherByteRange = other.byteRange
    Range(
      entityLength,
      Some(math.min(thisByteRange.start, otherByteRange.start)),
      Some(math.max(thisByteRange.end, otherByteRange.end))
    )
  }

  override def toString: String = s"${byteRange.start}-${byteRange.end}"

  override def compare(that: Range): Int = this.byteRange.compare(that.byteRange)
}

private[mvc] object Range {

  // Since the typical overhead between parts of a multipart/byteranges
  // payload is around 80 bytes, depending on the selected representation's
  // media type and the chosen boundary parameter length, it can be less
  // efficient to transfer many small disjoint parts than it is to transfer
  // the entire selected representation.
  val minimumDistance = 80 // TODO this should be configurable

  // A server that supports range requests MAY ignore or reject a Range
  // header field that consists of [...] a set of many small ranges that
  // are not listed in ascending order, since both are indications of either
  // a broken client or a deliberate denial-of-service attack (Section 6.1).
  // http://tools.ietf.org/html/rfc7233#section-6.1
  val maxNumberOfRanges = 16 // TODO this should be configurable

  val RangePattern = """(\d*)-(\d*)""".r

  def apply(entityLength: Long, range: String): Option[Range] = range match {
    case RangePattern(first, last) =>
      val firstByte = asOptionLong(first)
      val lastByte = asOptionLong(last)

      if ((firstByte ++ lastByte).isEmpty) return None // unsatisfiable range

      Some(Range(entityLength, firstByte, lastByte))
    case _ => None // unsatisfiable range
  }

  private def asOptionLong(string: String) = if (string.isEmpty) None else Some(string.toLong)
}

private[mvc] trait RangeSet {

  def ranges: Seq[Option[Range]]

  def entityLength: Long

  // Rules according to RFC 7233:
  // 1. If a valid byte-range-set includes at least one byte-range-spec with
  //    a first-byte-pos that is less than the current length of the
  //    representation, or at least one suffix-byte-range-spec with a
  //    non-zero suffix-length, then the byte-range-set is satisfiable.
  //    Otherwise, the byte-range-set is unsatisfiable.
  // 2. A server that supports range requests MAY ignore or reject a Range
  //    header field that consists of more than two overlapping ranges, or a
  //    set of many small ranges that are not listed in ascending order,
  //    since both are indications of either a broken client or a deliberate
  //    denial-of-service attack.
  // 3. When multiple ranges are requested, a server MAY coalesce any of the
  //    ranges that overlap, or that are separated by a gap that is smaller
  //    than the overhead of sending multiple parts, regardless of the order
  //    in which the corresponding byte-range-spec appeared in the received
  //    Range header field
  // 4. When a multipart response payload is generated, the server SHOULD
  //    send the parts in the same order that the corresponding
  //    byte-range-spec appeared in the received Range header field,
  //    excluding those ranges that were deemed unsatisfiable or that were
  //    coalesced into other ranges.
  def normalize: RangeSet = {
    if (isValid) {
      flattenRanges.sorted match {
        case seq if seq.isEmpty => UnsatisfiableRangeSet(entityLength)
        case seq => SatisfiableRangeSet(entityLength, ranges = coalesce(seq.toList).map(Option.apply))
      }
    } else {
      UnsatisfiableRangeSet(entityLength)
    }
  }

  private def coalesce(rangeSeq: List[Range]): List[Range] = {
    rangeSeq.foldLeft(List.empty[Range]) { (coalesced, current) =>
      val (mergeCandidates, otherCandidates) = coalesced.partition(_.byteRange.distance(current.byteRange) <= Range.minimumDistance)
      val merged = mergeCandidates.foldLeft(current)(_ merge _)
      otherCandidates :+ merged
    }
  }

  def first = ranges.head match {
    case Some(r) => r
    case None => Range(entityLength = entityLength, start = Some(0), end = Some(entityLength))
  }

  private def isValid: Boolean = !flattenRanges.exists(!_.isValid)

  private def flattenRanges = ranges.filter(_.isDefined).flatten

  override def toString = s"bytes ${flattenRanges.mkString(",")}/$entityLength"
}

private[mvc] abstract class DefaultRangeSet(entityLength: Long) extends RangeSet {
  override def ranges: Seq[Option[Range]] = Seq.empty
}

private[mvc] case class SatisfiableRangeSet(entityLength: Long, override val ranges: Seq[Option[Range]]) extends DefaultRangeSet(entityLength)

private[mvc] case class UnsatisfiableRangeSet(entityLength: Long) extends DefaultRangeSet(entityLength)

private[mvc] case class NoHeaderRangeSet(entityLength: Long) extends DefaultRangeSet(entityLength)

private[mvc] object RangeSet {

  // Play accepts only bytes as the range unit. According to RFC 7233:
  //
  //     An origin server MUST ignore a Range header field that contains a
  //     range unit it does not understand.  A proxy MAY discard a Range
  //     header field that contains a range unit it does not understand.
  val RangeSetPattern = """^bytes=[0-9,-]+""".r

  def apply(entityLength: Long, rangeHeader: Option[String]): RangeSet = rangeHeader match {
    case Some(header) => rangeSet(entityLength, header).normalize
    case None => NoHeaderRangeSet(entityLength)
  }

  private def rangeSet(entityLength: Long, rangeHeader: String): RangeSet = rangeHeader match {
    case RangeSetPattern() =>
      val ranges = rangeHeader.split("=")(1).split(",").map { r => Range(entityLength, r) }
      SatisfiableRangeSet(entityLength, ranges)
    case _ => NoHeaderRangeSet(entityLength)
  }
}

object RangeResult {

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
    // TODO use FileIO.fromFile after https://github.com/akka/akka/issues/20031 was released
    val source = StreamConverters.fromInputStream(() => java.nio.file.Files.newInputStream(path))
    ofSource(path.toFile.length(), source, rangeHeader, Option(fileName), contentType)
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
    // TODO use FileIO.fromFile after https://github.com/akka/akka/issues/20031 was released
    val source = StreamConverters.fromInputStream(() => new java.io.FileInputStream(file))
    ofSource(file.length(), source, rangeHeader, Option(fileName), contentType)
  }

  def ofSource(entityLength: Long, source: Source[ByteString, _], rangeHeader: Option[String], fileName: Option[String], contentType: Option[String]): Result = {
    val commonHeaders = Seq(
      Some(ACCEPT_RANGES -> "bytes"),
      Some(CONTENT_TYPE -> contentType.getOrElse(ContentTypes.BINARY)),
      fileName.map(f => CONTENT_DISPOSITION -> s"""attachment; filename="$f"""")
    ).flatten.toMap

    RangeSet(entityLength, rangeHeader) match {
      case rangeSet: SatisfiableRangeSet =>
        val firstRange = rangeSet.first
        val byteRange = firstRange.byteRange

        val entitySource = source.via(sliceBytesTransformer(byteRange.start, firstRange.length))

        Result(
          ResponseHeader(
            status = PARTIAL_CONTENT,
            headers = Map(CONTENT_RANGE -> rangeSet.toString) ++ commonHeaders
          ),
          HttpEntity.Streamed(
            entitySource,
            Some(firstRange.length),
            contentType.orElse(Some(ContentTypes.BINARY))
          )
        )
      case rangeSet: UnsatisfiableRangeSet =>
        Result(
          ResponseHeader(
            status = REQUESTED_RANGE_NOT_SATISFIABLE,
            headers = Map(CONTENT_RANGE -> s"bytes */$entityLength") ++ commonHeaders
          ),
          HttpEntity.Strict(
            data = ByteString.empty,
            contentType
          )
        )
      case rangeSet: NoHeaderRangeSet =>
        if (entityLength > 0) {
          Result(
            ResponseHeader(
              status = OK,
              headers = commonHeaders
            ),
            HttpEntity.Streamed(
              source,
              Some(entityLength),
              contentType.orElse(Some(ContentTypes.BINARY))
            )
          )
        } else {
          Results.Ok.sendEntity(HttpEntity.Strict(ByteString.empty, contentType))
        }
    }
  }

  // See https://github.com/akka/akka/blob/v2.4.2/akka-http-core/src/main/scala/akka/http/impl/util/StreamUtils.scala#L83-L115
  private def sliceBytesTransformer(start: Long, length: Long): Flow[ByteString, ByteString, NotUsed] = {
    val transformer = new StatefulStage[ByteString, ByteString] {

      def skipping = new State {
        var toSkip = start

        override def onPush(element: ByteString, ctx: Context[ByteString]): SyncDirective =
          if (element.length < toSkip) {
            // keep skipping
            toSkip -= element.length
            ctx.pull()
          } else {
            become(taking(length))
            // toSkip <= element.length <= Int.MaxValue
            current.onPush(element.drop(toSkip.toInt), ctx)
          }
      }

      def taking(initiallyRemaining: Long) = new State {
        var remaining: Long = initiallyRemaining

        override def onPush(element: ByteString, ctx: Context[ByteString]): SyncDirective = {
          val data = element.take(math.min(remaining, Int.MaxValue).toInt)
          remaining -= data.size
          if (remaining <= 0) ctx.pushAndFinish(data)
          else ctx.push(data)
        }
      }

      override def initial: State = if (start > 0) skipping else taking(length)
    }
    Flow[ByteString].transform(() ⇒ transformer).named("sliceBytes")
  }
}
