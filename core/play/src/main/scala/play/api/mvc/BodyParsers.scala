/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import java.io._
import java.nio.charset.StandardCharsets._
import java.nio.charset._
import java.nio.file.Files
import java.util.Locale

import javax.inject.Inject
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.StreamConverters
import akka.stream.stage._
import akka.util.ByteString
import play.api._
import play.api.data.Form
import play.api.http.Status._
import play.api.http._
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.Files.TemporaryFile
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.json._
import play.api.libs.streams.Accumulator
import play.api.mvc.MultipartFormData._
import play.core.Execution
import play.core.parsers.Multipart
import play.utils.PlayIO

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.Exception.catching
import scala.util.control.NonFatal
import scala.xml._

/**
 * A request body that adapts automatically according the request Content-Type.
 */
sealed trait AnyContent {
  /**
   * application/x-www-form-urlencoded
   */
  def asFormUrlEncoded: Option[Map[String, Seq[String]]] = this match {
    case AnyContentAsFormUrlEncoded(data) => Some(data)
    case _                                => None
  }

  /**
   * text/plain
   */
  def asText: Option[String] = this match {
    case AnyContentAsText(txt) => Some(txt)
    case _                     => None
  }

  /**
   * application/xml
   */
  def asXml: Option[NodeSeq] = this match {
    case AnyContentAsXml(xml) => Some(xml)
    case _                    => None
  }

  /**
   * text/json or application/json
   */
  def asJson: Option[JsValue] = this match {
    case AnyContentAsJson(json) => Some(json)
    case _                      => None
  }

  /**
   * multipart/form-data
   */
  def asMultipartFormData: Option[MultipartFormData[TemporaryFile]] = this match {
    case AnyContentAsMultipartFormData(mfd) => Some(mfd)
    case _                                  => None
  }

  /**
   * Used when no Content-Type matches
   */
  def asRaw: Option[RawBuffer] = this match {
    case AnyContentAsRaw(raw) => Some(raw)
    case _                    => None
  }
}

/**
 * Factory object for creating an AnyContent instance.  Useful for unit testing.
 */
object AnyContent {
  def apply(): AnyContent                                           = AnyContentAsEmpty
  def apply(contentText: String): AnyContent                        = AnyContentAsText(contentText)
  def apply(json: JsValue): AnyContent                              = AnyContentAsJson(json)
  def apply(xml: NodeSeq): AnyContent                               = AnyContentAsXml(xml)
  def apply(formUrlEncoded: Map[String, Seq[String]]): AnyContent   = AnyContentAsFormUrlEncoded(formUrlEncoded)
  def apply(formData: MultipartFormData[TemporaryFile]): AnyContent = AnyContentAsMultipartFormData(formData)
  def apply(raw: RawBuffer): AnyContent                             = AnyContentAsRaw(raw)
}

/**
 * AnyContent - Empty request body
 */
case object AnyContentAsEmpty extends AnyContent

/**
 * AnyContent - Text body
 */
case class AnyContentAsText(txt: String) extends AnyContent

/**
 * AnyContent - Form url encoded body
 */
case class AnyContentAsFormUrlEncoded(data: Map[String, Seq[String]]) extends AnyContent

/**
 * AnyContent - Raw body (give access to the raw data as bytes).
 */
case class AnyContentAsRaw(raw: RawBuffer) extends AnyContent

/**
 * AnyContent - XML body
 */
case class AnyContentAsXml(xml: NodeSeq) extends AnyContent

/**
 * AnyContent - Json body
 */
case class AnyContentAsJson(json: JsValue) extends AnyContent

/**
 * AnyContent - Multipart form data body
 */
case class AnyContentAsMultipartFormData(mfd: MultipartFormData[TemporaryFile]) extends AnyContent

/**
 * Multipart form data body.
 */
case class MultipartFormData[A](dataParts: Map[String, Seq[String]], files: Seq[FilePart[A]], badParts: Seq[BadPart]) {
  /**
   * Extract the data parts as Form url encoded.
   */
  def asFormUrlEncoded: Map[String, Seq[String]] = dataParts

  /**
   * Access a file part.
   */
  def file(key: String): Option[FilePart[A]] = files.find(_.key == key)
}

/**
 * Defines parts handled by Multipart form data.
 */
object MultipartFormData {
  /**
   * A part.
   *
   * @tparam A the type that file parts are exposed as.
   */
  sealed trait Part[+A]

  /**
   * A data part.
   */
  case class DataPart(key: String, value: String) extends Part[Nothing]

  /**
   * A file part.
   */
  case class FilePart[A](
      key: String,
      filename: String,
      contentType: Option[String],
      ref: A,
      fileSize: Long = -1,
      dispositionType: String = "form-data"
  ) extends Part[A]

  /**
   * A part that has not been properly parsed.
   */
  case class BadPart(headers: Map[String, String]) extends Part[Nothing]

  /**
   * Emitted when the multipart stream can't be parsed for some reason.
   */
  case class ParseError(message: String) extends Part[Nothing]

