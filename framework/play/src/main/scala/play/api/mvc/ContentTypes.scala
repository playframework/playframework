package play.api.mvc

import java.io._

import scala.xml._

import play.api.json._
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

  def asMultipartFormData: Option[MultipartFormData[FilePart[(String, TemporaryFile)]]] = this match {
    case AnyContentAsMultipartFormData(mfd) => Some(mfd)
    case _ => None
  }

  def asRaw: Option[Array[Byte]] = this match {
    case AnyContentAsRaw(raw) => Some(raw)
    case _ => None
  }

}

case object AnyContentAsEmpty extends AnyContent
case class AnyContentAsText(txt: String) extends AnyContent
case class AnyContentAsUrlFormEncoded(data: Map[String, Seq[String]]) extends AnyContent
case class AnyContentAsRaw(raw: Array[Byte]) extends AnyContent
case class AnyContentAsXml(xml: NodeSeq) extends AnyContent
case class AnyContentAsJson(json: JsValue) extends AnyContent
case class AnyContentAsMultipartFormData(mdf: MultipartFormData[FilePart[(String, TemporaryFile)]]) extends AnyContent

case class MultipartFormData[A](dataParts: Map[String, Seq[String]], other: Seq[A], badParts: Seq[BadPart]) {

  def asUrlFormEncoded: Map[String, Seq[String]] = dataParts

  def files[Ref](implicit files: A <:< FilePart[Ref]): Seq[FilePart[Ref]] = {
    other.map(files)
  }

  def file[Ref](key: String)(implicit files: A <:< FilePart[Ref]): Option[FilePart[Ref]] = other.map(files).find(_.key == key)

  def fileKeys[Ref](implicit files: A <:< FilePart[Ref]): Seq[String] = {
    other.map(files).collect {
      case FilePart(key, _, _, _) => key
    }
  }
}

object MultipartFormData {
  trait Part
  case class DataPart(key: String, value: String) extends Part
  case class FilePart[A](key: String, filename: String, contentType: Option[String], fileRef: A) extends Part
  case class MissingFilePart(key: String) extends Part
  case class BadPart(headers: Map[String, String]) extends Part
  case class MaxDataPartSizeExcedeed(key: String) extends Part
}

trait BodyParsers {

  object parse {

    def tolerantText: BodyParser[String] = BodyParser { request =>
      Iteratee.consume[Array, Byte]().mapDone(c => Right(new String(c, request.charset.getOrElse("utf-8"))))
    }

    def text: BodyParser[String] = when(_.contentType.exists(_ == "text/plain"), tolerantText)

    def raw: BodyParser[Array[Byte]] = BodyParser { request =>
      Iteratee.consume[Array, Byte]().mapDone(c => Right(c))
    }

    def json: BodyParser[JsValue] = when(_.contentType.exists(m => m == "text/json" || m == "application/json"), tolerantJson)

    def tolerantJson: BodyParser[JsValue] = BodyParser { request =>
      Iteratee.consume[Array, Byte]().mapDone { bytes =>
        scala.util.control.Exception.allCatch[JsValue].either {
          parseJson(new String(bytes, request.charset.getOrElse("utf-8")))
        }.left.map { e =>
          (Results.BadRequest, bytes)
        }
      }.flatMap {
        case Left((r, in)) => Done(Left(r), El(in))
        case Right(json) => Done(Right(json), Empty)
      }
    }

    def empty: BodyParser[None.type] = BodyParser { request =>
      Done(Right(None), Empty)
    }

    def tolerantXml: BodyParser[NodeSeq] = BodyParser { request =>
      Iteratee.consume[Array, Byte]().mapDone { bytes =>
        scala.util.control.Exception.allCatch[NodeSeq].either {
          XML.loadString(new String(bytes, request.charset.getOrElse("utf-8")))
        }.left.map { e =>
          (Results.BadRequest, bytes)
        }
      }.flatMap {
        case Left((r, in)) => Done(Left(r), El(in))
        case Right(xml) => Done(Right(xml), Empty)
      }
    }

    def xml: BodyParser[NodeSeq] = when(_.contentType.exists(_.startsWith("text/xml")), tolerantXml)

    def file(to: File): BodyParser[File] = BodyParser { request =>
      Iteratee.fold[Array[Byte], FileOutputStream](new FileOutputStream(to)) { (os, data) =>
        os.write(data)
        os
      }.mapDone { os =>
        os.close()
        Right(to)
      }
    }

    def error[A](result: Result = Results.BadRequest): BodyParser[A] = BodyParser { request =>
      Done(Left(result), Empty)
    }

    def temporaryFile: BodyParser[TemporaryFile] = BodyParser { request =>
      val tempFile = TemporaryFile("requestBody", "asTemporaryFile")
      file(tempFile.file)(request).mapDone(_ => Right(tempFile))
    }

    def tolerantUrlFormEncoded: BodyParser[Map[String, Seq[String]]] = BodyParser { request =>

      import play.core.parsers._
      import scala.collection.JavaConverters._

      Iteratee.consume[Array, Byte]().mapDone { c =>
        scala.util.control.Exception.allCatch[Map[String, Seq[String]]].either {
          UrlFormEncodedParser.parse(new String(c, request.charset.getOrElse("utf-8")))
        }.left.map { e =>
          Results.BadRequest
        }
      }
    }

    def urlFormEncoded: BodyParser[Map[String, Seq[String]]] = when(_.contentType.exists(_ == "application/x-www-form-urlencoded"), tolerantUrlFormEncoded)

