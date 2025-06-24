/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.parsers

import java.net.URLDecoder

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.Future

import org.apache.pekko.stream.scaladsl._
import org.apache.pekko.stream.stage._
import org.apache.pekko.stream.ActorAttributes
import org.apache.pekko.stream.Attributes
import org.apache.pekko.stream.FlowShape
import org.apache.pekko.stream.IOResult
import org.apache.pekko.stream.Inlet
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.Outlet
import org.apache.pekko.stream.Supervision
import org.apache.pekko.util.ByteString
import play.api.http.HttpErrorHandler
import play.api.http.Status._
import play.api.libs.streams.Accumulator
import play.api.libs.Files.TemporaryFile
import play.api.libs.Files.TemporaryFileCreator
import play.api.mvc._
import play.api.mvc.MultipartFormData._
import play.core.Execution.Implicits.trampoline

/**
 * Utilities for handling multipart bodies
 */
object Multipart {
  private final val maxHeaderBuffer = 4096
  private val KeyValue              = """^([a-zA-Z_0-9]+)="?(.*?)"?$""".r
  private val ExtendedKeyValue      = """^([a-zA-Z_0-9]+)\*=(.*?)'.*'(.*?)$""".r

  /**
   * Parses the stream into a stream of [[play.api.mvc.MultipartFormData.Part]] to be handled by `partHandler`.
   *
   * @param maxMemoryBufferSize The maximum amount of data to parse into memory.
   * @param errorHandler The error handler to call when an error occurs.
   * @param partHandler The accumulator to handle the parts.
   * @deprecated Since 2.9.0. Use the overloaded partParser method that takes the allowEmptyFiles flag.
   */
  @deprecated("Use the overloaded partParser method that takes the allowEmptyFiles flag", "2.9.0")
  def partParser[A](maxMemoryBufferSize: Long, errorHandler: HttpErrorHandler)(
      partHandler: Accumulator[Part[Source[ByteString, _]], Either[Result, A]]
  )(implicit mat: Materializer): BodyParser[A] = partParser(maxMemoryBufferSize, false, errorHandler)(partHandler)

  /**
   * Parses the stream into a stream of [[play.api.mvc.MultipartFormData.Part]] to be handled by `partHandler`.
   *
   * @param maxMemoryBufferSize The maximum amount of data to parse into memory.
   * @param allowEmptyFiles If file uploads are allowed to contain no data in the body
   * @param errorHandler The error handler to call when an error occurs.
   * @param partHandler The accumulator to handle the parts.
   */
  def partParser[A](maxMemoryBufferSize: Long, allowEmptyFiles: Boolean, errorHandler: HttpErrorHandler)(
      partHandler: Accumulator[Part[Source[ByteString, _]], Either[Result, A]]
  )(implicit mat: Materializer): BodyParser[A] = BodyParser { request =>
    val maybeBoundary = for {
      mt         <- request.mediaType
      (_, value) <- mt.parameters.find(_._1.equalsIgnoreCase("boundary"))
      boundary   <- value
    } yield boundary

    maybeBoundary
      .map { boundary =>
        val multipartFlow = Flow[ByteString]
          .via(new BodyPartParser(boundary, maxMemoryBufferSize, maxHeaderBuffer, allowEmptyFiles))
          .splitWhen(_.isLeft)
          .prefixAndTail(1)
          .map {
            case (Seq(Left(part: FilePart[_])), body) =>
              part.copy[Source[ByteString, _]](
                ref = body.collect {
                  case Right(bytes) => bytes
                },
                refToBytes =
                  byteSource => Some(Await.result(byteSource.runFold(ByteString.empty)(_ ++ _), Duration.Inf))
              )
            case (Seq(Left(other)), ignored) =>
              // If we don't run the source, it takes Pekko streams 5 seconds to wake up and realise the source is empty
              // before it progresses onto the next element
              ignored.runWith(Sink.cancelled)
              other.asInstanceOf[Part[Nothing]]
          }
          .concatSubstreams
          .withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))