  /**
   * The multipart/form-data parser buffers many things in memory, including data parts, headers, file names etc.
   *
   * Some buffer limits apply to each element, eg, there is a buffer for headers before they are parsed.  Other buffer
   * limits apply to all in memory data in aggregate, this includes data parts, file names, part names.
   *
   * If any of these buffers are exceeded, this will be emitted.
   */
  case class MaxMemoryBufferExceeded(message: String) extends Part[Nothing]
}

/**
 * Handle the request body a raw bytes data.
 *
 * @param memoryThreshold If the content size is bigger than this limit, the content is stored as file.
 * @param temporaryFileCreator the temporary file creator to store the content as file.
 * @param initialData the initial data, ByteString.empty by default.
 */
case class RawBuffer(
    memoryThreshold: Long,
    temporaryFileCreator: TemporaryFileCreator,
    initialData: ByteString = ByteString.empty
) {
  import play.api.libs.Files._

  @volatile private var inMemory: ByteString                 = initialData
  @volatile private var backedByTemporaryFile: TemporaryFile = _
  @volatile private var outStream: OutputStream              = _

  private[play] def push(chunk: ByteString): Unit = {
    if (inMemory != null) {
      if (chunk.length + inMemory.size > memoryThreshold) {
        backToTemporaryFile()
        outStream.write(chunk.toArray)
      } else {
        inMemory = inMemory ++ chunk
      }
    } else {
      outStream.write(chunk.toArray)
    }
  }

  private[play] def close(): Unit = if (outStream != null) outStream.close()

  private[play] def backToTemporaryFile(): Unit = {
    backedByTemporaryFile = temporaryFileCreator.create("requestBody", "asRaw")
    outStream = Files.newOutputStream(backedByTemporaryFile)
    outStream.write(inMemory.toArray)
    inMemory = null
  }

  /**
   * Buffer size.
   */
  def size: Long = {
    if (inMemory != null) inMemory.size else Files.size(backedByTemporaryFile)
  }

  /**
   * Returns the buffer content as a bytes array.
   *
   * This operation will cause the internal collection of byte arrays to be copied into a new byte array on each
   * invocation, no caching is done.  If the buffer has been written out to a file, it will read the contents of the
   * file.
   *
   * @param maxLength The max length allowed to be stored in memory.  If this is smaller than memoryThreshold, and the
   *                  buffer is already in memory then None will still be returned.
   * @return None if the content is greater than maxLength, otherwise, the data as bytes.
   */
  def asBytes(maxLength: Long = memoryThreshold): Option[ByteString] = {
    if (size <= maxLength) {
      Some(if (inMemory != null) inMemory else ByteString(PlayIO.readFile(backedByTemporaryFile.path)))
    } else {
      None
    }
  }

  /**
   * Returns the buffer content as File.
   */
  def asFile: File = {
    if (inMemory != null) {
      backToTemporaryFile()
      close()
    }
    backedByTemporaryFile
  }

  override def toString = {
    val inMemorySize: Any = Option(this.inMemory).map(_.size).orNull
    s"RawBuffer(inMemory=$inMemorySize, backedByTemporaryFile=$backedByTemporaryFile)"
  }
}

/**
 * A set of reusable body parsers and utilities that do not require configuration.
 */
trait BodyParserUtils {
  /**
   * Don't parse the body content.
   */
  def empty: BodyParser[Unit] = ignore(())

  def ignore[A](body: A): BodyParser[A] = BodyParser("ignore") { request =>
    Accumulator.done(Right(body))
  }

  /**
   * A body parser that always returns an error.
   */
  def error[A](result: Future[Result]): BodyParser[A] =
    BodyParser("error")(_ => Accumulator.done(result.map(Left.apply)(Execution.trampoline)))

  /**
   * Allows to choose the right BodyParser parser to use by examining the request headers.
   */
  def using[A](f: RequestHeader => BodyParser[A]) = BodyParser(request => f(request)(request))

  /**
   * A body parser that flattens a future BodyParser.
   */
  def flatten[A](underlying: Future[BodyParser[A]])(implicit ec: ExecutionContext, mat: Materializer): BodyParser[A] =
    BodyParser(request => Accumulator.flatten(underlying.map(_(request))))

  /**
   * Creates a conditional BodyParser.
   */
  def when[A](
      predicate: RequestHeader => Boolean,
      parser: BodyParser[A],
      badResult: RequestHeader => Future[Result]
  ): BodyParser[A] = {
    BodyParser(s"conditional, wrapping=$parser") { request =>
      if (predicate(request)) {
        parser(request)
      } else {
        Accumulator.done(badResult(request).map(Left.apply)(Execution.trampoline))
      }
    }
  }

