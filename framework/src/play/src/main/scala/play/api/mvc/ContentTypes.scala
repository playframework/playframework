package play.api.mvc

import java.io._

import scala.xml._

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._
import play.api.libs.iteratee.Parsing._
import play.api.libs.Files.{ TemporaryFile }

import Results._
import MultipartFormData._

sealed trait AnyContent {

  def asUrlFormEncoded: Option[Map[String, Seq[String]]] = this match {
    case AnyContentAsUrlFormEncoded(data) => Some(data)
    case _ => None
  }

  def asText: Option[String] = this match {
    case AnyContentAsText(txt) => Some(txt)
    case _ => None
  }

  def asXml: Option[NodeSeq] = this match {
    case AnyContentAsXml(xml) => Some(xml)
    case _ => None
  }

  def asJson: Option[JsValue] = this match {
    case AnyContentAsJson(json) => Some(json)
    case _ => None
  }

  def asMultipartFormData: Option[MultipartFormData[TemporaryFile]] = this match {
    case AnyContentAsMultipartFormData(mfd) => Some(mfd)
    case _ => None
  }

  def asRaw: Option[RawBuffer] = this match {
    case AnyContentAsRaw(raw) => Some(raw)
    case _ => None
  }

}

case object AnyContentAsEmpty extends AnyContent
case class AnyContentAsText(txt: String) extends AnyContent
case class AnyContentAsUrlFormEncoded(data: Map[String, Seq[String]]) extends AnyContent
case class AnyContentAsRaw(raw: RawBuffer) extends AnyContent
case class AnyContentAsXml(xml: NodeSeq) extends AnyContent
case class AnyContentAsJson(json: JsValue) extends AnyContent
case class AnyContentAsMultipartFormData(mdf: MultipartFormData[TemporaryFile]) extends AnyContent

case class MultipartFormData[A](dataParts: Map[String, Seq[String]], files: Seq[FilePart[A]], badParts: Seq[BadPart], missingFileParts: Seq[MissingFilePart]) {
  def asUrlFormEncoded: Map[String, Seq[String]] = dataParts
  def file(key: String): Option[FilePart[A]] = files.find(_.key == key)
}

object MultipartFormData {
  trait Part
  case class DataPart(key: String, value: String) extends Part
  case class FilePart[A](key: String, filename: String, contentType: Option[String], ref: A) extends Part
  case class MissingFilePart(key: String) extends Part
  case class BadPart(headers: Map[String, String]) extends Part
  case class MaxDataPartSizeExcedeed(key: String) extends Part
}

