/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs

import java.io.{ FileOutputStream, File }

import org.specs2.mutable.Specification
import org.xml.sax.SAXException

object XMLSpec extends Specification {

  "The Java XML support" should {

    def parse(xml: String) = {
      XML.fromString(xml)
    }

    def writeStringToFile(file: File, text: String) = {
      val out = new FileOutputStream(file)
      try {
        out.write(text.getBytes("utf-8"))
      } finally {
        out.close()
      }
    }

    "parse XML bodies" in {
      parse("<foo>bar</foo>").getChildNodes.item(0).getNodeName must_== "foo"
    }

    "parse XML bodies without loading in a related schema" in {
      val f = File.createTempFile("xxe", ".txt")
      writeStringToFile(f, "I shouldn't be there!")
      f.deleteOnExit()
      val xml = s"""<?xml version="1.0" encoding="ISO-8859-1"?>
                  | <!DOCTYPE foo [
                  |   <!ELEMENT foo ANY >
                  |   <!ENTITY xxe SYSTEM "${f.toURI}">]><foo>hello&xxe;</foo>""".stripMargin

      parse(xml) must throwA[RuntimeException].like {
        case re => re.getCause must beAnInstanceOf[SAXException]
      }
    }

    "parse XML bodies without loading in a related schema from a parameter" in {
      val externalParameterEntity = File.createTempFile("xep", ".dtd")
      val externalGeneralEntity = File.createTempFile("xxe", ".txt")
      writeStringToFile(externalParameterEntity,
        s"""
          |<!ENTITY % xge SYSTEM "${externalGeneralEntity.toURI}">
          |<!ENTITY % pe "<!ENTITY xxe '%xge;'>">
        """.stripMargin)
      writeStringToFile(externalGeneralEntity, "I shouldnt be there!")
      externalGeneralEntity.deleteOnExit()
      externalParameterEntity.deleteOnExit()
      val xml = s"""<?xml version="1.0" encoding="ISO-8859-1"?>
                  | <!DOCTYPE foo [
                  |   <!ENTITY % xpe SYSTEM "${externalParameterEntity.toURI}">
                  |   %xpe;
                  |   %pe;
                  |   ]><foo>hello&xxe;</foo>""".stripMargin

      parse(xml) must throwA[RuntimeException].like {
        case re => re.getCause must beAnInstanceOf[SAXException]
      }
    }

    "gracefully fail when there are too many nested entities" in {
      val nested = for (x <- 1 to 30) yield "<!ENTITY laugh" + x + " \"&laugh" + (x - 1) + ";&laugh" + (x - 1) + ";\">"
      val xml = s"""<?xml version="1.0"?>
                  | <!DOCTYPE billion [
                  | <!ELEMENT billion (#PCDATA)>
                  | <!ENTITY laugh0 "ha">
                  | ${nested.mkString("\n")}
                  | ]>
                  | <billion>&laugh30;</billion>""".stripMargin

      parse(xml) must throwA[RuntimeException].like {
        case re => re.getCause must beAnInstanceOf[SAXException]
      }
    }

    "gracefully fail when an entity expands to be very large" in {
      val as = "a" * 50000
      val entities = "&a;" * 50000
      val xml = s"""<?xml version="1.0"?>
                  | <!DOCTYPE kaboom [
                  | <!ENTITY a "$as">
                  | ]>
                  | <kaboom>$entities</kaboom>""".stripMargin

      parse(xml) must throwA[RuntimeException].like {
        case re => re.getCause must beAnInstanceOf[SAXException]
      }
    }
  }
}