  /**
   * Wrap an existing BodyParser with a maxLength constraints.
   *
   * @param maxLength The max length allowed
   * @param parser The BodyParser to wrap
   */
  def maxLength[A](maxLength: Long, parser: BodyParser[A])(
      implicit mat: Materializer
  ): BodyParser[Either[MaxSizeExceeded, A]] =
    BodyParser(s"maxLength=$maxLength, wrapping=$parser") { request =>
      if (BodyParserUtils.contentLengthHeaderExceedsMaxLength(request, maxLength)) {
        Accumulator.done(Future.successful(Right(Left(MaxSizeExceeded(maxLength)))))
      } else {
        val takeUpToFlow = Flow.fromGraph(new BodyParsers.TakeUpTo(maxLength))

        // Apply the request
        val parserSink = parser.apply(request).toSink

        Accumulator(takeUpToFlow.toMat(parserSink) { (statusFuture, resultFuture) =>
          import Execution.Implicits.trampoline
          statusFuture.flatMap {
            case exceeded: MaxSizeExceeded => Future.successful(Right(Left(exceeded)))
            case _ =>
              resultFuture.map {
                case Left(result) => Left(result)
                case Right(a)     => Right(Right(a))
              }
          }
        })
      }
    }
}

object BodyParserUtils {
  /**
   * @param request The request whose Content-Length header will be checked (if it exists).
   * @param maxLength Maximum allowed bytes.
   * @return true if the request's Content-Length header value is greater than maxLength.
   *         false otherwise or if the request does not have a Content-Length header (or if it can't be parsed).
   */
  def contentLengthHeaderExceedsMaxLength(request: RequestHeader, maxLength: Long) =
    request.headers
      .get(HeaderNames.CONTENT_LENGTH)
      .flatMap(clh => catching(classOf[NumberFormatException]).opt(clh.toLong))
      .exists(_ > maxLength)
}

class DefaultPlayBodyParsers @Inject() (
    val config: ParserConfiguration,
    val errorHandler: HttpErrorHandler,
    val materializer: Materializer,
    val temporaryFileCreator: TemporaryFileCreator
) extends PlayBodyParsers

object PlayBodyParsers {
  /**
   * A helper method for creating PlayBodyParsers. The default values are mainly useful in testing, and default the
   * TemporaryFileCreator and HttpErrorHandler to singleton versions.
   */
  def apply(
      tfc: TemporaryFileCreator = SingletonTemporaryFileCreator,
      eh: HttpErrorHandler = new DefaultHttpErrorHandler(),
      conf: ParserConfiguration = ParserConfiguration()
  )(implicit mat: Materializer): PlayBodyParsers = {
    new DefaultPlayBodyParsers(conf, eh, mat, tfc)
  }
}

/**
 * Body parsers officially supported by Play (i.e. built-in to Play)
 */
trait PlayBodyParsers extends BodyParserUtils {
  private val logger = Logger(classOf[PlayBodyParsers])

  private[play] implicit def materializer: Materializer
  private[play] def config: ParserConfiguration
  private[play] def errorHandler: HttpErrorHandler
  private[play] def temporaryFileCreator: TemporaryFileCreator

  /**
   * Unlimited size.
   */
  val UNLIMITED: Long = Long.MaxValue

  private[play] val ApplicationXmlMatcher = """application/.*\+xml.*""".r

  /**
   * Default max length allowed for text based body.
   *
   * You can configure it in application.conf:
   *
   * {{{
   * play.http.parser.maxMemoryBuffer = 512k
   * }}}
   */
  def DefaultMaxTextLength: Long = config.maxMemoryBuffer

  /**
   * Default max length allowed for disk based body.
   *
   * You can configure it in application.conf:
   *
   * {{{
   * play.http.parser.maxDiskBuffer = 512k
   * }}}
   */
  def DefaultMaxDiskLength: Long = config.maxDiskBuffer

  // -- Text parser

  /**
   * Parses the body as text without checking the Content-Type.
   *
   * Will attempt to parse content with an explicit charset, but will fallback to UTF-8, ISO-8859-1, and finally US-ASCII if incorrect characters are detected.
   *
   * @param maxLength Max length (in bytes) allowed or returns EntityTooLarge HTTP response.
   */
  def tolerantText(maxLength: Long): BodyParser[String] =
    tolerantBodyParser("text", maxLength, "Error decoding text body") { (request, bytes) =>
      val byteBuffer = bytes.toByteBuffer

      def decode(encodingToTry: Charset): Try[String] = {
        import java.nio.charset.CodingErrorAction
        val decoder = encodingToTry.newDecoder.onMalformedInput(CodingErrorAction.REPORT)
        try {
          Success(decoder.decode(byteBuffer).toString)
        } catch {
          case e: CharacterCodingException =>
            logger.warn(
              s"TolerantText body parser tried to parse request ${request.id} as text body with charset $encodingToTry, but it contains invalid characters!"
            )
            Failure(e)
          case e: Exception =>
            logger.error("Unexpected exception while decoding text/plain body", e)
            Failure(e)
        }
      }

      // Run through a common set of encoders to get an idea of the best character encoding.

      // Per RFC-7321, "The default charset of ISO-8859-1 for text media types has been removed; the default is now
      // whatever the media type definition says." and
      // The default "charset" parameter value for "text/plain" is unchanged from [RFC2046] and remains as "US-ASCII".
      // https://tools.ietf.org/html/rfc6657#section-4
      val charset = request.charset.fold(US_ASCII)(Charset.forName)
      decode(charset)
        .recoverWith {
          case _: CharacterCodingException => decode(UTF_8)
        }
        .recoverWith {
          case _: CharacterCodingException => decode(ISO_8859_1)
        }
        .getOrElse {
          // We can't get a decent charset.  If we added https://github.com/albfernandez/juniversalchardet
          // then we could guess at the encoding, but that's best done in userspace rather than adding
          // it into the core...
          bytes.decodeString(charset)
        }
    }

