package play.it.http.parsing

import java.io.File
import org.apache.commons.io.{FileUtils, IOUtils}
import org.specs2.mutable.Specification
import play.api.test._
import play.api.mvc.{BodyParser, BodyParsers}
import play.api.libs.iteratee.Enumerator
import scala.xml.NodeSeq

object XmlBodyParserSpec extends PlaySpecification {

  "The XML body parser" should {

    def parse(xml: String, contentType: Option[String], encoding: String, bodyParser: BodyParser[NodeSeq] = BodyParsers.parse.tolerantXml) = {
      await(Enumerator(xml.getBytes(encoding)) |>>>
        bodyParser(FakeRequest().withHeaders(contentType.map(CONTENT_TYPE -> _).toSeq:_*)))
    }

    "parse XML bodies" in new WithApplication() {
      parse("<foo>bar</foo>", Some("application/xml; charset=utf-8"), "utf-8") must beRight.like {
        case xml => xml.text must_== "bar"
      }
    }

    "honour the external charset for application sub types" in new WithApplication() {
      parse("<foo>bär</foo>", Some("application/xml; charset=iso-8859-1"), "iso-8859-1") must beRight.like {
        case xml => xml.text must_== "bär"
      }
      parse("<foo>bär</foo>", Some("application/xml; charset=utf-16"), "utf-16") must beRight.like {
        case xml => xml.text must_== "bär"
      }
    }

    "reject non XML content types" in new WithApplication() {
      parse("<foo>bar</foo>", Some("text/plain; charset=utf-8"), "utf-8", BodyParsers.parse.xml) must beLeft
      parse("<foo>bar</foo>", Some("xml/xml; charset=utf-8"), "utf-8", BodyParsers.parse.xml) must beLeft
      parse("<foo>bar</foo>", None, "utf-8", BodyParsers.parse.xml) must beLeft
    }

    "gracefully handle invalid xml" in new WithApplication() {
      parse("<foo", Some("text/xml; charset=utf-8"), "utf-8", BodyParsers.parse.xml) must beLeft
    }

    "parse XML bodies without loading in a related schema" in new WithApplication() {
      val f = File.createTempFile("xxe", "txt")
      FileUtils.writeStringToFile(f, "I shouldn't be there!")
      f.deleteOnExit()
      val xml = s"""<?xml version="1.0" encoding="ISO-8859-1"?>
                  | <!DOCTYPE foo [
                  |   <!ELEMENT foo ANY >
                  |   <!ENTITY xxe SYSTEM "${f.toURI}" >]><foo>hello&xxe;</foo>""".stripMargin

      parse(xml, Some("text/xml; charset=iso-8859-1"), "iso-8859-1") must beRight.like {
        case xml => xml.text must_== "hello"
      }
    }
  }

}