        partHandler.through(multipartFlow)
      }
      .getOrElse {
        Accumulator.done(createBadResult(msg = "Missing boundary header", errorHandler = errorHandler)(request))
      }
  }

  /**
   * Parses the request body into a Multipart body.
   *
   * @param maxMemoryBufferSize The maximum amount of data to parse into memory.
   * @param filePartHandler The accumulator to handle the file parts.
   * @param errorHandler The error handler to call when an error occurs.
   * @deprecated Since 2.9.0. Use the overloaded multipartParser method that takes the allowEmptyFiles flag.
   */
  @deprecated("Use the overloaded multipartParser method that takes the allowEmptyFiles flag", "2.9.0")
  def multipartParser[A](
      maxMemoryBufferSize: Long,
      filePartHandler: FilePartHandler[A],
      errorHandler: HttpErrorHandler
  )(implicit mat: Materializer): BodyParser[MultipartFormData[A]] =
    multipartParser(maxMemoryBufferSize, false, filePartHandler, errorHandler)

  /**
   * Parses the request body into a Multipart body.
   *
   * @param maxMemoryBufferSize The maximum amount of data to parse into memory.
   * @param allowEmptyFiles If empty file uploads are allowed (no matter if filename or file is empty)
   * @param filePartHandler The accumulator to handle the file parts.
   * @param errorHandler The error handler to call when an error occurs.
   */
  def multipartParser[A](
      maxMemoryBufferSize: Long,
      allowEmptyFiles: Boolean,
      filePartHandler: FilePartHandler[A],
      errorHandler: HttpErrorHandler
  )(implicit mat: Materializer): BodyParser[MultipartFormData[A]] = BodyParser { request =>
    partParser(maxMemoryBufferSize, allowEmptyFiles, errorHandler) {
      val handleFileParts = Flow[Part[Source[ByteString, _]]].mapAsync(1) {
        case filePart: FilePart[Source[ByteString, _]] =>
          filePartHandler(FileInfo(filePart.key, filePart.filename, filePart.contentType, filePart.dispositionType))
            .run(filePart.ref)
        case other: Part[_] => Future.successful(other.asInstanceOf[Part[Nothing]])
      }

      val multipartAccumulator = Accumulator(Sink.fold[Seq[Part[A]], Part[A]](Vector.empty)(_ :+ _)).mapFuture {
        parts =>
          def parseError = parts.collectFirst {
            case ParseError(msg) => createBadResult(msg, errorHandler = errorHandler)(request)
          }

          def bufferExceededError = parts.collectFirst {
            case MaxMemoryBufferExceeded(msg) => createBadResult(msg, REQUEST_ENTITY_TOO_LARGE, errorHandler)(request)
          }

          parseError.orElse(bufferExceededError).getOrElse {
            Future.successful(
              Right(
                MultipartFormData(
                  parts
                    .collect {
                      case dp: DataPart => dp
                    }
                    .groupBy(_.key)
                    .map {
                      case (key, partValues) => key -> partValues.map(_.value)
                    },
                  parts.collect {
                    case fp: FilePart[A] => fp
                  },
                  parts.collect {
                    case bad: BadPart => bad
                  }
                )
              )
            )
          }
      }

      multipartAccumulator.through(handleFileParts)
    }.apply(request)
  }

  type FilePartHandler[A] = FileInfo => Accumulator[ByteString, FilePart[A]]

  def handleFilePartAsTemporaryFile(temporaryFileCreator: TemporaryFileCreator): FilePartHandler[TemporaryFile] = {
    case FileInfo(partName, filename, contentType, dispositionType) =>
      val tempFile = temporaryFileCreator.create("multipartBody", "asTemporaryFile")
      Accumulator(FileIO.toPath(tempFile.path)).mapFuture {
        // Can't use unapply in Scala 3 here as long as we use the .cross(CrossVersion.for3Use2_13) workaround for pekko-http
        // That's because Scala 3 changed the unapply signature/behaviour and here we try to call unapply of a Scala 2 artifacts
        // from a Scala 3 artifact, which results in:
        // [error] java.lang.NoSuchMethodError: 'org.apache.pekko.stream.IOResult org.apache.pekko.stream.IOResult$.unapply(org.apache.pekko.stream.IOResult)'
        case r: IOResult if r.status.isFailure => Future.failed(r.status.failed.get)
        case r: IOResult if r.status.isSuccess =>
          Future.successful(
            FilePart(
              partName,
              filename,
              contentType,
              tempFile,
              r.count,
              dispositionType,
              tf => Some(ByteString.fromArray(java.nio.file.Files.readAllBytes(tf.path)))
            )
          )
      }
  }

  case class FileInfo(
      /** Name of the part in HTTP request (e.g. field name) */
      partName: String,
      /** Name of the file */
      fileName: String,
      /** Type of content (e.g. "application/pdf"), or `None` if unspecified. */
      contentType: Option[String],
      /** Disposition type in HTTP request (e.g. `form-data` or `file`) */
      dispositionType: String = "form-data"
  )

  private[play] object FileInfoMatcher {
    private def split(str: String): List[String] = {
      var buffer          = new java.lang.StringBuilder
      var escape: Boolean = false
      var quote: Boolean  = false
      val result          = new ListBuffer[String]

      def addPart() = {
        result += buffer.toString.trim
        buffer = new java.lang.StringBuilder
      }

      str.foreach {
        case '\\' =>
          buffer.append('\\')
          escape = true
        case '"' =>
          buffer.append('"')
          if (!escape)
            quote = !quote
          escape = false
        case ';' =>
          if (!quote) {
            addPart()
          } else {
            buffer.append(';')
          }
          escape = false
        case c =>
          buffer.append(c)
          escape = false
      }

      addPart()
      result.toList
    }

    def unapply(headers: Map[String, String]): Option[(String, String, Option[String], String)] = {
      for {
        values <-
          headers
            .get("content-disposition")
            .map(
              split(_).iterator
                .map(_.trim)
                .map {
                  // unescape escaped quotes
                  case KeyValue(key, v) =>
                    (key, v.trim.replaceAll("""\\"""", "\""))
                  case ExtendedKeyValue(key, encoding, value) =>
                    (key, URLDecoder.decode(value, encoding))
                  case key => (key.trim, "")
                }
                .toMap
            )

        dispositionType <- values.keys.find(key => key == "form-data" || key == "file")
        partName        <- values.get("name")
        fileName        <- values.get("filename")
        contentType = headers.get("content-type")
      } yield (partName, fileName, contentType, dispositionType)
    }
  }

  private[play] object PartInfoMatcher {
    def unapply(headers: Map[String, String]): Option[String] = {
      for {
        values <-
          headers
            .get("content-disposition")
            .map(
              _.split(";").iterator
                .map(_.trim)
                .map {
                  case KeyValue(key, v)                       => (key, v)
                  case ExtendedKeyValue(key, encoding, value) =>
                    (key, URLDecoder.decode(value, encoding))
                  case key => (key.trim, "")
                }
                .toMap
            )
        _        <- values.get("form-data")
        _        <- Option(values.contains("filename")).filter(_ == false)
        partName <- values.get("name")
      } yield partName
    }
  }

  private def createBadResult[A](
      msg: String,
      status: Int = BAD_REQUEST,
      errorHandler: HttpErrorHandler
  ): RequestHeader => Future[Either[Result, A]] = { request =>
    errorHandler.onClientError(request, status, msg).map(Left(_))
  }

  private type RawPart = Either[Part[Unit], ByteString]

  private def byteChar(input: ByteString, ix: Int): Char = byteAt(input, ix).toChar

  private def byteAt(input: ByteString, ix: Int): Byte =
    if (ix < input.length) input(ix) else throw NotEnoughDataException

  private object NotEnoughDataException extends RuntimeException(null, null, false, false)

  private val crlfcrlf: ByteString = {
    ByteString("\r\n\r\n")
  }

  /**
   * Copied and then heavily modified to suit Play's needs from Pekko HTTP pekko.http.impl.engine.BodyPartParser.
   *
   * INTERNAL API
   *
   * see: http://tools.ietf.org/html/rfc2046#section-5.1.1
   */
  private final class BodyPartParser(
      boundary: String,
      maxMemoryBufferSize: Long,
      maxHeaderSize: Int,
      allowEmptyFiles: Boolean
  ) extends GraphStage[FlowShape[ByteString, RawPart]] {

    @deprecated("Use the main constructor", "2.9.0")
    def this(boundary: String, maxMemoryBufferSize: Long, maxHeaderSize: Int) = {
      this(boundary, maxMemoryBufferSize, maxHeaderSize, false)
    }

    require(boundary.nonEmpty, "'boundary' parameter of multipart Content-Type must be non-empty")
    require(
      boundary.charAt(boundary.length - 1) != ' ',
      "'boundary' parameter of multipart Content-Type must not end with a space char"
    )

    sealed trait StateResult
    sealed trait Done                            extends StateResult
    case object Done                             extends Done
    class ContinueParsing(parse: => StateResult) extends StateResult {
      def apply(): StateResult = parse
    }
    object ContinueParsing {
      def apply(parse: => StateResult): ContinueParsing = new ContinueParsing(parse)
    }

    private[this] val needle: Array[Byte] = {
      val array = new Array[Byte](boundary.length + 4)
      array(0) = '\r'.toByte
      array(1) = '\n'.toByte
      array(2) = '-'.toByte
      array(3) = '-'.toByte
      System.arraycopy(boundary.getBytes("US-ASCII"), 0, array, 4, boundary.length)
      array
    }

    // we use the Boyer-Moore string search algorithm for finding the boundaries in the multipart entity,
    // see: http://www.cgjennings.ca/fjs/ and http://ijes.info/4/1/42544103.pdf
    private val boyerMoore = new BoyerMoore(needle)

    val in  = Inlet[ByteString]("BodyPartParser.in")
    val out = Outlet[RawPart]("BodyPartParser.out")

    override val shape = FlowShape.of(in, out)

    override def createLogic(attributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) with InHandler with OutHandler {
        private var output                    = collection.immutable.Queue.empty[RawPart]
        private var state: ByteString => Done = tryParseInitialBoundary
        private var terminated                = false

        override def onPush(): Unit = {
          if (!terminated) {
            state(grab(in))
            if (output.nonEmpty) push(out, dequeue())
            else if (!terminated) pull(in)
            else completeStage()
          } else completeStage()
        }

        override def onPull(): Unit = {
          if (output.nonEmpty)
            push(out, dequeue())
          else if (isClosed(in)) {
            if (!terminated) push(out, Left(ParseError("Unexpected end of input")))
            completeStage()
          } else pull(in)
        }

        override def onUpstreamFinish(): Unit = {
          if (isAvailable(out)) onPull()
        }

        setHandlers(in, out, this)

        @tailrec
        def handleParsingState(parse: ContinueParsing): Done = {
          parse() match {
            case Done               => Done
            case c: ContinueParsing => handleParsingState(c)
          }
        }

        def handleParsingState(exec: => StateResult): Done = handleParsingState(ContinueParsing(exec))

        def tryParseInitialBoundary(input: ByteString): Done = {
          // we don't use boyerMoore here because we are testing for the boundary *without* a
          // preceding CRLF and at a known location (the very beginning of the entity)
          try {
            if (boundary(input, 0)) {
              val ix = boundaryLength
              if (crlf(input, ix)) handleParsingState(parseHeader(input, ix + 2, 0))
              else if (doubleDash(input, ix)) terminate()
              else parsePreamble(input, 0)
            } else parsePreamble(input, 0)
          } catch {
            case NotEnoughDataException => continue(input, 0)((newInput, _) => tryParseInitialBoundary(newInput))
          }
        }

        def parsePreamble(input: ByteString, offset: Int): Done = {
          try {
            @tailrec def rec(index: Int): Done = {
              val needleEnd = boyerMoore.nextIndex(input, index) + needle.length
              if (crlf(input, needleEnd)) handleParsingState(parseHeader(input, needleEnd + 2, 0))
              else if (doubleDash(input, needleEnd)) terminate()
              else rec(needleEnd)
            }
            rec(offset)
          } catch {
            case NotEnoughDataException => continue(input.takeRight(needle.length + 2), 0)(parsePreamble)
          }
        }

        /**
         * Parsing the header is done by buffering up to 4096 bytes until CRLFCRLF is encountered.
         *
         * Then, the resulting ByteString is converted to a String, split into lines, and then split into keys and values.
         */
        def parseHeader(input: ByteString, headerStart: Int, memoryBufferSize: Int): StateResult = {
          input.indexOfSlice(crlfcrlf, headerStart) match {
            case -1 if input.length - headerStart >= maxHeaderSize =>
              bufferExceeded("Header length exceeded maximum header size of " + maxHeaderSize)
            case -1 =>
              continue(input, headerStart)(parseHeader(_, _, memoryBufferSize))
            case headerEnd if headerEnd - headerStart >= maxHeaderSize =>
              bufferExceeded("Header length exceeded maximum header size of " + maxHeaderSize)
            case headerEnd =>
              val headerString                 = input.slice(headerStart, headerEnd).utf8String
              val headers: Map[String, String] =
                headerString.linesIterator.map { header =>
                  val key :: value = header.trim.split(":").toList

                  (key.trim.toLowerCase(java.util.Locale.ENGLISH), value.mkString(":").trim)
                }.toMap

              val partStart = headerEnd + 4

              // The amount of memory taken by the headers
              def headersSize = headers.foldLeft(0)((total, value) => total + value._1.length + value._2.length)

              val totalMemoryBufferSize = memoryBufferSize + headersSize

              headers match {
                case FileInfoMatcher(partName, fileName, contentType, dispositionType) => {
                  def processFilePart(in: ByteString) = handleFilePart(
                    in,
                    partStart,
                    totalMemoryBufferSize,
                    partName,
                    fileName,
                    contentType,
                    dispositionType
                  )
                  if (allowEmptyFiles) {
                    processFilePart(input)
                  } else {
                    if (fileName.trim.nonEmpty) {
                      checkEmptyBody(input, partStart, totalMemoryBufferSize)(newInput => processFilePart(newInput))(
                        newInput => handleBadPart(newInput, partStart, totalMemoryBufferSize, headers)
                      )
                    } else {
                      handleBadPart(input, partStart, totalMemoryBufferSize, headers)
                    }
                  }
                }
                case PartInfoMatcher(name) =>
                  handleDataPart(input, partStart, memoryBufferSize + name.length, name)
                case _ =>
                  handleBadPart(input, partStart, totalMemoryBufferSize, headers)
              }
          }
        }

        def checkEmptyBody(input: ByteString, partStart: Int, memoryBufferSize: Int)(
            nonEmpty: (ByteString) => StateResult
        )(empty: (ByteString) => StateResult): StateResult = {
          try {
            val currentPartEnd = boyerMoore.nextIndex(input, partStart)
            if (currentPartEnd - partStart == 0) {
              empty(input)
            } else {
              nonEmpty(input)
            }
          } catch {
            case NotEnoughDataException => // "not enough data" here means not enough data to locate the needle. However we might not even need the needle...
              if (partStart <= input.length - needle.length) {
                // There was already enough space in the input to contain the needle, but it wasn't found in the try block above.
                // This means the needle will start at some position _after_ partStart and there will for sure be data between
                // partStart and the start of the needle -> the body is definitely not empty.
                // We don't need to get more data (and also not the needle) to make a decision.
                nonEmpty(input)
              } else {
                // There was not even enough space in the input to contain the needle. Only after we have enough data
                // of at least the size of the needle we can decide if the body is empty or not.
                state = more =>
                  handleParsingState(checkEmptyBody(input ++ more, partStart, memoryBufferSize)(nonEmpty)(empty))
                done()
              }
          }
        }

        def handleFilePart(
            input: ByteString,
            partStart: Int,
            memoryBufferSize: Int,
            partName: String,
            fileName: String,
            contentType: Option[String],
            dispositionType: String
        ): StateResult = {
          if (memoryBufferSize > maxMemoryBufferSize) {
            bufferExceeded(s"Memory buffer full ($maxMemoryBufferSize) on part $partName")
          } else {
            emit(FilePart(partName, fileName, contentType, (), -1, dispositionType, (_: Any) => None))
            handleFileData(input, partStart, memoryBufferSize)
          }
        }

        def handleFileData(input: ByteString, offset: Int, memoryBufferSize: Int): StateResult = {
          try {
            val currentPartEnd = boyerMoore.nextIndex(input, offset)
            val needleEnd      = currentPartEnd + needle.length
            if (crlf(input, needleEnd)) {
              emit(input.slice(offset, currentPartEnd))
              ContinueParsing(parseHeader(input, needleEnd + 2, memoryBufferSize))
            } else if (doubleDash(input, needleEnd)) {
              emit(input.slice(offset, currentPartEnd))
              terminate()
            } else {
              fail("Unexpected boundary")
            }
          } catch {
            case NotEnoughDataException =>
              // we cannot emit all input bytes since the end of the input might be the start of the next boundary
              val emitEnd = input.length - needle.length - 2
              if (emitEnd > offset) {
                emit(input.slice(offset, emitEnd))
                continue(input.drop(emitEnd), 0)(handleFileData(_, _, memoryBufferSize))
              } else {
                continue(input, offset)(handleFileData(_, _, memoryBufferSize))
              }
          }
        }

        def handleDataPart(input: ByteString, partStart: Int, memoryBufferSize: Int, partName: String): StateResult = {
          try {
            val currentPartEnd      = boyerMoore.nextIndex(input, partStart)
            val needleEnd           = currentPartEnd + needle.length
            val newMemoryBufferSize = memoryBufferSize + (currentPartEnd - partStart)
            if (newMemoryBufferSize > maxMemoryBufferSize) {
              bufferExceeded("Memory buffer full on part " + partName)
            } else if (crlf(input, needleEnd)) {
              emit(DataPart(partName, input.slice(partStart, currentPartEnd).utf8String))
              ContinueParsing(parseHeader(input, needleEnd + 2, newMemoryBufferSize))
            } else if (doubleDash(input, needleEnd)) {
              emit(DataPart(partName, input.slice(partStart, currentPartEnd).utf8String))
              terminate()
            } else {
              fail("Unexpected boundary")
            }
          } catch {
            case NotEnoughDataException =>
              if (memoryBufferSize + (input.length - partStart - needle.length) > maxMemoryBufferSize) {
                bufferExceeded("Memory buffer full on part " + partName)
              }
              continue(input, partStart)(handleDataPart(_, _, memoryBufferSize, partName))
          }
        }

        def handleBadPart(
            input: ByteString,
            partStart: Int,
            memoryBufferSize: Int,
            headers: Map[String, String]
        ): StateResult = {
          try {
            val currentPartEnd = boyerMoore.nextIndex(input, partStart)
            val needleEnd      = currentPartEnd + needle.length
            if (crlf(input, needleEnd)) {
              emit(BadPart(headers))
              ContinueParsing(parseHeader(input, needleEnd + 2, memoryBufferSize))
            } else if (doubleDash(input, needleEnd)) {
              emit(BadPart(headers))
              terminate()
            } else {
              fail("Unexpected boundary")
            }
          } catch {
            case NotEnoughDataException =>
              continue(input, partStart)(handleBadPart(_, _, memoryBufferSize, headers))
          }
        }

        def emit(bytes: ByteString): Unit = if (bytes.nonEmpty) {
          output = output.enqueue(Right(bytes))
        }

        def emit(part: Part[Unit]): Unit = {
          output = output.enqueue(Left(part))
        }

        def dequeue(): RawPart = {
          val head = output.head
          output = output.tail
          head
        }

        def continue(input: ByteString, offset: Int)(next: (ByteString, Int) => StateResult): Done = {
          state = math.signum(offset - input.length) match {
            case -1 => more => handleParsingState(next(input ++ more, offset))
            case 0  => more => handleParsingState(next(more, 0))
            case 1  => throw new IllegalStateException
          }
          done()
        }

        def bufferExceeded(message: String): Done = {
          emit(MaxMemoryBufferExceeded(message))
          terminate()
        }

        def fail(message: String): Done = {
          emit(ParseError(message))
          terminate()
        }

        def terminate(): Done = {
          terminated = true
          done()
        }

        def done(): Done = Done

        // the length of the needle without the preceding CRLF
        def boundaryLength: Int = needle.length - 2

        @tailrec def boundary(input: ByteString, offset: Int, ix: Int = 2): Boolean =
          (ix == needle.length) || (byteAt(input, offset + ix - 2) == needle(ix)) && boundary(input, offset, ix + 1)

        def crlf(input: ByteString, offset: Int): Boolean =
          byteChar(input, offset) == '\r' && byteChar(input, offset + 1) == '\n'

        def doubleDash(input: ByteString, offset: Int): Boolean =
          byteChar(input, offset) == '-' && byteChar(input, offset + 1) == '-'
      }
  }

  /**
   * Copied from Pekko HTTP.
   *
   * Straight-forward Boyer-Moore string search implementation.
   */
  private class BoyerMoore(needle: Array[Byte]) {
    require(needle.length > 0, "needle must be non-empty")

    private[this] val nl1 = needle.length - 1

    private[this] val charTable: Array[Int] = {
      val table                      = Array.fill(256)(needle.length)
      @tailrec def rec(i: Int): Unit =
        if (i < nl1) {
          table(needle(i) & 0xff) = nl1 - i
          rec(i + 1)
        }
      rec(0)
      table
    }

    private[this] val offsetTable: Array[Int] = {
      val table = new Array[Int](needle.length)

      @tailrec def isPrefix(i: Int, j: Int): Boolean =
        i == needle.length || needle(i) == needle(j) && isPrefix(i + 1, j + 1)
      @tailrec def loop1(i: Int, lastPrefixPosition: Int): Unit =
        if (i >= 0) {
          val nextLastPrefixPosition = if (isPrefix(i + 1, 0)) i + 1 else lastPrefixPosition
          table(nl1 - i) = nextLastPrefixPosition - i + nl1
          loop1(i - 1, nextLastPrefixPosition)
        }
      loop1(nl1, needle.length)

      @tailrec def suffixLength(i: Int, j: Int, result: Int): Int =
        if (i >= 0 && needle(i) == needle(j)) suffixLength(i - 1, j - 1, result + 1) else result
      @tailrec def loop2(i: Int): Unit =
        if (i < nl1) {
          val sl = suffixLength(i, nl1, 0)
          table(sl) = nl1 - i + sl
          loop2(i + 1)
        }
      loop2(0)
      table
    }

    /**
     * Returns the index of the next occurrence of `needle` in `haystack` that is >= `offset`.
     * If none is found a `NotEnoughDataException` is thrown.
     */
    def nextIndex(haystack: ByteString, offset: Int): Int = {
      @tailrec def rec(i: Int, j: Int): Int = {
        val byte = byteAt(haystack, i)
        if (needle(j) == byte) {
          if (j == 0) i // found
          else rec(i - 1, j - 1)
        } else rec(i + math.max(offsetTable(nl1 - j), charTable(byte & 0xff)), nl1)
      }
      rec(offset + nl1, nl1)
    }
  }
}
