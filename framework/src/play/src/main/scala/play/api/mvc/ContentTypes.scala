/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

import play.api.data.{ FormUtils, Form }
import play.core.parsers.Multipart

import scala.language.reflectiveCalls
import java.io._
import scala.concurrent.Future
import scala.xml._
import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._
import play.api.libs.Files.TemporaryFile
import MultipartFormData._
import java.util.Locale
import scala.util.control.NonFatal
import play.api.http.{ ParserConfiguration, HttpConfiguration, HttpVerbs }
import play.utils.PlayIO
import play.api.http.Status._

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
case class MultipartFormData[A](dataParts: Map[String, Seq[String]], files: Seq[FilePart[A]], badParts: Seq[BadPart], missingFileParts: Seq[MissingFilePart]) {

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
   */
  sealed trait Part

  /**
   * A data part.
   */
  case class DataPart(key: String, value: String) extends Part

  /**
   * A file part.
   */
  case class FilePart[A](key: String, filename: String, contentType: Option[String], ref: A) extends Part

  /**
   * A file part with no content provided.
   */
  case class MissingFilePart(key: String) extends Part

  /**
   * A part that has not been properly parsed.
   */
  case class BadPart(headers: Map[String, String]) extends Part

  /**
   * A data part that has excedeed the max size allowed.
   */
  case class MaxDataPartSizeExceeded(key: String) extends Part
}

/**
 * Handle the request body a raw bytes data.
 *
 * @param memoryThreshold If the content size is bigger than this limit, the content is stored as file.
 */
