package play.api.mvc

import scala.language.reflectiveCalls
import java.io._
import scala.concurrent.Future
import scala.xml._
import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._
import play.api.libs.iteratee.Parsing._
import play.api.libs.Files.TemporaryFile
import MultipartFormData._
import scala.collection.mutable.ListBuffer
import scalax.io.Resource
import java.util.Locale
import scala.util.control.NonFatal

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
  def asBytes(maxLength: Int = memoryThreshold): Option[Array[Byte]] = {
    if (size <= maxLength) {

      if (inMemory != null) {

        val buffer = new Array[Byte](inMemorySize)
        inMemory.reverse.foldLeft(0) { (position, chunk) =>
          System.arraycopy(chunk, 0, buffer, position, Math.min(chunk.length, buffer.length - position))
          chunk.length + position
        }
        Some(buffer)

      } else {
        Some(Resource.fromFile(backedByTemporaryFile.file).byteArray)
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

  /**
   * Default body parsers.
   */
  object parse {

    /**
     * Unlimited size.
     */
    val UNLIMITED: Int = Integer.MAX_VALUE

    private val ApplicationXmlMatcher = """application/.*\+xml.*""".r

    /**
     * Default max length allowed for text based body.
     *
     * You can configure it in application.conf:
     *
     * {{{
     * parsers.text.maxLength = 512k
     * }}}
     */
    def DEFAULT_MAX_TEXT_LENGTH: Int = Play.maybeApplication.flatMap { app =>
      app.configuration.getBytes("parsers.text.maxLength").map(_.toInt)
    }.getOrElse(1024 * 100)

    // -- Text parser

    /**
     * Parse the body as text without checking the Content-Type.
     *
     * @param maxLength Max length allowed or returns EntityTooLarge HTTP response.
     */
    def tolerantText(maxLength: Int): BodyParser[String] = BodyParser("text, maxLength=" + maxLength) { request =>
      // Encoding notes: RFC-2616 section 3.7.1 mandates ISO-8859-1 as the default charset if none is specified.

      import Execution.Implicits.trampoline
      Traversable.takeUpTo[Array[Byte]](maxLength)
        .transform(Iteratee.consume[Array[Byte]]().map(c => new String(c, request.charset.getOrElse("ISO-8859-1"))))
        .flatMap(Iteratee.eofOrElse(Results.EntityTooLarge))
    }

    /**
     * Parse the body as text without checking the Content-Type.
     */
    def tolerantText: BodyParser[String] = tolerantText(DEFAULT_MAX_TEXT_LENGTH)

    /**
     * Parse the body as text if the Content-Type is text/plain.
     *
     * @param maxLength Max length allowed or returns EntityTooLarge HTTP response.
     */
    def text(maxLength: Int): BodyParser[String] = when(
      _.contentType.exists(_.equalsIgnoreCase("text/plain")),
      tolerantText(maxLength),
      createBadResult("Expecting text/plain body")
    )

    /**
     * Parse the body as text if the Content-Type is text/plain.
     */
    def text: BodyParser[String] = text(DEFAULT_MAX_TEXT_LENGTH)

    // -- Raw parser

    /**
     * Store the body content in a RawBuffer.
     *
     * @param memoryThreshold If the content size is bigger than this limit, the content is stored as file.
     */
    def raw(memoryThreshold: Int): BodyParser[RawBuffer] = BodyParser("raw, memoryThreshold=" + memoryThreshold) { request =>
      import play.core.Execution.Implicits.internalContext // Cannot run on same thread as may need to write to a file
      val buffer = RawBuffer(memoryThreshold)
      Iteratee.foreach[Array[Byte]](bytes => buffer.push(bytes)).map { _ =>
        buffer.close()
        Right(buffer)
      }
    }

    /**
     * Store the body content in a RawBuffer.
     */
    def raw: BodyParser[RawBuffer] = raw(memoryThreshold = 100 * 1024)

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
    def tolerantJson: BodyParser[JsValue] = tolerantJson(DEFAULT_MAX_TEXT_LENGTH)

    /**
     * Parse the body as Json if the Content-Type is text/json or application/json.
     *
     * @param maxLength Max length allowed or returns EntityTooLarge HTTP response.
     */
    def json(maxLength: Int): BodyParser[JsValue] = when(
      _.contentType.exists(m => m.equalsIgnoreCase("text/json") || m.equalsIgnoreCase("application/json")),
      tolerantJson(maxLength),
      createBadResult("Expecting text/json or application/json body")
    )

    /**
     * Parse the body as Json if the Content-Type is text/json or application/json.
     */
    def json: BodyParser[JsValue] = json(DEFAULT_MAX_TEXT_LENGTH)

    // -- Empty parser

    /**
     * Don't parse the body content.
     */
    def empty: BodyParser[Option[Any]] = BodyParser("empty") { request =>
      Done(Right(None), Empty)
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
    def tolerantXml: BodyParser[NodeSeq] = tolerantXml(DEFAULT_MAX_TEXT_LENGTH)

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
      createBadResult("Expecting xml body")
    )

    /**
     * Parse the body as Xml if the Content-Type is application/xml, text/xml or application/XXX+xml.
     */
    def xml: BodyParser[NodeSeq] = xml(DEFAULT_MAX_TEXT_LENGTH)

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
        FormUrlEncodedParser.parse(new String(bytes, request.charset.getOrElse("utf-8")), request.charset.getOrElse("utf-8"))
      }

    /**
     * Parse the body as form url encoded without checking the Content-Type.
     */
    def tolerantFormUrlEncoded: BodyParser[Map[String, Seq[String]]] = tolerantFormUrlEncoded(DEFAULT_MAX_TEXT_LENGTH)

    /**
     * Parse the body as form url encoded if the Content-Type is application/x-www-form-urlencoded.
     *
     * @param maxLength Max length allowed or returns EntityTooLarge HTTP response.
     */
    def urlFormEncoded(maxLength: Int): BodyParser[Map[String, Seq[String]]] = when(
      _.contentType.exists(_.equalsIgnoreCase("application/x-www-form-urlencoded")),
      tolerantFormUrlEncoded(maxLength),
      createBadResult("Expecting application/x-www-form-urlencoded body")
    )

    /**
     * Parse the body as form url encoded if the Content-Type is application/x-www-form-urlencoded.
     */
    def urlFormEncoded: BodyParser[Map[String, Seq[String]]] = urlFormEncoded(DEFAULT_MAX_TEXT_LENGTH)

    // -- Magic any content

    /**
     * Guess the body content by checking the Content-Type header.
     */
    def anyContent: BodyParser[AnyContent] = BodyParser("anyContent") { request =>
      import play.api.libs.iteratee.Execution.Implicits.trampoline
      request.contentType.map(_.toLowerCase(Locale.ENGLISH)) match {
        case _ if request.method == "GET" || request.method == "HEAD" => {
          Play.logger.trace("Parsing AnyContent as empty")
          empty(request).map(_.right.map(_ => AnyContentAsEmpty))
        }
        case Some("text/plain") => {
          Play.logger.trace("Parsing AnyContent as text")
          text(request).map(_.right.map(s => AnyContentAsText(s)))
        }
        case Some("text/xml") | Some("application/xml") | Some(ApplicationXmlMatcher()) => {
          Play.logger.trace("Parsing AnyContent as xml")
          xml(request).map(_.right.map(x => AnyContentAsXml(x)))
        }
        case Some("text/json") | Some("application/json") => {
          Play.logger.trace("Parsing AnyContent as json")
          json(request).map(_.right.map(j => AnyContentAsJson(j)))
        }
        case Some("application/x-www-form-urlencoded") => {
          Play.logger.trace("Parsing AnyContent as urlFormEncoded")
          urlFormEncoded(request).map(_.right.map(d => AnyContentAsFormUrlEncoded(d)))
        }
        case Some("multipart/form-data") => {
          Play.logger.trace("Parsing AnyContent as multipartFormData")
          multipartFormData(request).map(_.right.map(m => AnyContentAsMultipartFormData(m)))
        }
        case _ => {
          Play.logger.trace("Parsing AnyContent as raw")
          raw(request).map(_.right.map(r => AnyContentAsRaw(r)))
        }
      }
    }

    // -- Multipart

    /**
     * Parse the content as multipart/form-data
     */
    def multipartFormData: BodyParser[MultipartFormData[TemporaryFile]] = multipartFormData(Multipart.handleFilePartAsTemporaryFile)

    /**
     * Parse the content as multipart/form-data
     *
     * @param filePartHandler Handles file parts.
     */
    def multipartFormData[A](filePartHandler: Multipart.PartHandler[FilePart[A]]): BodyParser[MultipartFormData[A]] = BodyParser("multipartFormData") { request =>
      import play.api.libs.iteratee.Execution.Implicits.trampoline
      val handler: Multipart.PartHandler[Either[Part, FilePart[A]]] =
        Multipart.handleDataPart.andThen(_.map(Left(_)))
          .orElse({ case Multipart.FileInfoMatcher(partName, fileName, _) if fileName.trim.isEmpty => Done(Left(MissingFilePart(partName)), Input.Empty) }: Multipart.PartHandler[Either[Part, FilePart[A]]])
          .orElse(filePartHandler.andThen(_.map(Right(_))))
          .orElse { case headers => Done(Left(BadPart(headers)), Input.Empty) }

      Multipart.multipartParser(handler)(request).map { errorOrParts =>
        errorOrParts.right.map { parts =>
          val data = parts.collect { case Left(DataPart(key, value)) => (key, value) }.groupBy(_._1).mapValues(_.map(_._2))
          val az = parts.collect { case Right(a) => a }
          val bad = parts.collect { case Left(b @ BadPart(_)) => b }
          val missing = parts.collect { case Left(missing @ MissingFilePart(_)) => missing }
          MultipartFormData(data, az, bad, missing)

        }
      }
    }

    object Multipart {

      def multipartParser[A](partHandler: Map[String, String] => Iteratee[Array[Byte], A]): BodyParser[Seq[A]] = parse.using { request =>

        val maybeBoundary = for {
          mt <- request.mediaType
          (_, value) <- mt.parameters.find(_._1.equalsIgnoreCase("boundary"))
          boundary <- value
        } yield ("\r\n--" + boundary).getBytes("utf-8")

        maybeBoundary.map { boundary =>

          BodyParser { request =>

            import play.api.libs.iteratee.Execution.Implicits.trampoline

            val CRLF = "\r\n".getBytes
            val CRLFCRLF = CRLF ++ CRLF

            val takeUpToBoundary = Enumeratee.takeWhile[MatchInfo[Array[Byte]]](!_.isMatch)

            val maxHeaderBuffer = Traversable.takeUpTo[Array[Byte]](4 * 1024) transform Iteratee.consume[Array[Byte]]()

            val collectHeaders = maxHeaderBuffer.map { buffer =>
              val (headerBytes, rest) = Option(buffer.drop(2)).map(b => b.splitAt(b.indexOfSlice(CRLFCRLF))).get

              val headerString = new String(headerBytes, "utf-8")
              val headers = headerString.lines.map { header =>
                val key :: value = header.trim.split(":").toList
                (key.trim.toLowerCase, value.mkString.trim)
              }.toMap

              val left = rest.drop(CRLFCRLF.length)
              (headers, left)
            }

            val readPart = collectHeaders.flatMap { case (headers, left) => Iteratee.flatten(partHandler(headers).feed(Input.El(left))) }

            val handlePart = Enumeratee.map[MatchInfo[Array[Byte]]](_.content).transform(readPart)

            Traversable.take[Array[Byte]](boundary.size - 2).transform(Iteratee.consume()).flatMap { firstBoundary =>

              Parsing.search(boundary) transform Iteratee.repeat {

                takeUpToBoundary.transform(handlePart).flatMap { part =>
                  Enumeratee.take(1)(Iteratee.ignore[MatchInfo[Array[Byte]]]).map(_ => part)
                }

              }.map(parts => Right(parts.dropRight(1)))

            }

          }

        }.getOrElse(parse.error(createBadResult("Missing boundary header")(request)))

      }

      type PartHandler[A] = PartialFunction[Map[String, String], Iteratee[Array[Byte], A]]

      def handleFilePartAsTemporaryFile: PartHandler[FilePart[TemporaryFile]] = {
        handleFilePart {
          case FileInfo(partName, filename, contentType) =>
            val tempFile = TemporaryFile("multipartBody", "asTemporaryFile")
            import play.core.Execution.Implicits.internalContext
            Iteratee.fold[Array[Byte], FileOutputStream](new java.io.FileOutputStream(tempFile.file)) { (os, data) =>
              os.write(data)
              os
            }.map { os =>
              os.close()
              tempFile
            }
        }
      }

      case class FileInfo(partName: String, fileName: String, contentType: Option[String])

      object FileInfoMatcher {

        private def split(str: String) = {
          var buffer = new StringBuffer
          var escape: Boolean = false
          var quote: Boolean = false
          val result = new ListBuffer[String]

          def addPart() = {
            result += buffer.toString().trim
            buffer = new StringBuffer
          }

          str foreach { c =>
            c match {
              case '\\' =>
                buffer.append(c)
                escape = true
              case '"' =>
                buffer.append(c)
                if (!escape)
                  quote = !quote
                escape = false
              case ';' =>
                if (!quote) {
                  addPart
                } else {
                  buffer.append(c)
                }
                escape = false
              case _ =>
                buffer.append(c)
                escape = false
            }
          }

          addPart
          result.toList
        }

        def unapply(headers: Map[String, String]): Option[(String, String, Option[String])] = {

          val keyValue = """^([a-zA-Z_0-9]+)="(.*)"$""".r

          for {
            value <- headers.get("content-disposition")

            values = split(value).map(_.trim).map {
              // unescape escaped quotes 
              case keyValue(key, value) => (key.trim, value.trim.replaceAll("""\\"""", "\""))
              case key => (key.trim, "")
            }.toMap

            _ <- values.get("form-data");

            partName <- values.get("name");

            fileName <- values.get("filename");

            contentType = headers.get("content-type")

          } yield ((partName, fileName, contentType))
        }
      }

      def handleFilePart[A](handler: FileInfo => Iteratee[Array[Byte], A]): PartHandler[FilePart[A]] = {
        case FileInfoMatcher(partName, fileName, contentType) =>
          val safeFileName = fileName.split('\\').takeRight(1).mkString
          import play.api.libs.iteratee.Execution.Implicits.trampoline
          handler(FileInfo(partName, safeFileName, contentType)).map(a => FilePart(partName, safeFileName, contentType, a))
      }

      object PartInfoMatcher {

        def unapply(headers: Map[String, String]): Option[String] = {

          val keyValue = """^([a-zA-Z_0-9]+)="(.*)"$""".r

          for {
            value <- headers.get("content-disposition")

            values = value.split(";").map(_.trim).map {
              case keyValue(key, value) => (key.trim, value.trim)
              case key => (key.trim, "")
            }.toMap

            _ <- values.get("form-data");

            partName <- values.get("name")

          } yield (partName)
        }
      }

      def handleDataPart: PartHandler[Part] = {
        case headers @ PartInfoMatcher(partName) if !FileInfoMatcher.unapply(headers).isDefined =>
          import play.api.libs.iteratee.Execution.Implicits.trampoline
          Traversable.takeUpTo[Array[Byte]](DEFAULT_MAX_TEXT_LENGTH)
            .transform(Iteratee.consume[Array[Byte]]().map(bytes => DataPart(partName, new String(bytes, "utf-8"))))
            .flatMap { data =>
              Cont({
                case Input.El(_) => Done(MaxDataPartSizeExceeded(partName), Input.Empty)
                case in => Done(data, in)
              })
            }
      }

      def handlePart(fileHandler: PartHandler[FilePart[File]]): PartHandler[Part] = {
        handleDataPart
          .orElse({ case FileInfoMatcher(partName, fileName, _) if fileName.trim.isEmpty => Done(MissingFilePart(partName), Input.Empty) }: PartHandler[Part])
          .orElse(fileHandler)
          .orElse({ case headers => Done(BadPart(headers), Input.Empty) })
      }

    }

    // -- Parsing utilities

    /**
     * Wrap an existing BodyParser with a maxLength constraints.
     *
     * @param maxLength The max length allowed
     * @param parser The BodyParser to wrap
     */
    def maxLength[A](maxLength: Int, parser: BodyParser[A]): BodyParser[Either[MaxSizeExceeded, A]] = BodyParser("maxLength=" + maxLength + ", wrapping=" + parser.toString) { request =>
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
    def error[A](result: Future[SimpleResult]): BodyParser[A] = BodyParser("error, result=" + result) { request =>
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
    def when[A](predicate: RequestHeader => Boolean, parser: BodyParser[A], badResult: RequestHeader => Future[SimpleResult]): BodyParser[A] = {
      BodyParser("conditional, wrapping=" + parser.toString) { request =>
        if (predicate(request)) {
          parser(request)
        } else {
          import play.api.libs.iteratee.Execution.Implicits.trampoline
          Iteratee.flatten(badResult(request).map(result => Done(Left(result), Empty)))
        }
      }
    }

    private def createBadResult(msg: String): RequestHeader => Future[SimpleResult] = { request =>
      Play.maybeApplication.map(_.global.onBadRequest(request, msg))
        .getOrElse(Future.successful(Results.BadRequest))
    }

    private def tolerantBodyParser[A](name: String, maxLength: Int, errorMessage: String)(parser: (RequestHeader, Array[Byte]) => A): BodyParser[A] =
      BodyParser(name + ", maxLength=" + maxLength) { request =>
        import play.api.libs.iteratee.Execution.Implicits.trampoline
        import scala.util.control.Exception._

        val bodyParser: Iteratee[Array[Byte], Either[SimpleResult, Either[Future[SimpleResult], A]]] =
          Traversable.takeUpTo[Array[Byte]](maxLength).transform(
            Iteratee.consume[Array[Byte]]().map { bytes =>
              allCatch[A].either {
                parser(request, bytes)
              }.left.map {
                case NonFatal(e) =>
                  Play.logger.debug(errorMessage, e)
                  createBadResult(errorMessage)(request)
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
object BodyParsers extends BodyParsers

/**
 * Signal a max content size exceeded
 */
case class MaxSizeExceeded(length: Int)
