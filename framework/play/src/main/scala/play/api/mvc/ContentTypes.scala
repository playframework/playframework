package play.api.mvc

import java.io._

import scala.xml._

import play.api.libs.iteratee._
import play.api.libs.Files.{ TemporaryFile }

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

trait BodyParsers {

  object parse {

    def tolerantTxt: BodyParser[String] = BodyParser { request =>
      Iteratee.consume.mapDone(c => Right(new String(c, request.charset.getOrElse("utf-8"))))
    }

    def txt: BodyParser[String] = when(_.contentType.exists(_ == "text/plain"), tolerantTxt)

    def raw: BodyParser[Array[Byte]] = BodyParser { request =>
      Iteratee.consume.mapDone(c => Right(c))
    }

    def empty: BodyParser[None.type] = BodyParser { request =>
      Done(Right(None), Empty)
    }

    def tolerantXml: BodyParser[NodeSeq] = BodyParser { request =>
      Iteratee.consume.mapDone { bytes =>
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

    def error(result: Result = Results.BadRequest): BodyParser[None.type] = BodyParser { request =>
      Done(Left(result), Empty)
    }

    def temporaryFile: BodyParser[TemporaryFile] = BodyParser { request =>
      val tempFile = TemporaryFile("requestBody", "asTemporaryFile")
      file(tempFile.file)(request).mapDone(_ => Right(tempFile))
    }

    def tolerantUrlFormEncoded: BodyParser[Map[String, Seq[String]]] = BodyParser { request =>

      import play.core.parsers._
      import scala.collection.JavaConverters._

      Iteratee.consume.mapDone { c =>
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
        case _ if request.method == "GET" || request.method == "HEAD" => empty(request).mapDone(_.right.map(_ => AnyContentAsEmpty))
        case Some("text/plain") => txt(request).mapDone(_.right.map(s => AnyContentAsText(s)))
        case Some("text/xml") => xml(request).mapDone(_.right.map(x => AnyContentAsXml(x)))
        case Some("application/x-www-form-urlencoded") => urlFormEncoded(request).mapDone(_.right.map(d => AnyContentAsUrlFormEncoded(d)))
        case _ => raw(request).mapDone(_.right.map(r => AnyContentAsRaw(r)))
      }
    }

    // --

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