    def anyContent: BodyParser[AnyContent] = BodyParser { request =>
      request.contentType match {
        case _ if request.method == "GET" || request.method == "HEAD" => empty(request).map(_.right.map(_ => AnyContentAsEmpty))
        case Some("text/plain") => text(request).map(_.right.map(s => AnyContentAsText(s)))
        case Some("text/xml") => xml(request).map(_.right.map(x => AnyContentAsXml(x)))
        case Some("text/json") => json(request).map(_.right.map(j => AnyContentAsJson(j)))
        case Some("application/json") => json(request).map(_.right.map(j => AnyContentAsJson(j)))
        case Some("application/x-www-form-urlencoded") => urlFormEncoded(request).map(_.right.map(d => AnyContentAsUrlFormEncoded(d)))
        case Some("multipart/form-data") => multipartFormData(request).map(_.right.map(m => AnyContentAsMultipartFormData(m)))
        case _ => raw(request).map(_.right.map(r => AnyContentAsRaw(r)))
      }
    }

    // -- Multipart

    def multipartFormData: BodyParser[MultipartFormData[FilePart[(String, TemporaryFile)]]] = multipartFormData(handleFilePartAsTemporaryFile)

    type PartHandler[A] = PartialFunction[Map[String, String], Iteratee[Array[Byte], A]]

    def multipartFormData[A](partHandler: PartHandler[A]): BodyParser[MultipartFormData[A]] = BodyParser { request =>
      val handler: PartHandler[Either[Part, A]] =
        handleDataPart.andThen(_.map(Left(_)))
          .orElse(partHandler.andThen(_.map(Right(_))))
          .orElse { case headers => Done(Left(BadPart(headers)), Input.Empty) }

      multipartParser(handler)(request).map { errorOrParts =>
        errorOrParts.right.map { parts =>
          val data = parts.collect { case Left(DataPart(key, value)) => (key, value) }.groupBy(_._1).mapValues(_.map(_._2))
          val az = parts.collect { case Right(a) => a }
          val bad = parts.collect { case Left(b @ BadPart(_)) => b }
          MultipartFormData(data, az, bad)

        }
      }
    }

    def multipartParser[A](partHandler: Map[String, String] => Iteratee[Array[Byte], A]): BodyParser[Seq[A]] = parse.using { request =>

      val maybeBoundary = request.headers.get(play.api.http.HeaderNames.CONTENT_TYPE).filter(ct => ct.trim.startsWith("multipart/form-data")).flatMap { mpCt =>
        mpCt.trim.split("boundary=").tail.headOption.map(b => ("\r\n--" + b).getBytes("utf-8"))
      }

      maybeBoundary.map { boundary =>

        BodyParser { request =>

          val CRLF = "\r\n".getBytes

          val takeUpToBoundary = Enumeratee.takeWhile[MatchInfo[Array[Byte]]](!_.isMatch)

          val maxHeaderBuffer =
            Traversable.drop[Array[Byte]](2) ><>
              Traversable.takeUpTo(4 * 1024) transform
              Iteratee.consume[Array, Byte]()

          val collectHeaders = maxHeaderBuffer.flatMap { buffer =>
            val (headerBytes, rest) = buffer.splitAt(buffer.indexOfSlice(CRLF ++ CRLF))

            val headerString = new String(headerBytes)
            val headers = headerString.lines.map { header =>
              val key :: value = header.trim.split(":").toList
              (key.trim.toLowerCase, value.mkString.trim)
            }.toMap

            Cont(in => Done(headers, in match {
              case Input.El(e) => Input.El(rest.drop(4) ++ e)
              case Input.EOF => Input.El(rest.drop(4))
              case Input.Empty => Input.El(rest.drop(4))
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

      }.getOrElse(parse.error(BadRequest("Missing boundary header")))

    }

    def handleFilePartAsTemporaryFile: PartHandler[FilePart[(String, TemporaryFile)]] = {

      handleFilePart {
        case FileInfo(partName, filename, contentType) =>
          val tempFile = TemporaryFile("multipartBody", "asTemporaryFile")
          Iteratee.fold[Array[Byte], FileOutputStream](new java.io.FileOutputStream(tempFile.file)) { (os, data) =>
            os.write(data)
            os
          }.mapDone { os =>
            os.close()
            val safeFileName = filename.split('\\').takeRight(1).mkString
            FilePart(partName, filename, contentType, (safeFileName, tempFile))
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

    def handleFilePart[A](handler: FileInfo => Iteratee[Array[Byte], A]): PartialFunction[Map[String, String], Iteratee[Array[Byte], A]] = {

      case FileInfoMatcher(partName, fileName, contentType) => handler(FileInfo(partName, fileName, contentType))
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

    def handleDataPart: PartialFunction[Map[String, String], Iteratee[Array[Byte], Part]] = {

      case PartInfoMatcher(partName) =>
        Traversable.takeUpTo[Array[Byte]](100 * 1024)
          .transform(Iteratee.consume[Array, Byte]().map(bytes => DataPart(partName, new String(bytes, "utf-8"))))
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

    // -- Parsing utilities

    def using[A](f: RequestHeader => BodyParser[A]) = BodyParser { request =>
      f(request)(request)
    }

    def when[A](predicate: RequestHeader => Boolean, parser: BodyParser[A], badResult: Result = Results.BadRequest): BodyParser[A] = {
      BodyParser { request =>
        if (predicate(request)) {
          parser(request)
        } else {
          Done(Left(badResult), Empty)
        }
      }
    }

    def either[A, B](parser1: BodyParser[A], parser2: BodyParser[B], badResult: (Result, Result) => Result = (_, _) => Results.BadRequest): BodyParser[Either[A, B]] = {
      BodyParser { request =>
        parser1(request).flatMap {
          case Left(r1) => parser2(request).mapDone(_.right.map(Right(_)).left.map(r2 => badResult(r1, r2)))
          case Right(v) => Done(Right(Left(v)), Empty)
        }
      }
    }

  }

}

object BodyParsers extends BodyParsers