case class RawBuffer(memoryThreshold: Int) {

  import play.api.libs.Files._
  import scala.collection.mutable._

  private var inMemory = new ArrayBuffer[Byte]
  private var backedByTemporaryFile: TemporaryFile = _
  private var outStream: OutputStream = _

  private[mvc] def push(chunk: Array[Byte]) {
    if (inMemory != null) {
      inMemory ++= chunk
      if (inMemory.size > memoryThreshold) {
        backToTemporaryFile()
      }
    } else {
      outStream.write(chunk)
    }
  }

  private[mvc] def close() {
    if (outStream != null) {
      outStream.close()
    }
  }

  private[mvc] def backToTemporaryFile() {
    backedByTemporaryFile = TemporaryFile("requestBody", "asRaw")
    outStream = new FileOutputStream(backedByTemporaryFile.file)
    outStream.write(inMemory.toArray)
    inMemory = null
  }

  def size: Long = {
    if (inMemory != null) inMemory.size else backedByTemporaryFile.file.length
  }

  def asBytes(maxLength: Int = memoryThreshold): Option[Array[Byte]] = {
    if (size <= maxLength) {
      if (inMemory != null) {
        Some(inMemory.toArray)
      } else {
        val inStream = new FileInputStream(backedByTemporaryFile.file)
        try {
          val buffer = new Array[Byte](size.toInt)
          inStream.read(buffer)
          Some(buffer)
        } finally {
          inStream.close()
        }
      }
    } else {
      None
    }
  }

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

trait BodyParsers {

  object parse {

    val UNLIMITED: Int = Integer.MAX_VALUE

    lazy val DEFAULT_MAX_TEXT_LENGTH = Play.maybeApplication.flatMap { app =>
      app.configuration.getInt("parsers.text.maxLength")
    }.getOrElse(1024 * 100)

    // -- Text parser

    def tolerantText(maxLength: Int): BodyParser[String] = BodyParser("text, maxLength=" + maxLength) { request =>
      Traversable.takeUpTo[Array[Byte]](maxLength)
        .transform(Iteratee.consume[Array[Byte]]().map(c => new String(c, request.charset.getOrElse("utf-8"))))
        .flatMap(Iteratee.eofOrElse(Results.EntityTooLarge))

    }

    def tolerantText: BodyParser[String] = tolerantText(DEFAULT_MAX_TEXT_LENGTH)

    def text(maxLength: Int): BodyParser[String] = when(
      _.contentType.exists(_ == "text/plain"),
      tolerantText(maxLength),
      request => Play.maybeApplication.map(_.global.onBadRequest(request, "Expecting text/plain body")).getOrElse(Results.BadRequest)
    )

    def text: BodyParser[String] = text(DEFAULT_MAX_TEXT_LENGTH)

    // -- Raw parser

    def raw(memoryThreshold: Int): BodyParser[RawBuffer] = BodyParser("raw, memoryThreshold=" + memoryThreshold) { request =>
      val buffer = RawBuffer(memoryThreshold)
      Iteratee.mapChunk_[Array[Byte]](bytes => buffer.push(bytes)).mapDone { _ =>
        buffer.close()
        Right(buffer)
      }
    }

    def raw: BodyParser[RawBuffer] = raw(memoryThreshold = 100 * 1024)

    // -- JSON parser

    def tolerantJson(maxLength: Int): BodyParser[JsValue] = BodyParser("json, maxLength=" + maxLength) { request =>
      Traversable.takeUpTo[Array[Byte]](maxLength).apply(Iteratee.consume[Array[Byte]]().map { bytes =>
        scala.util.control.Exception.allCatch[JsValue].either {
          Json.parse(new String(bytes, request.charset.getOrElse("utf-8")))
        }.left.map { e =>
          (Play.maybeApplication.map(_.global.onBadRequest(request, "Invalid Json")).getOrElse(Results.BadRequest), bytes)
        }
      }).flatMap(Iteratee.eofOrElse(Results.EntityTooLarge))
        .flatMap {
          case Left(b) => Done(Left(b), Empty)
          case Right(it) => it.flatMap {
            case Left((r, in)) => Done(Left(r), El(in))
            case Right(json) => Done(Right(json), Empty)
          }
        }
    }

    def tolerantJson: BodyParser[JsValue] = tolerantJson(DEFAULT_MAX_TEXT_LENGTH)

    def json(maxLength: Int): BodyParser[JsValue] = when(
      _.contentType.exists(m => m == "text/json" || m == "application/json"),
      tolerantJson(maxLength),
      request => Play.maybeApplication.map(_.global.onBadRequest(request, "Expecting text/json or application/json body")).getOrElse(Results.BadRequest)
    )

    def json: BodyParser[JsValue] = json(DEFAULT_MAX_TEXT_LENGTH)

    // -- Empty parser

    def empty: BodyParser[None.type] = BodyParser("empty") { request =>
      Done(Right(None), Empty)
    }

    // -- XML parser

    def tolerantXml(maxLength: Int): BodyParser[NodeSeq] = BodyParser("xml, maxLength=" + maxLength) { request =>
      Traversable.takeUpTo[Array[Byte]](maxLength).apply(Iteratee.consume[Array[Byte]]().mapDone { bytes =>
        scala.util.control.Exception.allCatch[NodeSeq].either {
          XML.loadString(new String(bytes, request.charset.getOrElse("utf-8")))
        }.left.map { e =>
          (Play.maybeApplication.map(_.global.onBadRequest(request, "Invalid XML")).getOrElse(Results.BadRequest), bytes)
        }
      }).flatMap(Iteratee.eofOrElse(Results.EntityTooLarge))
        .flatMap {
          case Left(b) => Done(Left(b), Empty)
          case Right(it) => it.flatMap {
            case Left((r, in)) => Done(Left(r), El(in))
            case Right(xml) => Done(Right(xml), Empty)
          }
        }
    }

    def tolerantXml: BodyParser[NodeSeq] = tolerantXml(DEFAULT_MAX_TEXT_LENGTH)

    def xml(maxLength: Int): BodyParser[NodeSeq] = when(
      _.contentType.exists(_.startsWith("text/xml")),
      tolerantXml(maxLength),
      request => Play.maybeApplication.map(_.global.onBadRequest(request, "Expecting text/xml body")).getOrElse(Results.BadRequest)
    )

    def xml: BodyParser[NodeSeq] = xml(DEFAULT_MAX_TEXT_LENGTH)

    // -- File parsers

    def file(to: File): BodyParser[File] = BodyParser("file, to=" + to) { request =>
      Iteratee.fold[Array[Byte], FileOutputStream](new FileOutputStream(to)) { (os, data) =>
        os.write(data)
        os
      }.mapDone { os =>
        os.close()
        Right(to)
      }
    }

    def temporaryFile: BodyParser[TemporaryFile] = BodyParser("temporaryFile") { request =>
      val tempFile = TemporaryFile("requestBody", "asTemporaryFile")
      file(tempFile.file)(request).mapDone(_ => Right(tempFile))
    }

    // -- UrlFormEncoded

    def tolerantUrlFormEncoded(maxLength: Int): BodyParser[Map[String, Seq[String]]] = BodyParser("urlFormEncoded, maxLength=" + maxLength) { request =>

      import play.core.parsers._
      import scala.collection.JavaConverters._

      Traversable.takeUpTo[Array[Byte]](maxLength).apply(Iteratee.consume[Array[Byte]]().mapDone { c =>
        scala.util.control.Exception.allCatch[Map[String, Seq[String]]].either {
          UrlFormEncodedParser.parse(new String(c, request.charset.getOrElse("utf-8")), request.charset.getOrElse("utf-8"))
        }.left.map { e =>
          Play.maybeApplication.map(_.global.onBadRequest(request, "Error parsing application/x-www-form-urlencoded")).getOrElse(Results.BadRequest)
        }
      }).flatMap(Iteratee.eofOrElse(Results.EntityTooLarge))
        .flatMap {
          case Left(b) => Done(Left(b), Empty)
          case Right(it) => it.flatMap {
            case Left(r) => Done(Left(r), Empty)
            case Right(urlEncoded) => Done(Right(urlEncoded), Empty)
          }
        }
    }

    def tolerantUrlFormEncoded: BodyParser[Map[String, Seq[String]]] = tolerantUrlFormEncoded(DEFAULT_MAX_TEXT_LENGTH)

    def urlFormEncoded(maxLength: Int): BodyParser[Map[String, Seq[String]]] = when(
      _.contentType.exists(_ == "application/x-www-form-urlencoded"),
      tolerantUrlFormEncoded(maxLength),
      request => Play.maybeApplication.map(_.global.onBadRequest(request, "Expecting application/x-www-form-urlencoded body")).getOrElse(Results.BadRequest)
    )

    def urlFormEncoded: BodyParser[Map[String, Seq[String]]] = urlFormEncoded(DEFAULT_MAX_TEXT_LENGTH)

    // -- Magic any content

    def anyContent: BodyParser[AnyContent] = BodyParser("anyContent") { request =>
      request.contentType match {
        case _ if request.method == "GET" || request.method == "HEAD" => {
          Logger("play").trace("Parsing AnyContent as empty")
          empty(request).map(_.right.map(_ => AnyContentAsEmpty))
        }
        case Some("text/plain") => {
          Logger("play").trace("Parsing AnyContent as text")
          text(request).map(_.right.map(s => AnyContentAsText(s)))
        }
        case Some("text/xml") => {
          Logger("play").trace("Parsing AnyContent as xml")
          xml(request).map(_.right.map(x => AnyContentAsXml(x)))
        }
        case Some("text/json") | Some("application/json") => {
          Logger("play").trace("Parsing AnyContent as json")
          json(request).map(_.right.map(j => AnyContentAsJson(j)))
        }
        case Some("application/x-www-form-urlencoded") => {
          Logger("play").trace("Parsing AnyContent as urlFormEncoded")
          urlFormEncoded(request).map(_.right.map(d => AnyContentAsUrlFormEncoded(d)))
        }
        case Some("multipart/form-data") => {
          Logger("play").trace("Parsing AnyContent as multipartFormData")
          multipartFormData(request).map(_.right.map(m => AnyContentAsMultipartFormData(m)))
        }
        case _ => {
          Logger("play").trace("Parsing AnyContent as raw")
          raw(request).map(_.right.map(r => AnyContentAsRaw(r)))
        }
      }
    }

    // -- Multipart

    def multipartFormData: BodyParser[MultipartFormData[TemporaryFile]] = multipartFormData(Multipart.handleFilePartAsTemporaryFile)

    def multipartFormData[A](filePartHandler: Multipart.PartHandler[FilePart[A]]): BodyParser[MultipartFormData[A]] = BodyParser("multipartFormData") { request =>
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

        val maybeBoundary = request.headers.get(play.api.http.HeaderNames.CONTENT_TYPE).filter(ct => ct.trim.startsWith("multipart/form-data")).flatMap { mpCt =>
          mpCt.trim.split("boundary=").tail.headOption.map(b => ("\r\n--" + b).getBytes("utf-8"))
        }

        maybeBoundary.map { boundary =>

          BodyParser { request =>

            val CRLF = "\r\n".getBytes
            val CRLFCRLF = CRLF ++ CRLF

            val takeUpToBoundary = Enumeratee.takeWhile[MatchInfo[Array[Byte]]](!_.isMatch)

            val maxHeaderBuffer = Traversable.takeUpTo[Array[Byte]](4 * 1024) transform Iteratee.consume[Array[Byte]]()

            val collectHeaders = maxHeaderBuffer.flatMap { buffer =>
              val (headerBytes, rest) = Option(buffer.drop(2)).map(b => b.splitAt(b.indexOfSlice(CRLFCRLF))).get

              val headerString = new String(headerBytes)
              val headers = headerString.lines.map { header =>
                val key :: value = header.trim.split(":").toList
                (key.trim.toLowerCase, value.mkString.trim)
              }.toMap

              val left = rest.drop(CRLFCRLF.length)

              Cont(in => Done(headers, in match {
                case Input.El(e) => Input.El(left ++ e)
                case Input.EOF => Input.El(left)
                case Input.Empty => Input.El(left)
              }))
            }

            val readPart = collectHeaders.flatMap(partHandler)

            val handlePart = Enumeratee.map[MatchInfo[Array[Byte]]](_.content).transform(readPart)

            Traversable.take[Array[Byte]](boundary.size - 2).transform(Iteratee.consume()).flatMap { firstBoundary =>

              Parsing.search(boundary) transform Iteratee.repeat {

                takeUpToBoundary.transform(handlePart).flatMap { part =>
                  Enumeratee.take(1)(Iteratee.ignore[MatchInfo[Array[Byte]]]).mapDone(_ => part)
                }

              }.map(parts => Right(parts.dropRight(1)))

            }

          }

        }.getOrElse(parse.error(Play.maybeApplication.map(_.global.onBadRequest(request, "Missing boundary header")).getOrElse(Results.BadRequest)))

      }

      type PartHandler[A] = PartialFunction[Map[String, String], Iteratee[Array[Byte], A]]

      def handleFilePartAsTemporaryFile: PartHandler[FilePart[TemporaryFile]] = {
        handleFilePart {
          case FileInfo(partName, filename, contentType) =>
            val tempFile = TemporaryFile("multipartBody", "asTemporaryFile")
            Iteratee.fold[Array[Byte], FileOutputStream](new java.io.FileOutputStream(tempFile.file)) { (os, data) =>
              os.write(data)
              os
            }.mapDone { os =>
              os.close()
              tempFile
            }
        }
      }

      case class FileInfo(partName: String, fileName: String, contentType: Option[String])

      object FileInfoMatcher {

        def unapply(headers: Map[String, String]): Option[(String, String, Option[String])] = {

          val keyValue = """^([a-zA-Z_0-9]+)="(.*)"$""".r

          for {
            value <- headers.get("content-disposition")

            val values = value.split(";").map(_.trim).map {
              case keyValue(key, value) => (key.trim, value.trim)
              case key => (key.trim, "")
            }.toMap

            _ <- values.get("form-data");

            partName <- values.get("name");

            fileName <- values.get("filename");

            val contentType = headers.get("content-type")

          } yield ((partName, fileName, contentType))
        }
      }

      def handleFilePart[A](handler: FileInfo => Iteratee[Array[Byte], A]): PartHandler[FilePart[A]] = {
        case FileInfoMatcher(partName, fileName, contentType) =>
          val safeFileName = fileName.split('\\').takeRight(1).mkString
          handler(FileInfo(partName, safeFileName, contentType)).map(a => FilePart(partName, safeFileName, contentType, a))
      }

      object PartInfoMatcher {

        def unapply(headers: Map[String, String]): Option[String] = {

          val keyValue = """^([a-zA-Z_0-9]+)="(.*)"$""".r

          for {
            value <- headers.get("content-disposition")

            val values = value.split(";").map(_.trim).map {
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
          Traversable.takeUpTo[Array[Byte]](DEFAULT_MAX_TEXT_LENGTH)
            .transform(Iteratee.consume[Array[Byte]]().map(bytes => DataPart(partName, new String(bytes, "utf-8"))))
            .flatMap { data =>
              Cont({
                case Input.El(_) => Done(MaxDataPartSizeExcedeed(partName), Input.Empty)
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

    def maxLength[A](maxLength: Int, parser: BodyParser[A]): BodyParser[Either[MaxSizeExceeded, A]] = BodyParser("maxLength=" + maxLength + ", wrapping=" + parser.toString) { request =>
      Traversable.takeUpTo[Array[Byte]](maxLength).transform(parser(request)).flatMap(Iteratee.eofOrElse(MaxSizeExceeded(maxLength))).map {
        case Right(Right(result)) => Right(Right(result))
        case Right(Left(badRequest)) => Left(badRequest)
        case Left(maxSizeExceeded) => Right(Left(maxSizeExceeded))
      }
    }

    def error[A](result: Result): BodyParser[A] = BodyParser("error, result=" + result) { request =>
      Done(Left(result), Empty)
    }

    def using[A](f: RequestHeader => BodyParser[A]) = BodyParser { request =>
      f(request)(request)
    }

    def when[A](predicate: RequestHeader => Boolean, parser: BodyParser[A], badResult: RequestHeader => Result): BodyParser[A] = {
      BodyParser("conditional, wrapping=" + parser.toString) { request =>
        if (predicate(request)) {
          parser(request)
        } else {
          Done(Left(badResult(request)), Empty)
        }
      }
    }

  }

}

object BodyParsers extends BodyParsers

case class MaxSizeExceeded(length: Int)