  /**
   * Parse the body as text without checking the Content-Type.
   */
  def tolerantText: BodyParser[String] = tolerantText(DefaultMaxTextLength)

  /**
   * Parse the body as text if the Content-Type is text/plain.
   *
   * If the charset is not explicitly declared, then the default "charset" parameter value is US-ASCII,
   * per https://tools.ietf.org/html/rfc6657#section-4.  Use tolerantText if more flexible character
   * decoding is desired.
   *
   * @param maxLength Max length (in bytes) allowed or returns EntityTooLarge HTTP response.
   */
  def text(maxLength: Long): BodyParser[String] = {
    BodyParser("text") { request =>
      if (request.contentType.exists(_.equalsIgnoreCase("text/plain"))) {
        val bodyParser = tolerantBodyParser("text", maxLength, "Error decoding text body") { (request, bytes) =>
          val charset = request.charset.fold(US_ASCII)(Charset.forName)
          import java.nio.charset.CodingErrorAction
          val decoder = charset.newDecoder.onMalformedInput(CodingErrorAction.REPORT)
          try {
            // Render with assumption that all characters are valid
            decoder.decode(bytes.toByteBuffer).toString
          } catch {
            case e: CharacterCodingException =>
              // Log a warning, and render to the given charset with unmappable characters.
              // This is slower (exception + 2 * rendering) but the happy path is just as fast.
              logger.warn(
                s"Text body parser tried to parse request ${request.id} as text body with charset $charset, but it contains invalid characters!"
              )
              bytes.decodeString(charset)
          }
        }
        bodyParser(request)
      } else {
        Accumulator.done {
          val badResult = createBadResult("Expecting text/plain body", UNSUPPORTED_MEDIA_TYPE)
          badResult(request).map(Left.apply)(Execution.trampoline)
        }
      }
    }
  }

  /**
   * Parse the body as text if the Content-Type is text/plain.
   */
  def text: BodyParser[String] = text(DefaultMaxTextLength)

  /**
   * Buffer the body as a simple [[akka.util.ByteString]].
   *
   * @param maxLength Max length (in bytes) allowed or returns EntityTooLarge HTTP response.
   */
  def byteString(maxLength: Long): BodyParser[ByteString] = {
    tolerantBodyParser("byteString", maxLength, "Error decoding byte string body")((_, bytes) => bytes)
  }

  /**
   * Buffer the body as a simple [[akka.util.ByteString]].
   *
   * Will buffer up to the configured max memory buffer amount, after which point, it will return an EntityTooLarge
   * HTTP response.
   */
  def byteString: BodyParser[ByteString] = byteString(config.maxMemoryBuffer)

  // -- Raw parser

  /**
   * Store the body content in a RawBuffer.
   *
   * @param memoryThreshold If the content size is bigger than this limit, the content is stored as file.
   *
   * @see [[DefaultMaxDiskLength]]
   * @see [[Results.EntityTooLarge]]
   */
  def raw(memoryThreshold: Long = DefaultMaxTextLength, maxLength: Long = DefaultMaxDiskLength): BodyParser[RawBuffer] =
    BodyParser("raw, memoryThreshold=" + memoryThreshold) { request =>
      import Execution.Implicits.trampoline
      enforceMaxLength(
        request,
        maxLength,
        Accumulator
          .strict[ByteString, RawBuffer](
            { maybeStrictBytes =>
              Future.successful(
                RawBuffer(memoryThreshold, temporaryFileCreator, maybeStrictBytes.getOrElse(ByteString.empty))
              )
            }, {
              val buffer = RawBuffer(memoryThreshold, temporaryFileCreator)
              val sink = Sink.fold[RawBuffer, ByteString](buffer) { (bf, bs) =>
                bf.push(bs); bf
              }
              sink.mapMaterializedValue { future =>
                future.andThen { case _ => buffer.close() }
              }
            }
          )
          .map(buffer => Right(buffer))
      )
    }

  /**
   * Store the body content in a RawBuffer.
   */
  def raw: BodyParser[RawBuffer] = raw()

  // -- JSON parser

  /**
   * Parse the body as Json without checking the Content-Type.
   *
   * @param maxLength Max length (in bytes) allowed or returns EntityTooLarge HTTP response.
   */
  def tolerantJson(maxLength: Long): BodyParser[JsValue] =
    tolerantBodyParser[JsValue]("json", maxLength, "Invalid Json") { (request, bytes) =>
      // Encoding notes: RFC 4627 requires that JSON be encoded in Unicode, and states that whether that's
      // UTF-8, UTF-16 or UTF-32 can be auto detected by reading the first two bytes. So we ignore the declared
      // charset and don't decode, we passing the byte array as is because Jackson supports auto detection.
      Json.parse(bytes.iterator.asInputStream)
    }

