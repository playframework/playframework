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

  def asMultipartFormData: Option[MultipartFormData] = this match {
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
case class AnyContentAsMultipartFormData(mdf: MultipartFormData) extends AnyContent

case class MultipartFormData(parts: Seq[Part]) {

  def asUrlFormEncoded: Map[String, Seq[String]] = {
    parts.collect { case DataPart(key, value) => (key, value) }.groupBy(_._1).mapValues(_.map(_._2))
  }

  def files: Seq[(String, String, Option[String], File)] = {
    parts.collect {
      case FilePart(key, filename, contentType, file: File) => (key, filename, contentType, file)
      case FilePart(key, filename, contentType, TemporaryFile(file)) => (key, filename, contentType, file)
    }
  }

  def file(key: String): Option[(String, Option[String], File)] = files.find(_._1 == key).map(f => (f._2, f._3, f._4))

  def fileKeys: Seq[String] = {
    parts.collect {
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

    def multipartFormData: BodyParser[MultipartFormData] = multipartFormData(handleFilePartAsTemporaryFile)

    def multipartFormData[A](filePartHandler: (String, String, Option[String]) => Iteratee[Array[Byte], (String, A)]): BodyParser[MultipartFormData] = BodyParser { request =>
      multipartParser(handlePart(_: Map[String, String], filePartHandler))(request).map { errorOrParts =>
        errorOrParts.right.map(MultipartFormData(_))
      }
    }

    def multipartParser[A](partHandler: Map[String, String] => Iteratee[Array[Byte], A]): BodyParser[Seq[A]] = parse.using { request =>

      val maybeBoundary = request.headers.get(play.api.http.HeaderNames.CONTENT_TYPE).filter(ct => ct.trim.startsWith("multipart/form-data")).flatMap { mpCt =>
        mpCt.trim.split("boundary=").tail.headOption.map(b => ("\r\n--" + b).getBytes("utf-8"))
      }

      maybeBoundary.map { boundary =>

        BodyParser { request =>

          val CRLF = "\r\n".getBytes

          val takeUpToBoundary = {
            Enumeratee.takeWhile[MatchInfo[Array[Byte]]](!_.isMatch)
          }

          val maxHeaderBuffer = (Traversable.drop[Array[Byte]](2) ><> Traversable.takeUpTo[Array[Byte]](4 * 1024))(
            Iteratee.consume[Array, Byte]()).joinI

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

          val readPart = collectHeaders.flatMap { headers =>
            partHandler(headers)
          }

          val handlePart = Enumeratee.map[MatchInfo[Array[Byte]], Array[Byte]](_.content).transform(readPart)

          Traversable.take[Array[Byte]](boundary.size - 2).transform(Iteratee.consume[Array, Byte]()).flatMap { firstBoundary =>

            Parsing.search(boundary) transform Iteratee.repeat {

              takeUpToBoundary.transform(handlePart).flatMap { part =>
                Enumeratee.take(1)(Iteratee.ignore[MatchInfo[Array[Byte]]]).mapDone(_ => part)
              }

            }.map(parts => Right(parts.dropRight(1)))

          }

        }

      }.getOrElse(parse.error(BadRequest("Missing boundary header")))

    }

    def handleFilePartAsTemporaryFile(partName: String, filename: String, contentType: Option[String]): Iteratee[Array[Byte], (String, TemporaryFile)] = {
      val tempFile = TemporaryFile("multipartBody", "asTemporaryFile")
      Iteratee.fold[Array[Byte], FileOutputStream](new java.io.FileOutputStream(tempFile.file)) { (os, data) =>
        os.write(data)
        os
      }.mapDone { os =>
        os.close()
        val safeFileName = filename.split('\\').takeRight(1).mkString
        (safeFileName, tempFile)
      }
    }

    def handlePart[A](headers: Map[String, String], filePartHandler: (String, String, Option[String]) => Iteratee[Array[Byte], (String, A)]): Iteratee[Array[Byte], Part] = {

      val keyValue = """^([a-zA-Z_0-9]+)="(.*)"$""".r

      headers.get("content-disposition").flatMap { value =>

        val values = value.split(";").map(_.trim).map {
          case keyValue(key, value) => (key.trim, value.trim)
          case key => (key.trim, "")
        }.toMap

        values.get("form-data").flatMap(_ => values.get("name")).map { partName =>

          values.get("filename").map { filename =>

            if (filename.trim.isEmpty) {
              Done[Array[Byte], Part](MissingFilePart(partName), Input.Empty)
            } else {
              val contentType = headers.get("content-type")
              filePartHandler(partName, filename, contentType).map {
                case (safeFileName, fileRef) => FilePart(partName, safeFileName, headers.get("content-type"), fileRef)
              }
            }

          }.getOrElse {

            Traversable.takeUpTo[Array[Byte]](100 * 1024).transform(
              Iteratee.consume[Array, Byte]().mapDone { bytes =>
                DataPart(partName, new String(bytes, "utf-8"))
              }).flatMap { data =>
                Cont({
                  case Input.El(_) => Done(MaxDataPartSizeExcedeed(partName), Input.Empty)
                  case in => Done(data, in)
                })
              }

          }

        }

      }.getOrElse {
        Done[Array[Byte], Part](BadPart(headers), Input.Empty)
      }

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

