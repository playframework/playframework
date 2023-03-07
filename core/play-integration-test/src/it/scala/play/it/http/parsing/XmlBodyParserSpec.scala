/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.parsing

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import scala.xml.NodeSeq

import akka.stream.scaladsl.Source
import akka.stream.Materializer
import akka.util.ByteString
import play.api.mvc.BodyParser
import play.api.mvc.PlayBodyParsers
import play.api.test._
import play.api.Application

class XmlBodyParserSpec extends PlaySpecification {
  "The XML body parser" should {
    implicit def tolerantXmlBodyParser(implicit app: Application): BodyParser[NodeSeq] =
      app.injector.instanceOf[PlayBodyParsers].tolerantXml(1048576)

    def xmlBodyParser(implicit app: Application) = app.injector.instanceOf[PlayBodyParsers].xml

    def parse(xml: String, contentType: Option[String], encoding: String)(
        implicit mat: Materializer,
        bodyParser: BodyParser[NodeSeq]
    ) = {
      await(
        bodyParser(FakeRequest().withHeaders(contentType.map(CONTENT_TYPE -> _).toSeq: _*))
          .run(Source.single(ByteString(xml, encoding)))
      )
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

    "honour the external charset for text sub types" in new WithApplication() {
      parse("<foo>bär</foo>", Some("text/xml; charset=iso-8859-1"), "iso-8859-1") must beRight.like {
        case xml => xml.text must_== "bär"
      }
      parse("<foo>bär</foo>", Some("text/xml; charset=utf-16"), "utf-16") must beRight.like {
        case xml => xml.text must_== "bär"
      }
    }

    "default to iso-8859-1 for text sub types" in new WithApplication() {
      parse("<foo>bär</foo>", Some("text/xml"), "iso-8859-1") must beRight.like {
        case xml => xml.text must_== "bär"
      }
    }

    "default to reading the encoding from the prolog for application sub types" in new WithApplication() {
      parse("""<?xml version="1.0" encoding="utf-16"?><foo>bär</foo>""", Some("application/xml"), "utf-16") must beRight
        .like {
          case xml => xml.text must_== "bär"
        }
      parse(
        """<?xml version="1.0" encoding="iso-8859-1"?><foo>bär</foo>""",
        Some("application/xml"),
        "iso-8859-1"
      ) must beRight
        .like {
          case xml => xml.text must_== "bär"
        }
    }

    "default to reading the encoding from the prolog for no content type" in new WithApplication() {
      parse("""<?xml version="1.0" encoding="utf-16"?><foo>bär</foo>""", None, "utf-16") must beRight.like {
        case xml => xml.text must_== "bär"
      }
      parse("""<?xml version="1.0" encoding="iso-8859-1"?><foo>bär</foo>""", None, "iso-8859-1") must beRight.like {
        case xml => xml.text must_== "bär"
      }
    }

    "accept all common xml content types" in new WithApplication() {
      parse("<foo>bar</foo>", Some("application/xml; charset=utf-8"), "utf-8") must beRight.like {
        case xml => xml.text must_== "bar"
      }
      parse("<foo>bar</foo>", Some("text/xml; charset=utf-8"), "utf-8") must beRight.like {
        case xml => xml.text must_== "bar"
      }
      parse("<foo>bar</foo>", Some("application/xml+rdf; charset=utf-8"), "utf-8") must beRight.like {
        case xml => xml.text must_== "bar"
      }
    }

    "reject non XML content types" in new WithApplication() {
      parse("<foo>bar</foo>", Some("text/plain; charset=utf-8"), "utf-8")(app.materializer, xmlBodyParser) must beLeft
      parse("<foo>bar</foo>", Some("xml/xml; charset=utf-8"), "utf-8")(app.materializer, xmlBodyParser) must beLeft
      parse("<foo>bar</foo>", None, "utf-8")(app.materializer, xmlBodyParser) must beLeft
    }

    "gracefully handle invalid xml" in new WithApplication() {
      parse("<foo", Some("text/xml; charset=utf-8"), "utf-8") must beLeft
    }

    "parse XML bodies without loading in a related schema" in new WithApplication() {
      val f = File.createTempFile("xxe", ".txt")
      Files.write(f.toPath, "I shouldn't be there!".getBytes(StandardCharsets.UTF_8))
      f.deleteOnExit()
      val xml = s"""<?xml version="1.0" encoding="ISO-8859-1"?>
                   | <!DOCTYPE foo [
                   |   <!ELEMENT foo ANY >
                   |   <!ENTITY xxe SYSTEM "${f.toURI}">]><foo>hello&xxe;</foo>""".stripMargin

      parse(xml, Some("text/xml; charset=iso-8859-1"), "iso-8859-1") must beLeft
    }

    "parse XML bodies without loading in a related schema from a parameter" in new WithApplication() {
      val externalParameterEntity = File.createTempFile("xep", ".dtd")
      val externalGeneralEntity   = File.createTempFile("xxe", ".txt")
      Files.write(
        externalParameterEntity.toPath,
        s"""
           |<!ENTITY % xge SYSTEM "${externalGeneralEntity.toURI}">
           |<!ENTITY % pe "<!ENTITY xxe '%xge;'>">
        """.stripMargin.getBytes(StandardCharsets.UTF_8)
      )
      Files.write(externalGeneralEntity.toPath, "I shouldnt be there!".getBytes(StandardCharsets.UTF_8))
      externalGeneralEntity.deleteOnExit()
      externalParameterEntity.deleteOnExit()
      val xml = s"""<?xml version="1.0" encoding="ISO-8859-1"?>
                   | <!DOCTYPE foo [
                   |   <!ENTITY % xpe SYSTEM "${externalParameterEntity.toURI}">
                   |   %xpe;
                   |   %pe;
                   |   ]><foo>hello&xxe;</foo>""".stripMargin

      parse(xml, Some("text/xml; charset=iso-8859-1"), "iso-8859-1") must beLeft
    }

    "gracefully fail when there are too many nested entities" in new WithApplication() {
      val nested = for (x <- 1 to 30) yield "<!ENTITY laugh" + x + " \"&laugh" + (x - 1) + ";&laugh" + (x - 1) + ";\">"
      val xml = s"""<?xml version="1.0"?>
                   | <!DOCTYPE billion [
                   | <!ELEMENT billion (#PCDATA)>
                   | <!ENTITY laugh0 "ha">
                   | ${nested.mkString("\n")}
                   | ]>
                   | <billion>&laugh30;</billion>""".stripMargin
      parse(xml, Some("text/xml; charset=utf-8"), "utf-8") must beLeft
      success
    }

    "gracefully fail when an entity expands to be very large" in new WithApplication() {
      val as       = "a" * 50000
      val entities = "&a;" * 50000
      val xml = s"""<?xml version="1.0"?>
                   | <!DOCTYPE kaboom [
                   | <!ENTITY a "$as">
                   | ]>
                   | <kaboom>$entities</kaboom>""".stripMargin
      parse(xml, Some("text/xml; charset=utf-8"), "utf-8") must beLeft
    }
  }
}