  /**
   * Parse the body as Json without checking the Content-Type.
   */
  def tolerantJson: BodyParser[JsValue] = tolerantJson(DefaultMaxTextLength)

  /**
   * Parse the body as Json without checking the Content-Type,
   * validating the result with the Json reader.
   *
   * @tparam A the type to read and validate from the body.
   * @param reader a Json reader for type A.
   */
  def tolerantJson[A](implicit reader: Reads[A]): BodyParser[A] = jsonReads(tolerantJson)

  /**
   * Parse the body as Json if the Content-Type is text/json or application/json.
   *
   * @param maxLength Max length (in bytes) allowed or returns EntityTooLarge HTTP response.
   */
  def json(maxLength: Long): BodyParser[JsValue] = when(
    _.contentType.exists(m => m.equalsIgnoreCase("text/json") || m.equalsIgnoreCase("application/json")),
    tolerantJson(maxLength),
    createBadResult("Expecting text/json or application/json body", UNSUPPORTED_MEDIA_TYPE)
  )

  /**
   * Parse the body as Json if the Content-Type is text/json or application/json.
   */
  def json: BodyParser[JsValue] = json(DefaultMaxTextLength)

  /**
   * Parse the body as Json if the Content-Type is text/json or application/json,
   * validating the result with the Json reader.
   *
   * @tparam A the type to read and validate from the body.
   * @param reader a Json reader for type A.
   */
  def json[A](implicit reader: Reads[A]): BodyParser[A] = jsonReads(json)

  /**
   * Parse the body as Json given a BodyParser,
   * validating the result with the Json reader.
   */
  private def jsonReads[A](parser: BodyParser[JsValue])(implicit reader: Reads[A]): BodyParser[A] =
    BodyParser("json reader") { request =>
      import Execution.Implicits.trampoline
      parser(request).mapFuture {
        case Left(simpleResult) =>
          Future.successful(Left(simpleResult))
        case Right(jsValue) =>
          jsValue
            .validate(reader)
            .map { a =>
              Future.successful(Right(a))
            }
            .recoverTotal { jsError =>
              val msg = s"Json validation error ${JsError.toFlatForm(jsError)}"
              createBadResult(msg)(request).map(Left.apply)
            }
      }
    }

  // -- Form parser

  /**
   * Parse the body and binds it to a given form model.
   *
   * {{{
   *   case class User(name: String)
   *
   *   val userForm: Form[User] = Form(mapping("name" -> nonEmptyText)(User.apply)(User.unapply))
   *
   *   Action(parse.form(userForm)) { request =>
   *     Ok(s"Hello, \${request.body.name}!")
   *   }
   * }}}
   *
   * @param form Form model
   * @param maxLength Max length (in bytes) allowed or returns EntityTooLarge HTTP response. If `None`, the default `play.http.parser.maxMemoryBuffer` configuration value is used.
   * @param onErrors The result to reply in case of errors during the form binding process
   */
  def form[A](
      form: Form[A],
      maxLength: Option[Long] = None,
      onErrors: Form[A] => Result = (_: Form[A]) => Results.BadRequest
  ): BodyParser[A] =
    BodyParser { requestHeader =>
      val parser = anyContent(maxLength)
      parser(requestHeader).map { resultOrBody =>
        resultOrBody.right.flatMap { body =>
          form
            .bindFromRequest()(Request[AnyContent](requestHeader, body))
            .fold(formErrors => Left(onErrors(formErrors)), a => Right(a))
        }
      }(Execution.trampoline)
    }

  // -- XML parser

  /**
   * Parse the body as Xml without checking the Content-Type.
   *
   * @param maxLength Max length (in bytes) allowed or returns EntityTooLarge HTTP response.
   */
  def tolerantXml(maxLength: Long): BodyParser[NodeSeq] =
    tolerantBodyParser[NodeSeq]("xml", maxLength, "Invalid XML") { (request, bytes) =>
      val inputSource = new InputSource(bytes.iterator.asInputStream)

      // Encoding notes: RFC 3023 is the RFC for XML content types.  Comments below reflect what it says.

      // An externally declared charset takes precedence
      request.charset
        .orElse(
          // If omitted, maybe select a default charset, based on the media type.
          request.mediaType.collect {
            // According to RFC 3023, the default encoding for text/xml is us-ascii. This contradicts RFC 2616, which
            // states that the default for text/* is ISO-8859-1.  An RFC 3023 conforming client will send US-ASCII,
            // in that case it is safe for us to use US-ASCII or ISO-8859-1.  But a client that knows nothing about
            // XML, and therefore nothing about RFC 3023, but rather conforms to RFC 2616, will send ISO-8859-1.
            // Since decoding as ISO-8859-1 works for both clients that conform to RFC 3023, and clients that conform
            // to RFC 2616, we use that.
            case mt if mt.mediaType == "text" => "iso-8859-1"
            // Otherwise, there should be no default, it will be detected by the XML parser.
          }
        )
        .foreach { charset =>
          inputSource.setEncoding(charset)
        }
      Play.XML.load(inputSource)
    }

