/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs

import java.io.File
import java.nio.file.Files

import org.specs2.mutable.Specification
import org.xml.sax.SAXException

class XMLSpec extends Specification {
  "The Java XML support" should {
    def parse(xml: String) = {
      XML.fromString(xml)
    }

    def writeStringToFile(file: File, text: String) = {
      val out = java.nio.file.Files.newOutputStream(file.toPath)
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
      val f = Files.createTempFile("xxe", ".txt").toFile
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
      val externalParameterEntity = Files.createTempFile("xep", ".dtd").toFile
      val externalGeneralEntity   = Files.createTempFile("xxe", ".txt").toFile
      writeStringToFile(
        externalParameterEntity,
        s"""
           |<!ENTITY % xge SYSTEM "${externalGeneralEntity.toURI}">
           |<!ENTITY % pe "<!ENTITY xxe '%xge;'>">
        """.stripMargin
      )
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
      val xml    = s"""<?xml version="1.0"?>
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
      val as       = "a" * 50000
      val entities = "&a;" * 50000
      val xml      = s"""<?xml version="1.0"?>
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
