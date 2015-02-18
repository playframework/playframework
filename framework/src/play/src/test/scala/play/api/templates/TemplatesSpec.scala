/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.templates

import org.specs2.mutable._
import play.api.http.Writeable
import play.api.libs.iteratee._
import play.api.mvc.Results
import play.core.j.JavaResults
import play.mvc.{ Results => JResults }
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object TemplatesSpec extends Specification {
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

    "have body trimmed by JavaResults.writeContent" in {
      val writeable = JavaResults.writeContent(xml.contentType)
      string(writeable.transform(xml)) must_== "xml"
    }

    "have Scala result body trimmed" in {
      consume(Results.Ok(xml).body) must_== "xml"
    }

    "have Java result body trimmed" in {
      consume(JResults.ok(xml).toScala.body) must_== "xml"
    }
  }

  def string(bytes: Array[Byte]): String = new String(bytes, "UTF-8")

  def consume(enumerator: Enumerator[Array[Byte]]): String = {
    string(Await.result(enumerator |>>> Iteratee.consume[Array[Byte]](), Duration(5, "seconds")))
  }
}