  /**
   * Parse the body as Xml without checking the Content-Type.
   */
  def tolerantXml: BodyParser[NodeSeq] = tolerantXml(DefaultMaxTextLength)

  /**
   * Parse the body as Xml if the Content-Type is application/xml, text/xml or application/XXX+xml.
   *
   * @param maxLength Max length (in bytes) allowed or returns EntityTooLarge HTTP response.
   */
  def xml(maxLength: Long): BodyParser[NodeSeq] = when(
    _.contentType.exists { t =>
      val tl = t.toLowerCase(Locale.ENGLISH)
      tl.startsWith("text/xml") || tl
        .startsWith("application/xml") || ApplicationXmlMatcher.pattern.matcher(tl).matches()
    },
    tolerantXml(maxLength),
    createBadResult("Expecting xml body", UNSUPPORTED_MEDIA_TYPE)
  )

  /**
   * Parse the body as Xml if the Content-Type is application/xml, text/xml or application/XXX+xml.
   */
  def xml: BodyParser[NodeSeq] = xml(DefaultMaxTextLength)

  // -- File parsers

  /**
   * Store the body content into a file.
   *
   * @param to The file used to store the content.
   * @param maxLength Max length (in bytes) allowed or returns EntityTooLarge HTTP response.
   */
  def file(to: File, maxLength: Long): BodyParser[File] = BodyParser(s"file, to=$to") { request =>
    import Execution.Implicits.trampoline
    val bodyAccumulator =
      Accumulator(StreamConverters.fromOutputStream(() => Files.newOutputStream(to.toPath))).map(_ => Right(to))
    enforceMaxLength(request, maxLength, bodyAccumulator)
  }

  /**
   * Store the body content into a file.
   *
   * @param to The file used to store the content.
   */
  def file(to: File): BodyParser[File] = file(to, DefaultMaxDiskLength)

  private def requestEntityTooLarge(request: RequestHeader) =
    createBadResult("Request Entity Too Large", REQUEST_ENTITY_TOO_LARGE)(request).map(Left(_))(Execution.trampoline)

  /**
   * Store the body content into a temporary file.
   *
   * @param maxLength Max length (in bytes) allowed or returns EntityTooLarge HTTP response.
   */
  def temporaryFile(maxLength: Long): BodyParser[TemporaryFile] = BodyParser("temporaryFile") { request =>
    if (BodyParserUtils.contentLengthHeaderExceedsMaxLength(request, maxLength)) {
      // We check early here already to not even create a temporary file
      Accumulator.done(requestEntityTooLarge(request))
    } else {
      val tempFile = temporaryFileCreator.create("requestBody", "asTemporaryFile")
      file(tempFile, maxLength)(request).map(_.fold(result => Left(result), _ => Right(tempFile)))(Execution.trampoline)
    }
  }

  /**
   * Store the body content into a temporary file.
   */
  def temporaryFile: BodyParser[TemporaryFile] = temporaryFile(DefaultMaxDiskLength)

  // -- FormUrlEncoded

  /**
   * Parse the body as Form url encoded without checking the Content-Type.
   *
   * @param maxLength Max length (in bytes) allowed or returns EntityTooLarge HTTP response.
   */
  def tolerantFormUrlEncoded(maxLength: Long): BodyParser[Map[String, Seq[String]]] =
    tolerantBodyParser("formUrlEncoded", maxLength, "Error parsing application/x-www-form-urlencoded") {
      (request, bytes) =>
        import play.core.parsers._
        val charset          = request.charset.getOrElse("UTF-8")
        val urlEncodedString = bytes.decodeString("UTF-8")
        FormUrlEncodedParser.parse(urlEncodedString, charset)
    }

  /**
   * Parse the body as form url encoded without checking the Content-Type.
   */
  def tolerantFormUrlEncoded: BodyParser[Map[String, Seq[String]]] =
    tolerantFormUrlEncoded(DefaultMaxTextLength)

  /**
   * Parse the body as form url encoded if the Content-Type is application/x-www-form-urlencoded.
   *
   * @param maxLength Max length (in bytes) allowed or returns EntityTooLarge HTTP response.
   */
  def formUrlEncoded(maxLength: Long): BodyParser[Map[String, Seq[String]]] = when(
    _.contentType.exists(_.equalsIgnoreCase("application/x-www-form-urlencoded")),
    tolerantFormUrlEncoded(maxLength),
    createBadResult("Expecting application/x-www-form-urlencoded body", UNSUPPORTED_MEDIA_TYPE)
  )

  /**
   * Parse the body as form url encoded if the Content-Type is application/x-www-form-urlencoded.
   */
  def formUrlEncoded: BodyParser[Map[String, Seq[String]]] =
    formUrlEncoded(DefaultMaxTextLength)