case class RawBuffer(memoryThreshold: Int, initialData: Array[Byte] = Array.empty[Byte]) {

  import play.api.libs.Files._
  import scala.collection.mutable._

  @volatile private var inMemory: List[Array[Byte]] = if (initialData.length == 0) Nil else List(initialData)
  @volatile private var inMemorySize = initialData.length
  @volatile private var backedByTemporaryFile: TemporaryFile = _
  @volatile private var outStream: OutputStream = _

  private[play] def push(chunk: Array[Byte]) {
    if (inMemory != null) {
      if (chunk.length + inMemorySize > memoryThreshold) {
        backToTemporaryFile()
        outStream.write(chunk)
      } else {
        inMemory = chunk :: inMemory
        inMemorySize += chunk.length
      }
    } else {
      outStream.write(chunk)
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
    inMemory.reverse.foreach { chunk =>
      outStream.write(chunk)
    }
    inMemory = null
  }

  /**
   * Buffer size.
   */
  def size: Long = {
    if (inMemory != null) inMemorySize else backedByTemporaryFile.file.length
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
  def asBytes(maxLength: Long = memoryThreshold): Option[Array[Byte]] = {
    if (size <= maxLength) {
      if (inMemory != null) {
        val buffer = new Array[Byte](inMemorySize)
        inMemory.reverse.foldLeft(0) { (position, chunk) =>
          System.arraycopy(chunk, 0, buffer, position, Math.min(chunk.length, buffer.length - position))
          chunk.length + position
        }
        Some(buffer)
      } else {
        Some(PlayIO.readFile(backedByTemporaryFile.file))
      }
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

    private val ApplicationXmlMatcher = """application/.*\+xml.*""".r

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
     * Default max length allowed for text based body.
     *
     * You can configure it in application.conf:
     *
     * {{{
     * parsers.disk.maxLength = 512k
     * }}}
     */
    def DefaultMaxDiskLength: Long = config.maxDiskBuffer

    // -- Text parser

    /**
     * Parse the body as text without checking the Content-Type.
     *
     * @param maxLength Max length allowed or returns EntityTooLarge HTTP response.
     */
    def tolerantText(maxLength: Long): BodyParser[String] = BodyParser("text, maxLength=" + maxLength) { request =>
      // Encoding notes: RFC-2616 section 3.7.1 mandates ISO-8859-1 as the default charset if none is specified.

      import Execution.Implicits.trampoline
      Traversable.takeUpTo[Array[Byte]](maxLength)
        .transform(Iteratee.consume[Array[Byte]]().map(c => new String(c, request.charset.getOrElse("ISO-8859-1"))))
        .flatMap(Iteratee.eofOrElse(Results.EntityTooLarge))
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
        import play.core.Execution.Implicits.internalContext // Cannot run on same thread as may need to write to a file
        val buffer = RawBuffer(memoryThreshold)
        Traversable.takeUpTo[Array[Byte]](maxLength).transform(
          Iteratee.foreach[Array[Byte]](bytes => buffer.push(bytes)).map { _ =>
            buffer.close()
            buffer
          }
        ).flatMap(Iteratee.eofOrElse(Results.EntityTooLarge))
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
        Json.parse(bytes)
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
        json(request) mapM {
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
      Done(Right(body), Empty)
    }

    // -- XML parser

    /**
     * Parse the body as Xml without checking the Content-Type.
     *
     * @param maxLength Max length allowed or returns EntityTooLarge HTTP response.
     */
    def tolerantXml(maxLength: Int): BodyParser[NodeSeq] =
      tolerantBodyParser[NodeSeq]("xml", maxLength, "Invalid XML") { (request, bytes) =>
        val inputSource = new InputSource(new ByteArrayInputStream(bytes))

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
      import play.core.Execution.Implicits.internalContext
      Iteratee.fold[Array[Byte], FileOutputStream](new FileOutputStream(to)) { (os, data) =>
        os.write(data)
        os
      }.map { os =>
        os.close()
        Right(to)
      }
    }

    /**
     * Store the body content into a temporary file.
     */
    def temporaryFile: BodyParser[TemporaryFile] = BodyParser("temporaryFile") { request =>
      Iteratee.flatten(Future {
        val tempFile = TemporaryFile("requestBody", "asTemporaryFile")
        file(tempFile.file)(request).map(_ => Right(tempFile))(play.api.libs.iteratee.Execution.trampoline)
      }(play.core.Execution.internalContext))
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
        FormUrlEncodedParser.parse(new String(bytes, request.charset.getOrElse("utf-8")),
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
        case Some("text/plain") => {
          logger.trace("Parsing AnyContent as text")
          text(maxLengthOrDefault)(request).map(_.right.map(s => AnyContentAsText(s)))
        }
        case Some("text/xml") | Some("application/xml") | Some(ApplicationXmlMatcher()) => {
          logger.trace("Parsing AnyContent as xml")
          xml(maxLengthOrDefault)(request).map(_.right.map(x => AnyContentAsXml(x)))
        }
        case Some("text/json") | Some("application/json") => {
          logger.trace("Parsing AnyContent as json")
          json(maxLengthOrDefault)(request).map(_.right.map(j => AnyContentAsJson(j)))
        }
        case Some("application/x-www-form-urlencoded") => {
          logger.trace("Parsing AnyContent as urlFormEncoded")
          urlFormEncoded(maxLengthOrDefault)(request).map(_.right.map(d => AnyContentAsFormUrlEncoded(d)))
        }
        case Some("multipart/form-data") => {
          logger.trace("Parsing AnyContent as multipartFormData")
          multipartFormData(Multipart.handleFilePartAsTemporaryFile, maxLengthOrDefaultLarge)(request)
            .map(_.right.map(m => AnyContentAsMultipartFormData(m)))
        }
        case _ => {
          logger.trace("Parsing AnyContent as raw")
          raw(DefaultMaxTextLength, maxLengthOrDefaultLarge)(request).map(_.right.map(r => AnyContentAsRaw(r)))
        }
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
    def multipartFormData[A](filePartHandler: Multipart.PartHandler[FilePart[A]], maxLength: Long = DefaultMaxDiskLength): BodyParser[MultipartFormData[A]] = {
      BodyParser("multipartFormData") { request =>
        import play.api.libs.iteratee.Execution.Implicits.trampoline

        val parser = Traversable.takeUpTo[Array[Byte]](maxLength).transform(
          Multipart.multipartParser(DefaultMaxTextLength, filePartHandler)(request)
        ).flatMap(Iteratee.eofOrElse(Results.EntityTooLarge))

        parser.map {
          case Left(tooLarge) => Left(tooLarge)
          case Right(Left(badResult)) => Left(badResult)
          case Right(Right(body)) => Right(body)
        }
      }
    }

    // -- Parsing utilities

    /**
     * Wrap an existing BodyParser with a maxLength constraints.
     *
     * @param maxLength The max length allowed
     * @param parser The BodyParser to wrap
     */
    def maxLength[A](maxLength: Long, parser: BodyParser[A]): BodyParser[Either[MaxSizeExceeded, A]] = BodyParser("maxLength=" + maxLength + ", wrapping=" + parser.toString) { request =>
      import play.api.libs.iteratee.Execution.Implicits.trampoline
      Traversable.takeUpTo[Array[Byte]](maxLength).transform(parser(request)).flatMap(Iteratee.eofOrElse(MaxSizeExceeded(maxLength))).map {
        case Right(Right(result)) => Right(Right(result))
        case Right(Left(badRequest)) => Left(badRequest)
        case Left(maxSizeExceeded) => Right(Left(maxSizeExceeded))
      }
    }

    /**
     * A body parser that always returns an error.
     */
    def error[A](result: Future[Result]): BodyParser[A] = BodyParser("error") { request =>
      import play.api.libs.iteratee.Execution.Implicits.trampoline
      Iteratee.flatten(result.map(r => Done(Left(r), Empty)))
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
          Iteratee.flatten(badResult(request).map(result => Done(Left(result), Empty)))
        }
      }
    }

    private def createBadResult(msg: String, statusCode: Int = BAD_REQUEST): RequestHeader => Future[Result] = { request =>
      Play.maybeApplication.map(_.errorHandler.onClientError(request, statusCode, msg))
        .getOrElse(Future.successful(Results.BadRequest))
    }

    private def tolerantBodyParser[A](name: String, maxLength: Long, errorMessage: String)(parser: (RequestHeader, Array[Byte]) => A): BodyParser[A] =
      BodyParser(name + ", maxLength=" + maxLength) { request =>
        import play.api.libs.iteratee.Execution.Implicits.trampoline
        import scala.util.control.Exception._

        val bodyParser: Iteratee[Array[Byte], Either[Result, Either[Future[Result], A]]] =
          Traversable.takeUpTo[Array[Byte]](maxLength).transform(
            Iteratee.consume[Array[Byte]]().map { bytes =>
              allCatch[A].either {
                parser(request, bytes)
              }.left.map {
                case NonFatal(e) =>
                  logger.debug(errorMessage, e)
                  createBadResult(errorMessage + ": " + e.getMessage)(request)
                case t => throw t
              }
            }
          ).flatMap(Iteratee.eofOrElse(Results.EntityTooLarge))

        bodyParser.mapM {
          case Left(tooLarge) => Future.successful(Left(tooLarge))
          case Right(Left(badResult)) => badResult.map(Left.apply)
          case Right(Right(body)) => Future.successful(Right(body))
        }
      }
  }
}

/**
 * Defaults BodyParsers.
 */
object BodyParsers extends BodyParsers {
  private val logger = Logger(this.getClass)

  private val hcCache = Application.instanceCache[HttpConfiguration]
}

/**
 * Signal a max content size exceeded
 */
case class MaxSizeExceeded(length: Long)
