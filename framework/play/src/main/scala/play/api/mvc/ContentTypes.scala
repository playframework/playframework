package play.api.mvc

import scala.xml._

import play.api.libs.iteratee._

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

trait Parsers {

  def txt: BodyParser[String] = BodyParser { request =>
    Iteratee.consume.mapDone(c => Right(new String(c, request.charset.getOrElse("utf-8"))))
  }

  def raw: BodyParser[Array[Byte]] = BodyParser { request =>
    Iteratee.consume.mapDone(c => Right(c))
  }

  def empty: BodyParser[None.type] = BodyParser { request =>
    Done(Right(None), Empty)
  }

  def xml: BodyParser[NodeSeq] = BodyParser { request =>
    Iteratee.consume.mapDone { x =>
      scala.util.control.Exception.allCatch[NodeSeq].either {
        XML.loadString(new String(x, request.charset.getOrElse("utf-8")))
      }.left.map { e =>
        Results.BadRequest
      }
    }
  }

  def urlFormEncoded: BodyParser[Map[String, Seq[String]]] = BodyParser { request =>

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

  def anyContent: BodyParser[AnyContent] = BodyParser { request =>
    request.contentType match {
      case _ if request.method == "GET" || request.method == "HEAD" => empty(request).mapDone(_.right.map(_ => AnyContentAsEmpty))
      case Some("text/plain") => txt(request).mapDone(_.right.map(s => AnyContentAsText(s)))
      case Some("text/xml") => xml(request).mapDone(_.right.map(x => AnyContentAsXml(x)))
      case Some("application/x-www-form-urlencoded") => urlFormEncoded(request).mapDone(_.right.map(d => AnyContentAsUrlFormEncoded(d)))
      case _ => raw(request).mapDone(_.right.map(r => AnyContentAsRaw(r)))
    }
  }

}

object Parsers extends Parsers