  // -- Magic any content

  /**
   * If the request has a body, parse the body content by checking the Content-Type header.
   */
  def default: BodyParser[AnyContent] = default(None)

  // this is an alias method since "default" is a Java reserved word
  def defaultBodyParser: BodyParser[AnyContent] = default

  /**
   * If the request has a body, parse the body content by checking the Content-Type header.
   */
  def default(maxLength: Option[Long]): BodyParser[AnyContent] = using { request =>
    if (request.hasBody) {
      anyContent(maxLength)
    } else {
      ignore(AnyContentAsEmpty)
    }
  }

  /**
   * Guess the body content by checking the Content-Type header.
   */
  def anyContent: BodyParser[AnyContent] = anyContent(None)

  /**
   * Guess the body content by checking the Content-Type header.
   */
  def anyContent(maxLength: Option[Long]): BodyParser[AnyContent] = BodyParser("anyContent") { request =>
    import Execution.Implicits.trampoline

    def maxLengthOrDefault          = maxLength.fold(DefaultMaxTextLength)(_.toInt)
    def maxLengthOrDefaultLarge     = maxLength.getOrElse(DefaultMaxDiskLength)
    val contentType: Option[String] = request.contentType.map(_.toLowerCase(Locale.ENGLISH))
    contentType match {
      case Some("text/plain") =>
        logger.trace("Parsing AnyContent as text")
        text(maxLengthOrDefault)(request).map(_.right.map(s => AnyContentAsText(s)))

      case Some("text/xml") | Some("application/xml") | Some(ApplicationXmlMatcher()) =>
        logger.trace("Parsing AnyContent as xml")
        xml(maxLengthOrDefault)(request).map(_.right.map(x => AnyContentAsXml(x)))

      case Some("text/json") | Some("application/json") =>
        logger.trace("Parsing AnyContent as json")
        json(maxLengthOrDefault)(request).map(_.right.map(j => AnyContentAsJson(j)))

      case Some("application/x-www-form-urlencoded") =>
        logger.trace("Parsing AnyContent as urlFormEncoded")
        formUrlEncoded(maxLengthOrDefault)(request).map(_.right.map(d => AnyContentAsFormUrlEncoded(d)))

      case Some("multipart/form-data") =>
        logger.trace("Parsing AnyContent as multipartFormData")
        multipartFormData(Multipart.handleFilePartAsTemporaryFile(temporaryFileCreator), maxLengthOrDefaultLarge)
          .apply(request)
          .map(_.right.map(m => AnyContentAsMultipartFormData(m)))

      case _ =>
        logger.trace("Parsing AnyContent as raw")
        raw(DefaultMaxTextLength, maxLengthOrDefaultLarge)(request).map(_.right.map(r => AnyContentAsRaw(r)))
    }
  }

  // -- Multipart

  /**
   * Parse the content as multipart/form-data
   */
  def multipartFormData: BodyParser[MultipartFormData[TemporaryFile]] =
    multipartFormData(Multipart.handleFilePartAsTemporaryFile(temporaryFileCreator))

  /**
   * Parse the content as multipart/form-data
   *
   * @param maxLength Max length (in bytes) allowed or returns EntityTooLarge HTTP response.
   */
  def multipartFormData(maxLength: Long): BodyParser[MultipartFormData[TemporaryFile]] =
    multipartFormData(Multipart.handleFilePartAsTemporaryFile(temporaryFileCreator), maxLength)

  /**
   * Parse the content as multipart/form-data
   *
   * @param filePartHandler Handles file parts.
   * @param maxLength Max length (in bytes) allowed or returns EntityTooLarge HTTP response.
   *
   * @see [[DefaultMaxDiskLength]]
   * @see [[Results.EntityTooLarge]]
   */
  def multipartFormData[A](
      filePartHandler: Multipart.FilePartHandler[A],
      maxLength: Long = DefaultMaxDiskLength
  ): BodyParser[MultipartFormData[A]] = {
    BodyParser("multipartFormData") { request =>
      val bodyAccumulator =
        Multipart.multipartParser(DefaultMaxTextLength, filePartHandler, errorHandler).apply(request)
      enforceMaxLength(request, maxLength, bodyAccumulator)
    }
  }

  protected def createBadResult(msg: String, statusCode: Int = BAD_REQUEST): RequestHeader => Future[Result] = {
    request =>
      errorHandler.onClientError(request, statusCode, msg)
  }

  /**
   * Enforce the max length on the stream consumed by the given accumulator.
   */
  private[play] def enforceMaxLength[A](
      request: RequestHeader,
      maxLength: Long,
      accumulator: Accumulator[ByteString, Either[Result, A]]
  ): Accumulator[ByteString, Either[Result, A]] = {
    if (BodyParserUtils.contentLengthHeaderExceedsMaxLength(request, maxLength)) {
      Accumulator.done(requestEntityTooLarge(request))
    } else {
      val takeUpToFlow = Flow.fromGraph(new BodyParsers.TakeUpTo(maxLength))
      Accumulator(takeUpToFlow.toMat(accumulator.toSink) { (statusFuture, resultFuture) =>
        statusFuture.flatMap {
          case MaxSizeExceeded(_) => requestEntityTooLarge(request)
          case MaxSizeNotExceeded => resultFuture
        }(Execution.trampoline)
      })
    }
  }

