/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

import akka.util.ByteString
import play.api.data.Form
import play.api.libs.streams.{ Streams, Accumulator }
import play.core.parsers.Multipart
import scala.language.reflectiveCalls
import java.io._
import scala.concurrent.Future
import scala.xml._
import play.api._
import play.api.libs.json._
import play.api.libs.Files.TemporaryFile
import MultipartFormData._
import java.util.Locale
import scala.util.control.NonFatal
import play.api.http.{ LazyHttpErrorHandler, ParserConfiguration, HttpConfiguration, HttpVerbs }
import play.utils.PlayIO
import play.api.http.Status._
import akka.stream.Materializer
import akka.stream.scaladsl.{ Flow, Sink }
import akka.stream.stage.{ Context, PushStage, SyncDirective }

/**
 * A request body that adapts automatically according the request Content-Type.
 */
sealed trait AnyContent {

  /**
   * application/form-url-encoded
   */
  def asFormUrlEncoded: Option[Map[String, Seq[String]]] = this match {
    case AnyContentAsFormUrlEncoded(data) => Some(data)
    case _ => None
  }

  /**
   * text/plain
   */
  def asText: Option[String] = this match {
    case AnyContentAsText(txt) => Some(txt)
    case _ => None
  }

  /**
   * application/xml
   */
  def asXml: Option[NodeSeq] = this match {
    case AnyContentAsXml(xml) => Some(xml)
    case _ => None
  }

  /**
   * text/json or application/json
   */
  def asJson: Option[JsValue] = this match {
    case AnyContentAsJson(json) => Some(json)
    case _ => None
  }

  /**
   * multipart/form-data
   */
  def asMultipartFormData: Option[MultipartFormData[TemporaryFile]] = this match {
    case AnyContentAsMultipartFormData(mfd) => Some(mfd)
    case _ => None
  }

