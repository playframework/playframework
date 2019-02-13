/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.templates

import akka.util.ByteString
import org.specs2.mutable._
import play.api.http.{ HttpEntity, Writeable }
import play.api.mvc.Results
import play.mvc.{ Results => JResults }

class TemplatesSpec extends Specification {
  "toHtmlArgs" should {
    "escape attribute values" in {
      PlayMagic.toHtmlArgs(Map('foo -> """bar <>&"'""")).body must_== """foo="bar &lt;&gt;&amp;&quot;&#x27;""""
    }
  }

  "Xml" should {
    import play.twirl.api.Xml

    val xml = Xml("\n\t xml")

    "have body trimmed by implicit Writeable" in {
      val writeable = implicitly[Writeable[Xml]]
      string(writeable.transform(xml)) must_== "xml"
    }

    "have Scala result body trimmed" in {
      consume(Results.Ok(xml).body) must_== "xml"
    }

    "have Java result body trimmed" in {
      consume(JResults.ok(xml).asScala.body) must_== "xml"
    }
  }

  def string(bytes: ByteString): String = bytes.utf8String

  def consume(entity: HttpEntity): String = entity match {
    case HttpEntity.Strict(data, _) => string(data)
    case _ => throw new IllegalArgumentException("Expected strict body")
  }
}