  /**
   * Create a body parser that uses the given parser and enforces the given max length.
   *
   * @param name The name of the body parser.
   * @param maxLength The maximum length of the body to buffer.
   * @param errorMessage The error message to prepend to the exception message if an error was encountered.
   * @param parser The parser.
   */
  protected def tolerantBodyParser[A](name: String, maxLength: Long, errorMessage: String)(
      parser: (RequestHeader, ByteString) => A
  ): BodyParser[A] =
    BodyParser(name + ", maxLength=" + maxLength) { request =>
      import Execution.Implicits.trampoline

      def parseBody(bytes: ByteString): Future[Either[Result, A]] = {
        try {
          Future.successful(Right(parser(request, bytes)))
        } catch {
          case NonFatal(e) =>
            logger.debug(errorMessage, e)
            createBadResult(errorMessage + ": " + e.getMessage)(request).map(Left(_))
        }
      }

      if (BodyParserUtils.contentLengthHeaderExceedsMaxLength(request, maxLength)) {
        Accumulator.done(requestEntityTooLarge(request))
      } else {
        Accumulator.strict[ByteString, Either[Result, A]](
          // If the body was strict
          {
            case Some(bytes) if bytes.size <= maxLength =>
              parseBody(bytes)
            case None =>
              parseBody(ByteString.empty)
            case _ => requestEntityTooLarge(request)
          },
          // Otherwise, use an enforce max length accumulator on a folding sink
          enforceMaxLength(
            request,
            maxLength,
            Accumulator(
              Sink.fold[ByteString, ByteString](ByteString.empty)((state, bs) => state ++ bs)
            ).mapFuture(parseBody)
          ).toSink
        )
      }
    }
}

/**
 * Default BodyParsers.
 */
object BodyParsers {
  /**
   * The default body parser provided by Play
   */
  class Default @Inject() (parse: PlayBodyParsers) extends BodyParser[AnyContent] {
    /**
     * An alternate constructor primarily designed for unit testing. Default values are set to empty or singleton
     * implementations where appropriate.
     */
    def this(
        tfc: TemporaryFileCreator = SingletonTemporaryFileCreator,
        eh: HttpErrorHandler = new DefaultHttpErrorHandler(),
        config: ParserConfiguration = ParserConfiguration()
    )(implicit mat: Materializer) = this(PlayBodyParsers(tfc, eh, config))
    override def apply(rh: RequestHeader) = parse.default(None)(rh)
  }

  object utils extends BodyParserUtils

  private[play] def takeUpTo(maxLength: Long): Graph[FlowShape[ByteString, ByteString], Future[MaxSizeStatus]] =
    new TakeUpTo(maxLength)

  private[play] class TakeUpTo(maxLength: Long)
      extends GraphStageWithMaterializedValue[FlowShape[ByteString, ByteString], Future[MaxSizeStatus]] {
    private val in  = Inlet[ByteString]("TakeUpTo.in")
    private val out = Outlet[ByteString]("TakeUpTo.out")

    override def shape: FlowShape[ByteString, ByteString] = FlowShape.of(in, out)

    override def createLogicAndMaterializedValue(
        inheritedAttributes: Attributes
    ): (GraphStageLogic, Future[MaxSizeStatus]) = {
      val status            = Promise[MaxSizeStatus]()
      var pushedBytes: Long = 0

      val logic = new GraphStageLogic(shape) {
        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            pull(in)
          }
          override def onDownstreamFinish(): Unit = {
            status.success(MaxSizeNotExceeded)
            completeStage()
          }
        })
        setHandler(
          in,
          new InHandler {
            override def onPush(): Unit = {
              val chunk = grab(in)
              pushedBytes += chunk.size
              if (pushedBytes > maxLength) {
                status.success(MaxSizeExceeded(maxLength))
                // Make sure we fail the stream, this will ensure downstream body parsers don't try to parse it
                failStage(new MaxLengthLimitAttained)
              } else {
                push(out, chunk)
              }
            }
            override def onUpstreamFinish(): Unit = {
              status.success(MaxSizeNotExceeded)
              completeStage()
            }
            override def onUpstreamFailure(ex: Throwable): Unit = {
              status.failure(ex)
              failStage(ex)
            }
          }
        )
      }

      (logic, status.future)
    }
  }

  private[play] class MaxLengthLimitAttained extends RuntimeException(null, null, false, false)
}

/**
 * The status of a max size flow.
 */
sealed trait MaxSizeStatus

/**
 * Signal a max content size exceeded.
 */
case class MaxSizeExceeded(length: Long) extends MaxSizeStatus

/**
 * Signal max size is not exceeded.
 */
case object MaxSizeNotExceeded extends MaxSizeStatus