  /**
   * Used when no Content-Type matches
   */
  def asRaw: Option[RawBuffer] = this match {
    case AnyContentAsRaw(raw) => Some(raw)
    case _ => None
  }

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
case class AnyContentAsMultipartFormData(mdf: MultipartFormData[TemporaryFile]) extends AnyContent

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
  case class FilePart[A](key: String, filename: String, contentType: Option[String], ref: A) extends Part[A]

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
 */
case class RawBuffer(memoryThreshold: Int, initialData: ByteString = ByteString.empty) {

  import play.api.libs.Files._

  @volatile private var inMemory: ByteString = initialData
  @volatile private var backedByTemporaryFile: TemporaryFile = _
  @volatile private var outStream: OutputStream = _

  private[play] def push(chunk: ByteString) {
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

  private[play] def close() {
    if (outStream != null) {
      outStream.close()
    }
  }

  private[play] def backToTemporaryFile() {
    backedByTemporaryFile = TemporaryFile("requestBody", "asRaw")
    outStream = new FileOutputStream(backedByTemporaryFile.file)
    outStream.write(inMemory.toArray)
    inMemory = null
  }

  /**
   * Buffer size.
   */
  def size: Long = {
    if (inMemory != null) inMemory.size else backedByTemporaryFile.file.length
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
      Some(if (inMemory != null) {
        inMemory
      } else {
        ByteString(PlayIO.readFile(backedByTemporaryFile.file))
      })
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
    backedByTemporaryFile.file
  }

  override def toString = {
    "RawBuffer(inMemory=" + Option(inMemory).map(_.size).orNull + ", backedByTemporaryFile=" + backedByTemporaryFile + ")"
  }

}

/**
 * Default body parsers.
 */
trait BodyParsers {

  import BodyParsers._

  /**
   * Default body parsers.
   */
  object parse {

    /**
     * Unlimited size.
     */
    val UNLIMITED: Long = Long.MaxValue

    private[play] val ApplicationXmlMatcher = """application/.*\+xml.*""".r

    private def config = Play.maybeApplication.map(app => hcCache(app).parser)
      .getOrElse(ParserConfiguration())

    /**
     * Default max length allowed for text based body.
     *
     * You can configure it in application.conf:
     *
     * {{{
     * play.http.parser.maxMemoryBuffer = 512k
     * }}}
     */
    def DefaultMaxTextLength: Int = config.maxMemoryBuffer

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
     * Parse the body as text without checking the Content-Type.
     *
     * @param maxLength Max length allowed or returns EntityTooLarge HTTP response.
     */
    def tolerantText(maxLength: Long): BodyParser[String] = {
      tolerantBodyParser("text", maxLength, "Error decoding text body") { (request, bytes) =>
        // Encoding notes: RFC-2616 section 3.7.1 mandates ISO-8859-1 as the default charset if none is specified.
        bytes.decodeString(request.charset.getOrElse("ISO-8859-1"))
      }
    }

    /**
     * Parse the body as text without checking the Content-Type.
     */
    def tolerantText: BodyParser[String] = tolerantText(DefaultMaxTextLength)

    /**
     * Parse the body as text if the Content-Type is text/plain.
     *
     * @param maxLength Max length allowed or returns EntityTooLarge HTTP response.
     */
    def text(maxLength: Int): BodyParser[String] = when(
      _.contentType.exists(_.equalsIgnoreCase("text/plain")),
      tolerantText(maxLength),
      createBadResult("Expecting text/plain body", UNSUPPORTED_MEDIA_TYPE)
    )

    /**
     * Parse the body as text if the Content-Type is text/plain.
     */
    def text: BodyParser[String] = text(DefaultMaxTextLength)

    // -- Raw parser

    /**
     * Store the body content in a RawBuffer.
     *
     * @param memoryThreshold If the content size is bigger than this limit, the content is stored as file.
     */
    def raw(memoryThreshold: Int = DefaultMaxTextLength, maxLength: Long = DefaultMaxDiskLength): BodyParser[RawBuffer] =
      BodyParser("raw, memoryThreshold=" + memoryThreshold) { request =>
        import play.api.libs.iteratee.Execution.Implicits.trampoline
        enforceMaxLength(request, maxLength, Accumulator {
          val buffer = RawBuffer(memoryThreshold)
          val sink = Sink.fold[RawBuffer, ByteString](buffer) { (bf, bs) => bf.push(bs); bf }
          sink.mapMaterializedValue { future =>
            future andThen { case _ => buffer.close() }
          }
        } map (buffer => Right(buffer)))
      }

    /**
     * Store the body content in a RawBuffer.
     */
    def raw: BodyParser[RawBuffer] = raw()

    // -- JSON parser

    /**
     * Parse the body as Json without checking the Content-Type.
     *
     * @param maxLength Max length allowed or returns EntityTooLarge HTTP response.
     */
    def tolerantJson(maxLength: Int): BodyParser[JsValue] =
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
     * Parse the body as Json if the Content-Type is text/json or application/json.
     *
     * @param maxLength Max length allowed or returns EntityTooLarge HTTP response.
     */
    def json(maxLength: Int): BodyParser[JsValue] = when(
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
    def json[A](implicit reader: Reads[A]): BodyParser[A] =
      BodyParser("json reader") { request =>
        import play.api.libs.iteratee.Execution.Implicits.trampoline
        json(request) mapFuture {
          case Left(simpleResult) =>
            Future.successful(Left(simpleResult))
          case Right(jsValue) =>
            jsValue.validate(reader) map { a =>
              Future.successful(Right(a))
            } recoverTotal { jsError =>
              val msg = s"Json validation error ${JsError.toFlatForm(jsError)}"
              createBadResult(msg)(request) map Left.apply
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
     *     Ok(s"Hello, ${request.body.name}!")
     *   }
     * }}}
     *
     * @param form Form model
     * @param maxLength Max length allowed or returns EntityTooLarge HTTP response. If `None`, the default `play.http.parser.maxMemoryBuffer` configuration value is used.
     * @param onErrors The result to reply in case of errors during the form binding process
     */
    def form[A](form: Form[A], maxLength: Option[Long] = None, onErrors: Form[A] => Result = (formErrors: Form[A]) => Results.BadRequest): BodyParser[A] =
      BodyParser { requestHeader =>
        import play.api.libs.iteratee.Execution.Implicits.trampoline
        anyContent(maxLength)(requestHeader).map { resultOrBody =>
          resultOrBody.right.flatMap { body =>
            form
              .bindFromRequest()(Request[AnyContent](requestHeader, body))
              .fold(formErrors => Left(onErrors(formErrors)), a => Right(a))
          }
        }
      }

    // -- Empty parser

    /**
     * Don't parse the body content.
     */
    def empty: BodyParser[Unit] = ignore(Unit)

    def ignore[A](body: A): BodyParser[A] = BodyParser("ignore") { request =>
      Accumulator.done(Right(body))
    }

    // -- XML parser

    /**
     * Parse the body as Xml without checking the Content-Type.
     *
     * @param maxLength Max length allowed or returns EntityTooLarge HTTP response.
     */
    def tolerantXml(maxLength: Int): BodyParser[NodeSeq] =
      tolerantBodyParser[NodeSeq]("xml", maxLength, "Invalid XML") { (request, bytes) =>
        val inputSource = new InputSource(bytes.iterator.asInputStream)

        // Encoding notes: RFC 3023 is the RFC for XML content types.  Comments below reflect what it says.

        // An externally declared charset takes precedence
        request.charset.orElse(
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
        ).foreach { charset =>
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
     * @param maxLength Max length allowed or returns EntityTooLarge HTTP response.
     */
    def xml(maxLength: Int): BodyParser[NodeSeq] = when(
      _.contentType.exists { t =>
        val tl = t.toLowerCase(Locale.ENGLISH)
        tl.startsWith("text/xml") || tl.startsWith("application/xml") || ApplicationXmlMatcher.pattern.matcher(tl).matches()
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
     */
    def file(to: File): BodyParser[File] = BodyParser("file, to=" + to) { request =>
      import play.api.libs.iteratee.Execution.Implicits.trampoline
      Accumulator(Streams.outputStreamToSink(() => new FileOutputStream(to))).map(_ => Right(to))
    }

    /**
     * Store the body content into a temporary file.
     */
    def temporaryFile: BodyParser[TemporaryFile] = BodyParser("temporaryFile") { request =>
      val tempFile = TemporaryFile("requestBody", "asTemporaryFile")
      file(tempFile.file)(request).map(_ => Right(tempFile))(play.api.libs.iteratee.Execution.trampoline)
    }

    // -- FormUrlEncoded

    /**
     * Parse the body as Form url encoded without checking the Content-Type.
     *
     * @param maxLength Max length allowed or returns EntityTooLarge HTTP response.
     */
    def tolerantFormUrlEncoded(maxLength: Int): BodyParser[Map[String, Seq[String]]] =
      tolerantBodyParser("urlFormEncoded", maxLength, "Error parsing application/x-www-form-urlencoded") { (request, bytes) =>
        import play.core.parsers._
        FormUrlEncodedParser.parse(bytes.decodeString(request.charset.getOrElse("utf-8")),
          request.charset.getOrElse("utf-8"))
      }

    /**
     * Parse the body as form url encoded without checking the Content-Type.
     */
    def tolerantFormUrlEncoded: BodyParser[Map[String, Seq[String]]] =
      tolerantFormUrlEncoded(DefaultMaxTextLength)

    /**
     * Parse the body as form url encoded if the Content-Type is application/x-www-form-urlencoded.
     *
     * @param maxLength Max length allowed or returns EntityTooLarge HTTP response.
     */
    def urlFormEncoded(maxLength: Int): BodyParser[Map[String, Seq[String]]] = when(
      _.contentType.exists(_.equalsIgnoreCase("application/x-www-form-urlencoded")),
      tolerantFormUrlEncoded(maxLength),
      createBadResult("Expecting application/x-www-form-urlencoded body", UNSUPPORTED_MEDIA_TYPE)
    )

    /**
     * Parse the body as form url encoded if the Content-Type is application/x-www-form-urlencoded.
     */
    def urlFormEncoded: BodyParser[Map[String, Seq[String]]] =
      urlFormEncoded(DefaultMaxTextLength)

    // -- Magic any content

    /**
     * If the request is a PATCH, POST, or PUT, parse the body content by checking the Content-Type header.
     */
    def default: BodyParser[AnyContent] = default(None)

    /**
     * If the request is a PATCH, POST, or PUT, parse the body content by checking the Content-Type header.
     */
    def default(maxLength: Option[Long]): BodyParser[AnyContent] = using { request =>
      if (request.method == HttpVerbs.PATCH || request.method == HttpVerbs.POST || request.method == HttpVerbs.PUT) {
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
      import play.api.libs.iteratee.Execution.Implicits.trampoline

      def maxLengthOrDefault = maxLength.fold(DefaultMaxTextLength)(_.toInt)
      def maxLengthOrDefaultLarge = maxLength.getOrElse(DefaultMaxDiskLength)
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
          urlFormEncoded(maxLengthOrDefault)(request).map(_.right.map(d => AnyContentAsFormUrlEncoded(d)))

        case Some("multipart/form-data") =>
          logger.trace("Parsing AnyContent as multipartFormData")
          multipartFormData(Multipart.handleFilePartAsTemporaryFile, maxLengthOrDefaultLarge).apply(request)
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
      multipartFormData(Multipart.handleFilePartAsTemporaryFile)

    /**
     * Parse the content as multipart/form-data
     *
     * @param filePartHandler Handles file parts.
     */
    def multipartFormData[A](filePartHandler: Multipart.FilePartHandler[A], maxLength: Long = DefaultMaxDiskLength): BodyParser[MultipartFormData[A]] = {
      BodyParser("multipartFormData") { request =>
        implicit val mat = Play.current.materializer
        val bodyAccumulator = Multipart.multipartParser(DefaultMaxTextLength, filePartHandler).apply(request)
        enforceMaxLength(request, maxLength, bodyAccumulator)
      }
    }

    // -- Parsing utilities

    /**
     * Wrap an existing BodyParser with a maxLength constraints.
     *
     * @param maxLength The max length allowed
     * @param parser The BodyParser to wrap
     */
    def maxLength[A](maxLength: Long, parser: BodyParser[A])(implicit mat: Materializer): BodyParser[Either[MaxSizeExceeded, A]] = BodyParser("maxLength=" + maxLength + ", wrapping=" + parser.toString) { request =>
      import play.api.libs.iteratee.Execution.Implicits.trampoline
      val takeUpToFlow = Flow[ByteString].transform { () => new BodyParsers.TakeUpTo(maxLength) }
      // If the parser is successful, the body becomes Right(body)
      parser.map(Right.apply)
        // Apply the request
        .apply(request)
        // Send it through our takeUpToFlow
        .through(takeUpToFlow)
        // And convert a max length failure to Right(Left(MaxSizeExceeded))
        .recover {
          case _: BodyParsers.MaxLengthLimitAttained => Right(Left(MaxSizeExceeded(maxLength)))
        }
    }

    /**
     * A body parser that always returns an error.
     */
    def error[A](result: Future[Result]): BodyParser[A] = BodyParser("error") { request =>
      import play.api.libs.iteratee.Execution.Implicits.trampoline
      Accumulator.done(result.map(Left.apply))
    }

    /**
     * Allow to choose the right BodyParser parser to use by examining the request headers.
     */
    def using[A](f: RequestHeader => BodyParser[A]) = BodyParser { request =>
      f(request)(request)
    }

    /**
     * Create a conditional BodyParser.
     */
    def when[A](predicate: RequestHeader => Boolean, parser: BodyParser[A], badResult: RequestHeader => Future[Result]): BodyParser[A] = {
      BodyParser("conditional, wrapping=" + parser.toString) { request =>
        if (predicate(request)) {
          parser(request)
        } else {
          import play.api.libs.iteratee.Execution.Implicits.trampoline
          Accumulator.done(badResult(request).map(Left.apply))
        }
      }
    }

    private def createBadResult(msg: String, statusCode: Int = BAD_REQUEST): RequestHeader => Future[Result] = { request =>
      LazyHttpErrorHandler.onClientError(request, statusCode, msg)
    }

    /**
     * Enforce the max length on the stream consumed by the given accumulator.
     */
    private def enforceMaxLength[A](request: RequestHeader, maxLength: Long, accumulator: Accumulator[ByteString, Either[Result, A]]): Accumulator[ByteString, Either[Result, A]] = {
      val takeUpToFlow = Flow[ByteString].transform { () => new BodyParsers.TakeUpTo(maxLength) }
      import play.api.libs.concurrent.Execution.Implicits.defaultContext
      accumulator.through(takeUpToFlow).recoverWith {
        case _: BodyParsers.MaxLengthLimitAttained =>
          val badResult = createBadResult("Request Entity Too Large", REQUEST_ENTITY_TOO_LARGE)(request)
          badResult.map(Left(_))
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
    private def tolerantBodyParser[A](name: String, maxLength: Long, errorMessage: String)(parser: (RequestHeader, ByteString) => A): BodyParser[A] =
      BodyParser(name + ", maxLength=" + maxLength) { request =>
        import play.api.libs.iteratee.Execution.Implicits.trampoline

        enforceMaxLength(request, maxLength, Accumulator(
          Sink.fold[ByteString, ByteString](ByteString.empty)((state, bs) => state ++ bs)
        ) mapFuture { bytes =>
            try {
              Future.successful(Right(parser(request, bytes)))
            } catch {
              case NonFatal(e) =>
                logger.debug(errorMessage, e)
                createBadResult(errorMessage + ": " + e.getMessage)(request).map(Left(_))
            }
          })
      }
  }
}

/**
 * Defaults BodyParsers.
 */
object BodyParsers extends BodyParsers {
  private val logger = Logger(this.getClass)

  private val hcCache = Application.instanceCache[HttpConfiguration]

  private[play] class TakeUpTo(maxLength: Long) extends PushStage[ByteString, ByteString] {
    private var pushedBytes: Long = 0

    override def onPush(chunk: ByteString, ctx: Context[ByteString]): SyncDirective = {
      pushedBytes += chunk.size
      if (pushedBytes > maxLength) ctx.fail(new MaxLengthLimitAttained)
      else ctx.push(chunk)
    }
  }

  private[play] class MaxLengthLimitAttained extends RuntimeException(null, null, false, false)
}

/**
 * Signal a max content size exceeded
 */
case class MaxSizeExceeded(length: Long)
